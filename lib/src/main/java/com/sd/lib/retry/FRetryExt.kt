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
    onStart: (() -> Unit)? = null,
    onPause: (() -> Unit)? = null,
    onStop: (() -> Unit)? = null,
    onRetryMaxCount: (() -> Unit)? = null,
    block: suspend () -> Result<T>,
): Result<T> {
    return fRetry(
        maxRetryCount = maxRetryCount,
        retryInterval = retryInterval,
        factory = { factory() },
        onStart = onStart,
        onPause = onPause,
        onStop = onStop,
        onRetryMaxCount = onRetryMaxCount,
        block = block,
    )
}

suspend fun <T> fRetry(
    maxRetryCount: Int = Int.MAX_VALUE,
    retryInterval: Long = 3000,
    onStart: (() -> Unit)? = null,
    onPause: (() -> Unit)? = null,
    onStop: (() -> Unit)? = null,
    onRetryMaxCount: (() -> Unit)? = null,
    block: suspend () -> Result<T>,
): Result<T> {
    return suspendCancellableCoroutine { cont ->
        val scope = MainScope()
        val retry = object : FRetry(maxRetryCount) {
            private var _result: Result<T>? = null

            override fun onRetry(session: Session): Boolean {
                scope.launch {
                    val result = block().also { _result = it }
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

            override fun onStart() {
                super.onStart()
                onStart?.invoke()
            }

            override fun onPause() {
                super.onPause()
                onPause?.invoke()
            }

            override fun onStop() {
                super.onStop()
                onStop?.invoke()
            }

            override fun onRetryMaxCount() {
                super.onRetryMaxCount()
                cont.resume(_result!!)
                onRetryMaxCount?.invoke()
            }
        }.apply {
            this.setRetryInterval(retryInterval)
        }

        cont.invokeOnCancellation {
            retry.stopRetry()
            scope.cancel()
        }

        retry.startRetry()
    }
}