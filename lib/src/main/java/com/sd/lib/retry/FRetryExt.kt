package com.sd.lib.retry

import kotlinx.coroutines.MainScope
import kotlinx.coroutines.cancel
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

suspend fun <T> fNetRetry(
    maxRetryCount: Int = Int.MAX_VALUE,
    retryInterval: Long = 3000,
    factory: () -> FNetRetry = {
        object : FNetRetry(maxRetryCount) {
            override fun onRetry(session: Session): Boolean {
                error("Callback is null")
            }
        }
    },
    block: suspend () -> Result<T>
): Result<T> {
    return fRetry(
        maxRetryCount = maxRetryCount,
        retryInterval = retryInterval,
        factory = { factory() },
        block = block,
    )
}

suspend fun <T> fRetry(
    maxRetryCount: Int = Int.MAX_VALUE,
    retryInterval: Long = 3000,
    factory: () -> FRetry = {
        object : FRetry(maxRetryCount) {
            override fun onRetry(session: Session): Boolean {
                error("Callback is null")
            }
        }
    },
    block: suspend () -> Result<T>
): Result<T> {
    return suspendCancellableCoroutine { cont ->
        val scope = MainScope()
        val retry = factory().apply {
            setRetryInterval(retryInterval)
        }
        retry.setCallback(object : FRetry.Callback() {
            private var _lastResult: Result<T>? = null

            override fun onRetry(session: FRetry.Session): Boolean {
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
                return scope.isActive
            }

            override fun onRetryMaxCount() {
                super.onRetryMaxCount()
                cont.resume(_lastResult!!)
            }
        })

        cont.invokeOnCancellation {
            scope.cancel()
            retry.stopRetry()
        }

        retry.startRetry()
    }
}