package com.sd.demo.retry

import android.content.Context
import com.sd.lib.retry.FNetRetry

class AppRetry(context: Context, maxRetryCount: Int) : FNetRetry(context, maxRetryCount) {
    private var _count = 0

    init {
        setRetryInterval(1000)
    }

    override fun onStart() {
        super.onStart()
        logMsg { "AppRetry onStart" }
        _count = 0
    }

    override fun onPause() {
        super.onPause()
        logMsg { "AppRetry onPause" }
    }

    override fun onStop() {
        super.onStop()
        logMsg { "AppRetry onStop" }
    }

    override fun onRetry(session: Session): Boolean {
        logMsg { "AppRetry onRetry $retryCount" }

        _count++
        if (_count >= 10) {
            session.onFinish()
        } else {
            session.onError()
        }

        return true
    }

    override fun onRetryMaxCount() {
        super.onRetryMaxCount()
        logMsg { "AppRetry onRetryMaxCount" }
    }
}