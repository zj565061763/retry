package com.sd.lib.retry

import android.content.Context
import com.sd.lib.retry.utils.FNetworkObserver

abstract class FNetRetry(context: Context, maxRetryCount: Int) : FRetry(maxRetryCount) {
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
        override fun onAvailable() {
            resumeRetry()
        }

        override fun onLost() {
        }
    }
}