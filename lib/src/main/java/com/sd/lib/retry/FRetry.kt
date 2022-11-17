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
    private var _isRetryPaused = false
    private var _loadSession: InternalLoadSession? = null

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
        onStart()
    }

    /**
     * 停止重试
     */
    fun cancel(): Boolean {
        synchronized(this@FRetry) {
            if (!isStarted) return false
            isStarted = false
            _mainHandler.removeCallbacks(_retryRunnable)
            _loadSession?.let { it.isFinish = true }
            _loadSession = null
            _isRetryPaused = false
        }
        onStop()
        return true
    }

    /**
     * 延迟[delayMillis]毫秒重试
     */
    private fun retryDelayed(delayMillis: Long) {
        _mainHandler.removeCallbacks(_retryRunnable)
        _mainHandler.postDelayed(_retryRunnable, delayMillis)
    }

    internal fun resumeRetry() {
        synchronized(this@FRetry) {
            if (_isRetryPaused) {
                _isRetryPaused = false
                if (isStarted && _loadSession == null) {
                    retryDelayed(0)
                }
            }
        }
    }

    private fun tryInternal() {
        check(Looper.myLooper() == Looper.getMainLooper())

        val isRetryMaxCount = synchronized(this@FRetry) {
            if (!isStarted) return
            check(_loadSession == null) { "Current LoadSession is not finished." }
            retryCount >= maxRetryCount
        }

        if (isRetryMaxCount) {
            if (cancel()) {
                onRetryMaxCount()
            }
            return
        }

        if (!checkRetry()) {
            synchronized(this@FRetry) {
                if (!_isRetryPaused && isStarted) {
                    _isRetryPaused = true
                    true
                } else false
            }.let {
                if (it) onPause()
            }
            return
        }

        _isRetryPaused = false
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
     * 开始回调
     */
    protected open fun onStart() {}

    /**
     * 暂停回调，[checkRetry]返回false的时候触发
     */
    protected open fun onPause() {}

    /**
     * 结束回调
     */
    protected open fun onStop() {}

    /**
     * 执行重试任务（UI线程），返回false将结束重试
     */
    abstract fun onRetry(session: LoadSession): Boolean

    /**
     * 达到最大重试次数（UI线程）
     */
    protected open fun onRetryMaxCount() {}

    private inner class InternalLoadSession : LoadSession {
        var isFinish = false
            set(value) {
                require(value) { "Require true value." }
                field = value
                if (_loadSession == this) {
                    _loadSession = null
                }
            }

        override fun onLoadFinish() {
            synchronized(this@FRetry) {
                if (isFinish) return
                isFinish = true
            }
            cancel()
        }

        override fun onLoadError() {
            synchronized(this@FRetry) {
                if (isFinish) return
                isFinish = true
            }

            val delay = if (retryCount >= maxRetryCount) 0 else _retryInterval
            retryDelayed(delay)
        }
    }

    interface LoadSession {
        fun onLoadFinish()
        fun onLoadError()
    }
}