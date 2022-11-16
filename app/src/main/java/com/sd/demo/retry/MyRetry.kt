package com.sd.demo.retry

import android.content.Context
import com.sd.lib.retry.FNetRetryHandler

class MyRetry(context: Context) : FNetRetryHandler(context, 10) {
    private var _count = 0

    init {
        setRetryInterval(1000)
    }

    override fun onStateChanged(started: Boolean) {
        super.onStateChanged(started)
        logMsg { "onStateChanged $started" }
        _count = 0
    }

    override fun onRetry(session: LoadSession) {
        session.onLoading()
        logMsg { "onRetry $retryCount" }

        _count++
        if (_count >= 5) {
            session.onLoadFinish()
        } else {
            session.onLoadError()
        }
    }

    override fun onRetryMaxCount() {
        super.onRetryMaxCount()
        logMsg { "onRetryMaxCount" }
    }
}