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
    @Synchronized
    fun start() {
        if (isStarted) return
        isStarted = true

        retryCount = 0
        _mainHandler.post { onStart() }
        retryDelayed(0)
    }

    /**
     * 停止重试
     */
    fun cancel() {
        cancelInternal()
    }

    @Synchronized
    private fun cancelInternal(checkRetryCount: Boolean = false) {
        if (!isStarted) return
        isStarted = false

        _mainHandler.removeCallbacks(_retryRunnable)
        _loadSession?.let { it.isFinish = true }
        _loadSession = null
        _isRetryPaused = false

        val notifyRetryMax = checkRetryCount && retryCount >= maxRetryCount
        _mainHandler.post {
            onStop()
            if (notifyRetryMax) {
                onRetryMaxCount()
            }
        }
    }

    /**
     * 延迟[delayMillis]毫秒重试
     */
    private fun retryDelayed(delayMillis: Long) {
        _mainHandler.removeCallbacks(_retryRunnable)
        _mainHandler.postDelayed(_retryRunnable, delayMillis)
    }

    @Synchronized
    internal fun resumeRetry() {
        if (_isRetryPaused) {
            _isRetryPaused = false
            if (isStarted && _loadSession == null) {
                retryDelayed(0)
            }
        }
    }

    private fun tryInternal() {
        check(Looper.myLooper() == Looper.getMainLooper())

        var session: LoadSession? = null
        synchronized(this@FRetry) {
            if (!isStarted) return
            check(_loadSession == null) { "Current LoadSession is not finished." }

            if (retryCount >= maxRetryCount) {
                cancelInternal(true)
                return
            }

            if (!checkRetry()) {
                check(isStarted) { "Cannot cancel retry in checkRetry() callback." }
                if (!_isRetryPaused) {
                    _isRetryPaused = true
                    _mainHandler.post { onPause() }
                }
                return
            }

            _isRetryPaused = false
            retryCount++
            _loadSession = InternalLoadSession().also {
                session = it
            }
        }

        session?.let {
            if (!onRetry(it)) {
                cancel()
            }
        }
    }

    /**
     * 检查是否可以发起重试（UI线程），此方法被调用时已经synchronized了当前对象，
     * 返回false会触发[onPause]回调
     */
    protected open fun checkRetry(): Boolean {
        return true
    }

    /**
     * 开始回调（UI线程）
     */
    protected open fun onStart() {}

    /**
     * 暂停回调（UI线程）
     */
    protected open fun onPause() {}

    /**
     * 结束回调（UI线程）
     */
    protected open fun onStop() {}

    /**
     * 重试回调（UI线程），返回false将结束重试
     */
    abstract fun onRetry(session: LoadSession): Boolean

    /**
     * 达到最大重试次数回调（UI线程）
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