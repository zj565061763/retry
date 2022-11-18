package com.sd.lib.retry

import com.sd.lib.context.FContext
import com.sd.lib.retry.utils.FNetworkObserver

abstract class FNetRetry(maxRetryCount: Int) : FRetry(maxRetryCount) {
    private val _context get() = FContext.get()

    override fun checkRetry(): Boolean {
        if (!FNetworkObserver.isNetworkAvailable(_context)) {
            return false
        }
        return super.checkRetry()
    }

    override fun onStart() {
        super.onStart()
        _networkObserver.register(_context)
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