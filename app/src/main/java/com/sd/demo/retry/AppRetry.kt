package com.sd.demo.retry

import com.sd.lib.retry.FNetRetry

class AppRetry(maxRetryCount: Int) : FNetRetry(maxRetryCount) {

    init {
        setRetryInterval(1000)
    }

    override fun onStart() {
        super.onStart()
        logMsg { "AppRetry onStart" }
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

        if (retryCount >= 10) {
            session.finish()
        } else {
            session.retry()
        }

        return true
    }

    override fun onRetryMaxCount() {
        super.onRetryMaxCount()
        logMsg { "AppRetry onRetryMaxCount" }
    }
}