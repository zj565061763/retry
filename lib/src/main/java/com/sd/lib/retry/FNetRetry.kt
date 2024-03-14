package com.sd.lib.retry

import com.sd.lib.network.FNetworkObserver

abstract class FNetRetry(maxRetryCount: Int) : FRetry(maxRetryCount) {

    override fun checkRetry(): Boolean {
        if (!FNetworkObserver.isNetworkAvailable()) {
            _networkObserver.register()
            return false
        }
        return super.checkRetry()
    }

    override fun onStop() {
        super.onStop()
        _networkObserver.unregister()
    }

    private val _networkObserver = object : FNetworkObserver() {
        override fun onChange(isAvailable: Boolean) {
            if (isAvailable) {
                resumeRetry()
            }
        }
    }
}