package com.sd.lib.retry

import kotlinx.coroutines.MainScope
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

suspend fun <T> fNetRetry(
    maxRetryCount: Int = 10,
    retryInterval: Long = 3000,
    block: suspend () -> Result<T>
): Result<T> {
    return suspendCancellableCoroutine { cont ->
        val scope = MainScope()
        val retry = object : FNetRetry(maxRetryCount) {
            private var _lastResult: Result<T>? = null

            override fun onRetry(session: Session): Boolean {
                scope.launch {
                    val result = block().also {
                        _lastResult = it
                    }
                    result.onSuccess {
                        session.finish()
                        cont.resume(result)
                    }
                    result.onFailure {
                        session.retry()
                    }
                }
                return true
            }

            override fun onRetryMaxCount() {
                super.onRetryMaxCount()
                cont.resume(_lastResult!!)
            }
        }.apply {
            setRetryInterval(retryInterval)
        }

        cont.invokeOnCancellation {
            scope.cancel()
            retry.stopRetry()
        }

        retry.startRetry()
    }
}