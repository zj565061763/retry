package com.sd.lib.retry

import android.os.Handler
import android.os.Looper
import android.os.MessageQueue
import java.lang.ref.ReferenceQueue
import java.lang.ref.WeakReference

abstract class FRetry(
    /** 最大重试次数 */
    private val maxRetryCount: Int
) {
    enum class State {
        /** 空闲 */
        Idle,
        /** 重试中 */
        Running,
        /** 暂停 */
        Paused
    }

    /** 当前状态 */
    @Volatile
    var state: State = State.Idle
        private set

    /** 当前第几次重试 */
    @Volatile
    var retryCount: Int = 0
        private set

    /** 重试间隔 */
    private var _retryInterval: Long = 3000L
    private var _currentSession: SessionImpl? = null

    private val _mainHandler = Handler(Looper.getMainLooper())
    private val _retryRunnable = Runnable { retryOnUiThread() }

    init {
        require(maxRetryCount > 0) { "Require maxRetryCount > 0" }
    }

    /**
     * 设置重试间隔，默认3000毫秒
     */
    @Synchronized
    fun setRetryInterval(interval: Long) {
        _retryInterval = interval.coerceAtLeast(0)
    }

    /**
     * 开始重试
     */
    @Synchronized
    fun startRetry() {
        if (state == State.Idle) {
            state = State.Running
            retryCount = 0
            notifyStart()
            retryDelayed(0)
        }
    }

    /**
     * 停止重试
     */
    @Synchronized
    fun stopRetry() {
        if (state != State.Idle) {
            state = State.Idle
            _mainHandler.removeCallbacks(_retryRunnable)
            _currentSession?.let { it.isFinish = true }
            _currentSession = null
            notifyStop()
        }
    }

    /**
     * 恢复重试
     */
    @Synchronized
    protected fun resumeRetry() {
        if (state == State.Paused) {
            state = State.Running
            retryDelayed(0)
        }
    }

    /**
     * 延迟[delayMillis]毫秒重试
     */
    private fun retryDelayed(delayMillis: Long) {
        check(state == State.Running)
        if (retryCount >= maxRetryCount) {
            stopRetry()
            notifyRetryMaxCount()
        } else {
            _mainHandler.postDelayed(_retryRunnable, delayMillis)
        }
    }

    private fun retryOnUiThread() {
        check(Looper.myLooper() === Looper.getMainLooper())

        val session = synchronized(this@FRetry) {
            if (state != State.Running) return
            if (retryCount >= maxRetryCount) return
            if (_currentSession != null) error("Current session is not finished.")

            val checkRetry = checkRetry()
            check(state == State.Running) { "Cannot stop retry in checkRetry() callback." }

            if (!checkRetry) {
                state = State.Paused
                notifyPause()
                return
            }

            retryCount++
            SessionImpl().also { _currentSession = it }
        }

        if (state == State.Running) {
            if (onRetry(session)) {
                // retry
            } else {
                stopRetry()
            }
        }
    }

    private fun notifyStart() {
        _mainHandler.post {
            onStart()
        }
    }

    private fun notifyPause() {
        _mainHandler.post {
            onPause()
        }
    }

    private fun notifyStop() {
        _mainHandler.post {
            onStop()
        }
    }

    private fun notifyRetryMaxCount() {
        _mainHandler.post {
            onRetryMaxCount()
        }
    }

    /**
     * 检查是否可以触发重试（UI线程），此回调触发时已经synchronized了当前对象，返回false会触发暂停重试
     * 注意：此回调里不允许调用[stopRetry]方法停止重试
     */
    protected open fun checkRetry(): Boolean = true

    /**
     * 计算重试间隔
     */
    protected open fun calculateInterval(interval: Long): Long = interval

    /**
     * 开始回调（UI线程）
     * 注意：在此回调里查询[state]并不一定是[State.Running]，此回调仅用来做通知事件
     */
    protected open fun onStart() = Unit

    /**
     * 暂停回调（UI线程）
     * 注意：在此回调里查询[state]并不一定是[State.Paused]，此回调仅用来做通知事件
     */
    protected open fun onPause() = Unit

    /**
     * 结束回调（UI线程）
     * 注意：在此回调里查询[state]并不一定是[State.Idle]，此回调仅用来做通知事件
     */
    protected open fun onStop() = Unit

    /**
     * 达到最大重试次数回调（UI线程）
     */
    protected open fun onRetryMaxCount() = Unit

    /**
     * 重试回调（UI线程），返回false将停止重试
     */
    abstract fun onRetry(session: Session): Boolean

    private inner class SessionImpl : Session {
        var isFinish = false
            set(value) {
                require(value) { "Require true value." }
                field = true
                if (_currentSession === this@SessionImpl) {
                    _currentSession = null
                }
            }

        override fun finish() {
            synchronized(this@FRetry) {
                if (!isFinish) {
                    isFinish = true
                    stopRetry()
                }
            }
        }

        override fun retry() {
            synchronized(this@FRetry) {
                if (!isFinish) {
                    isFinish = true
                    if (state == State.Running) {
                        val interval = calculateInterval(_retryInterval)
                        retryDelayed(interval)
                    }
                }
            }
        }
    }

    interface Session {
        /**
         * 停止重试
         */
        fun finish()

        /**
         * 重新发起一次重试
         */
        fun retry()
    }

    companion object {
        private val sLock = this@Companion

        private val sHolder: MutableMap<Class<out FRetry>, MutableMap<String, RetryRef<FRetry>>> = hashMapOf()
        private val sRefQueue = ReferenceQueue<FRetry>()

        private val sIdleHandler = MainIdleHandler {
            synchronized(sLock) {
                releaseRefLocked()
                sHolder.isNotEmpty()
            }
        }

        /**
         * 开始
         */
        @JvmStatic
        @JvmOverloads
        fun <T : FRetry> start(
            clazz: Class<T>,
            key: String = "",
            factory: () -> T = { clazz.getDeclaredConstructor().newInstance() },
        ): T {
            return synchronized(sLock) {
                val holder = sHolder.getOrPut(clazz) { hashMapOf() }

                holder[key]?.get()?.let {
                    @Suppress("UNCHECKED_CAST")
                    return it as T
                }

                factory().also { retry ->
                    holder[key] = RetryRef(
                        referent = retry,
                        queue = sRefQueue,
                        clazz = clazz,
                        key = key
                    )
                }
            }.also {
                it.startRetry()
                sIdleHandler.register()
            }
        }

        /**
         * 停止
         */
        @JvmStatic
        @JvmOverloads
        fun stop(
            clazz: Class<out FRetry>,
            key: String = "",
        ) {
            synchronized(sLock) {
                sHolder[clazz]?.get(key)?.get()
            }?.stopRetry()
        }

        private fun releaseRefLocked() {
            while (true) {
                val ref = sRefQueue.poll() ?: return
                check(ref is RetryRef)
                sHolder[ref.clazz]?.let { holder ->
                    holder.remove(ref.key)
                    if (holder.isEmpty()) {
                        sHolder.remove(ref.clazz)
                    }
                }
            }
        }
    }
}

private class RetryRef<T>(
    referent: T,
    queue: ReferenceQueue<T>,
    val clazz: Class<out FRetry>,
    val key: String,
) : WeakReference<T>(referent, queue)

private class MainIdleHandler(
    private val block: () -> Boolean,
) {
    private var _idleHandler: MessageQueue.IdleHandler? = null

    fun register() {
        val mainLooper = Looper.getMainLooper() ?: return
        if (mainLooper === Looper.myLooper()) {
            addIdleHandler()
        } else {
            Handler(mainLooper).post { addIdleHandler() }
        }
    }

    private fun addIdleHandler() {
        val myLooper = Looper.myLooper() ?: return
        check(myLooper === Looper.getMainLooper())

        _idleHandler?.let { return }
        MessageQueue.IdleHandler {
            block().also { keep ->
                if (keep) {
                    // keep
                } else {
                    _idleHandler = null
                }
            }
        }.also {
            _idleHandler = it
            Looper.myQueue().addIdleHandler(it)
        }
    }
}