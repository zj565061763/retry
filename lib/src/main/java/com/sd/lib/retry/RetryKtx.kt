package com.sd.lib.retry

import com.sd.lib.network.fAwaitNetworkAvailable
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.ensureActive

suspend fun <T> fNetRetry(
    /** 最多执行几次 */
    maxCount: Int = Int.MAX_VALUE,

    /** 执行间隔（毫秒） */
    interval: Long = 3_000,

    /** [block]执行之前调用 */
    beforeBlock: suspend FRetryScope.() -> Unit = { },

    /** 执行回调 */
    block: suspend FRetryScope.() -> Result<T>,
): Result<T> {
    return fRetry(
        maxCount = maxCount,
        interval = interval,
        beforeBlock = {
            fAwaitNetworkAvailable()
            beforeBlock()
        },
        block = block,
    )
}

suspend fun <T> fRetry(
    /** 最多执行几次 */
    maxCount: Int = Int.MAX_VALUE,

    /** 执行间隔（毫秒） */
    interval: Long = 3_000,

    /** [block]执行之前调用 */
    beforeBlock: suspend FRetryScope.() -> Unit = { },

    /** 执行回调 */
    block: suspend FRetryScope.() -> Result<T>,
): Result<T> {
    require(maxCount > 0)

    val scope = RetryScopeImpl()

    while (true) {
        scope.increaseRetryCount()
        with(scope) { beforeBlock() }

        currentCoroutineContext().ensureActive()
        val result = with(scope) { block() }
        currentCoroutineContext().ensureActive()

        if (result.isSuccess) {
            return result
        }

        if (scope.retryCount >= maxCount) {
            val cause = checkNotNull(result.exceptionOrNull())
            return Result.failure(FRetryExceptionRetryMaxCount(cause))
        }

        delay(interval)
    }
}

interface FRetryScope {
    /** 当前重试的次数 */
    val retryCount: Int
}

private class RetryScopeImpl : FRetryScope {
    private var _retryCount = 0

    override val retryCount: Int get() = _retryCount

    fun increaseRetryCount() {
        _retryCount++
    }
}

/**
 * 达到最大重试次数
 */
class FRetryExceptionRetryMaxCount(cause: Throwable) : Exception(cause)