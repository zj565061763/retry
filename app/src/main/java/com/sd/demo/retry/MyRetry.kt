package com.sd.demo.retry

import android.content.Context
import com.sd.lib.retry.FNetRetryHandler

class MyRetry(context: Context) : FNetRetryHandler(context, 10) {
    private var _count = 0

    init {
        setRetryInterval(1000)
    }

    override fun onStart() {
        super.onStart()
        logMsg { "MyRetry onStart" }
        _count = 0
    }

    override fun onStop() {
        super.onStop()
        logMsg { "MyRetry onStop" }
    }

    override fun onRetry(session: LoadSession): Boolean {
        logMsg { "onRetry $retryCount" }

        _count++
        if (_count >= 5) {
            session.onLoadFinish()
        } else {
            session.onLoadError()
        }

        return true
    }

    override fun onRetryMaxCount() {
        super.onRetryMaxCount()
        logMsg { "onRetryMaxCount" }
    }
}