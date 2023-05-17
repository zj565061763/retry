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
        if (state != State.Idle) return
        state = State.Running

        retryCount = 0
        _mainHandler.post { onStart() }
        retryDelayed(0)
    }

    /**
     * 停止重试
     */
    @Synchronized
    fun cancelRetry() {
        if (state == State.Idle) return
        state = State.Idle

        _mainHandler.removeCallbacks(_retryRunnable)
        _currentSession?.let { it.isFinish = true }
        _currentSession = null

        _mainHandler.post { onStop() }
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
        _mainHandler.removeCallbacks(_retryRunnable)
        _mainHandler.postDelayed(_retryRunnable, delayMillis)
    }

    private fun retryOnUiThread() {
        check(Looper.myLooper() == Looper.getMainLooper())

        var session: Session? = null
        synchronized(this@FRetry) {
            if (state != State.Running) return
            if (_currentSession != null) error("Current session is not finished.")

            if (retryCount >= maxRetryCount) {
                cancelRetry()
                _mainHandler.post { onRetryMaxCount() }
                return
            }

            val checkRetry = checkRetry()
            check(state == State.Running) { "Cannot cancel retry in checkRetry() callback." }

            if (!checkRetry) {
                state = State.Paused
                _mainHandler.post { onPause() }
                return
            }

            retryCount++
            _currentSession = InternalSession().also {
                session = it
            }
        }

        session?.let {
            if (!onRetry(it)) {
                cancelRetry()
            }
        }
    }

    /**
     * 检查是否可以触发重试（UI线程），此回调触发时已经synchronized了当前对象，返回false会触发暂停重试
     *
     * 注意：此回调里不允许调用[cancelRetry]方法停止重试
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

                cancelRetry()
            }
        }

        override fun retry() {
            synchronized(this@FRetry) {
                if (isFinish) return
                isFinish = true

                val delay = if (retryCount >= maxRetryCount) 0 else _retryInterval
                retryDelayed(delay)
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
}