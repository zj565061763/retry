package com.sd.lib.retry

import android.os.Handler
import android.os.Looper

abstract class FRetry(
    /** 最大重试次数 */
    val maxRetryCount: Int
) {
    /** 重试是否已经开始 */
    @Volatile
    var isStarted: Boolean = false
        private set

    /** 当前第几次重试 */
    @Volatile
    var retryCount: Int = 0
        private set

    /** 重试间隔 */
    @Volatile
    private var _retryInterval: Long = 3_000L

    @Volatile
    private var _loadSession: InternalLoadSession? = null

    /** 某一次重试是否在加载中 */
    private val isLoading: Boolean
        get() = _loadSession != null

    private val _mainHandler = Handler(Looper.getMainLooper())
    private val _retryRunnable = Runnable { tryInternal() }

    init {
        require(maxRetryCount > 0) { "Require maxRetryCount > 0" }
    }

    /**
     * 设置重试间隔，默认3000毫秒
     */
    fun setRetryInterval(interval: Long) {
        _retryInterval = interval.coerceAtLeast(0)
    }

    /**
     * 开始重试
     */
    fun start() {
        synchronized(this@FRetry) {
            if (isStarted) return
            isStarted = true
            retryCount = 0
        }
        retryDelayed(0)
        onStateChanged(true)
    }

    /**
     * 停止重试
     */
    fun cancel() {
        synchronized(this@FRetry) {
            if (!isStarted) return
            _mainHandler.removeCallbacks(_retryRunnable)
            _loadSession?.let { it._isFinish = true }
            _loadSession = null
            isStarted = false
        }
        onStateChanged(false)
    }

    /**
     * 延迟[delayMillis]毫秒重试
     */
    private fun retryDelayed(delayMillis: Long) {
        _mainHandler.removeCallbacks(_retryRunnable)
        _mainHandler.postDelayed(_retryRunnable, delayMillis)
    }

    private fun tryInternal() {
        check(Looper.myLooper() == Looper.getMainLooper())

        val isRetryMaxCount = synchronized(this@FRetry) {
            if (!isStarted) return
            if (isLoading) error("Current LoadSession is not finished.")
            retryCount >= maxRetryCount
        }

        if (isRetryMaxCount) {
            cancel()
            onRetryMaxCount()
            return
        }

        if (!checkRetry()) {
            return
        }

        synchronized(this@FRetry) {
            if (isStarted) {
                retryCount++
                InternalLoadSession().also { _loadSession = it }
            } else {
                null
            }
        }?.let {
            if (!onRetry(it)) {
                cancel()
            }
        }
    }

    /**
     * 返回是否可以发起重试
     */
    protected open fun checkRetry(): Boolean {
        return true
    }

    /**
     * 是否开始状态变化
     */
    protected open fun onStateChanged(started: Boolean) {}

    /**
     * 执行重试任务（UI线程），返回false将结束重试。
     */
    abstract fun onRetry(session: LoadSession): Boolean

    /**
     * 达到最大重试次数（UI线程）
     */
    protected open fun onRetryMaxCount() {}

    private inner class InternalLoadSession : LoadSession {
        var _isFinish = false
            set(value) {
                require(value) { "Require true value." }
                field = value
                if (_loadSession == this) {
                    _loadSession = null
                }
            }

        override fun onLoadFinish() {
            synchronized(this@FRetry) {
                if (_isFinish) return
                _isFinish = true
            }
            cancel()
        }

        override fun onLoadError() {
            synchronized(this@FRetry) {
                if (_isFinish) return
                _isFinish = true
            }
            retryDelayed(_retryInterval)
        }
    }

    interface LoadSession {
        fun onLoadFinish()
        fun onLoadError()
    }
}