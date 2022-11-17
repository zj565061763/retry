package com.sd.lib.retry

import android.content.Context
import com.sd.lib.retry.utils.FNetworkObserver

abstract class FNetRetry(maxRetryCount: Int, context: Context) : FRetry(maxRetryCount) {
    private val _context = context.applicationContext

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
        override fun onNetworkAvailableChanged(isAvailable: Boolean) {
            this@FNetRetry.onNetworkAvailableChanged(isAvailable)
        }
    }

    protected open fun onNetworkAvailableChanged(isAvailable: Boolean) {
        if (isAvailable) {
            resumeRetry()
        }
    }
}