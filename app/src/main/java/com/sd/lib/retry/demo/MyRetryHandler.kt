package com.sd.lib.retry.demo

import android.content.Context
import android.util.Log
import com.sd.lib.retry.FNetRetryHandler
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch

class MyRetryHandler : FNetRetryHandler {
    private val TAG = MyRetryHandler::class.java.simpleName
    private var _count = 0

    constructor(context: Context) : super(context, 15) {
        setRetryInterval(1000)
    }

    override fun onRetry(session: LoadSession) {
        session.onLoading()
        GlobalScope.launch {
            _count++
            Log.i(TAG, "onRetry count:${_count}")
            if (_count >= 10) {
                session.onLoadFinish()
            } else {
                session.onLoadError()
            }
        }
    }

    override fun onRetryMaxCount() {
        super.onRetryMaxCount()
        Log.i(TAG, "onRetryMaxCount")
    }
}