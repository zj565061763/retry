package com.sd.demo.retry

import android.content.Context
import com.sd.lib.retry.FNetRetry

class MyRetry(context: Context) : FNetRetry(context, 10) {
    private var _count = 0

    init {
        setRetryInterval(1000)
    }

    override fun onStart() {
        super.onStart()
        logMsg { "MyRetry onStart" }
        _count = 0
    }

    override fun onPause() {
        super.onPause()
        logMsg { "MyRetry onPause" }
    }

    override fun onStop() {
        super.onStop()
        logMsg { "MyRetry onStop" }
    }

    override fun onRetry(session: LoadSession): Boolean {
        logMsg { "MyRetry onRetry $retryCount" }

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
        logMsg { "MyRetry onRetryMaxCount" }
    }
}