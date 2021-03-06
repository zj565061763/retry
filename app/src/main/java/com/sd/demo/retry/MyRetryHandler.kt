package com.sd.demo.retry

import android.content.Context
import android.util.Log
import com.sd.lib.retry.FNetRetryHandler

class MyRetryHandler : FNetRetryHandler {
    private val TAG = MyRetryHandler::class.java.simpleName
    private var _count = 0

    constructor(context: Context) : super(context, 15) {
        setRetryInterval(1000)
    }

    override fun onStateChanged(started: Boolean) {
        super.onStateChanged(started)
        _count = 0
        Log.i(TAG, "onStateChanged started:${started}")
    }

    override fun onRetry(session: LoadSession) {
        session.onLoading()
        _count++
        Log.i(TAG, "onRetry count:${_count}")

        if (_count >= 10) {
            session.onLoadFinish()
        } else {
            session.onLoadError()
        }
    }

    override fun onRetryMaxCount() {
        super.onRetryMaxCount()
        Log.i(TAG, "onRetryMaxCount")
    }
}