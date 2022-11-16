package com.sd.lib.retry

import android.os.Handler
import android.os.Looper

abstract class FRetry(maxRetryCount: Int) {
    private val _maxRetryCount = maxRetryCount

    /** 重试是否已经开始 */
    @Volatile
    var isStarted = false
        private set(value) {
            if (field != value) {
                field = value
                retryCount = 0
            }
        }

    /** 当前第几次重试 */
    @Volatile
    var retryCount = 0
        private set

    /** 某一次重试是否正在加载中 */
    val isLoading
        get() = _loadSession?._isFinish == false

    /** 重试间隔 */
    @Volatile
    private var _retryInterval = 3_000L

    private var _loadSession: InternalLoadSession? = null
    private val _mainHandler = Handler(Looper.getMainLooper())

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
        }
        retry(0)
        onStateChanged(true)
    }

    /**
     * 停止重试
     */
    fun cancel() {
        cancelInternal(true)
    }

    /**
     * 延迟[delayMillis]毫秒重试
     */
    internal fun retry(delayMillis: Long) {
        val isRetryMaxCount = synchronized(this@FRetry) {
            if (!isStarted) return
            retryCount >= _maxRetryCount
        }

        if (isRetryMaxCount) {
            // TODO 检查重复触发逻辑
            cancelInternal(false)
            _mainHandler.post { onRetryMaxCount() }
            return
        }

        synchronized(this@FRetry) {
            if (isLoading) error("Current LoadSession is not finished.")
            if (checkRetry()) {
                _mainHandler.removeCallbacks(_retryRunnable)
                _mainHandler.postDelayed(_retryRunnable, delayMillis)
            }
        }
    }

    private val _retryRunnable = Runnable {
        synchronized(this@FRetry) {
            if (isStarted && checkRetry()) {
                retryCount++
                InternalLoadSession().also { _loadSession = it }
            } else {
                null
            }
        }?.let { onRetry(it) }
    }

    @Synchronized
    private fun cancelInternal(cancelSession: Boolean) {
        if (!isStarted) return
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
     * 执行重试任务（UI线程）
     */
    abstract fun onRetry(session: LoadSession)

    /**
     * 达到最大重试次数（UI线程）
     */
    protected open fun onRetryMaxCount() {}

    private inner class InternalLoadSession : LoadSession {
        @Volatile
        var _isFinish: Boolean? = null
            private set

        override fun onLoading() {
            synchronized(this@FRetry) {
                if (_isFinish == true) return
                if (_isFinish == null) {
                    _isFinish = !isStarted
                }
            }
        }

        override fun onLoadFinish() {
            synchronized(this@FRetry) {
                if (_isFinish == true) return
                _isFinish = true
            }
            cancelInternal(false)
        }

        override fun onLoadError() {
            synchronized(this@FRetry) {
                if (_isFinish == true) return
                _isFinish = true
            }
            retry(_retryInterval)
        }
    }

    interface LoadSession {
        fun onLoading()
        fun onLoadFinish()
        fun onLoadError()
    }
}