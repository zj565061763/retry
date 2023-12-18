package com.sd.lib.retry

import com.sd.lib.network.FNetworkObserver

abstract class FNetRetry(maxRetryCount: Int) : FRetry(maxRetryCount) {

    override fun checkRetry(): Boolean {
        if (!FNetworkObserver.isNetworkAvailable()) {
            return false
        }
        return super.checkRetry()
    }

    override fun onStart() {
        super.onStart()
        _networkObserver.register()
    }

    override fun onStop() {
        super.onStop()
        _networkObserver.unregister()
    }

    private val _networkObserver = object : FNetworkObserver() {
        override fun onAvailable() {
            resumeRetry()
        }

        override fun onLost() {
        }
    }
}