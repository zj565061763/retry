package com.sd.lib.retry

import android.os.Handler
import android.os.Looper

abstract class FRetry(
    /** 最大重试次数 */
    private val maxRetryCount: Int
) {
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
    private var _currentSession: InternalSession? = null
    private var _callback: Callback? = null

    private val _mainHandler = Handler(Looper.getMainLooper())
    private val _retryRunnable = Runnable { retryOnUiThread() }

    init {
        require(maxRetryCount > 0) { "Require maxRetryCount > 0" }
    }

    /**
     * 设置回调对象
     */
    fun setCallback(callback: Callback?) {
        _callback = callback
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
            _mainHandler.removeCallbacks(_retryRunnable)
            _mainHandler.postDelayed(_retryRunnable, delayMillis)
        }
    }

    private fun retryOnUiThread() {
        check(Looper.myLooper() == Looper.getMainLooper())

        var session: Session? = null
        synchronized(this@FRetry) {
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
            _currentSession = InternalSession().also {
                session = it
            }
        }

        session?.let {
            if (!notifyRetry(it)) {
                stopRetry()
            }
        }
    }

    private fun notifyRetry(session: Session): Boolean {
        return _callback?.onRetry(session) ?: onRetry(session)
    }

    private fun notifyStart() {
        _mainHandler.post {
            _callback?.onStart()
            onStart()
        }
    }

    private fun notifyPause() {
        _mainHandler.post {
            _callback?.onPause()
            onPause()
        }
    }

    private fun notifyStop() {
        _mainHandler.post {
            _callback?.onStop()
            onStop()
        }
    }

    private fun notifyRetryMaxCount() {
        _mainHandler.post {
            _callback?.onRetryMaxCount()
            onRetryMaxCount()
        }
    }

    /**
     * 检查是否可以触发重试（UI线程），此回调触发时已经synchronized了当前对象，返回false会触发暂停重试
     *
     * 注意：此回调里不允许调用[stopRetry]方法停止重试
     */
    protected open fun checkRetry(): Boolean {
        return true
    }

    /**
     * 开始回调（UI线程）
     *
     * 注意：在此回调里查询[state]并不一定是[State.Running]，此回调仅用来做通知事件
     */
    protected open fun onStart() {}

    /**
     * 暂停回调（UI线程）
     *
     * 注意：在此回调里查询[state]并不一定是[State.Paused]，此回调仅用来做通知事件
     */
    protected open fun onPause() {}

    /**
     * 结束回调（UI线程）
     *
     * 注意：在此回调里查询[state]并不一定是[State.Idle]，此回调仅用来做通知事件
     */
    protected open fun onStop() {}

    /**
     * 重试回调（UI线程），返回false将停止重试
     */
    abstract fun onRetry(session: Session): Boolean

    /**
     * 达到最大重试次数回调（UI线程）
     */
    protected open fun onRetryMaxCount() {}

    private inner class InternalSession : Session {
        var isFinish = false
            set(value) {
                require(value) { "Require true value." }
                field = value
                if (_currentSession == this) {
                    _currentSession = null
                }
            }

        override fun finish() {
            synchronized(this@FRetry) {
                if (isFinish) return
                isFinish = true

                stopRetry()
            }
        }

        override fun retry() {
            synchronized(this@FRetry) {
                if (isFinish) return
                isFinish = true

                if (state == State.Running) {
                    retryDelayed(_retryInterval)
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

    enum class State {
        Idle,
        Running,
        Paused
    }

    abstract class Callback {
        /**
         * 开始回调（UI线程）
         */
        open fun onStart() {}

        /**
         * 暂停回调（UI线程）
         */
        open fun onPause() {}

        /**
         * 结束回调（UI线程）
         */
        open fun onStop() {}

        /**
         * 重试回调（UI线程），返回false将停止重试
         */
        abstract fun onRetry(session: Session): Boolean

        /**
         * 达到最大重试次数回调（UI线程）
         */
        open fun onRetryMaxCount() {}
    }
}