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
    check: suspend () -> Unit = { },

    /** 执行回调 */
    block: suspend () -> Result<T>,
): Result<T> {
    return fRetry(
        maxCount = maxCount,
        interval = interval,
        check = {
            fAwaitNetworkAvailable()
            check()
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
    check: suspend () -> Unit = { },

    /** 执行回调 */
    block: suspend () -> Result<T>,
): Result<T> {
    require(maxCount > 0)

    var count = 0
    while (true) {
        count++
        check()

        currentCoroutineContext().ensureActive()
        val result = block()
        currentCoroutineContext().ensureActive()

        if (result.isSuccess) {
            return result
        }

        if (count >= maxCount) {
            return Result.failure(FRetryExceptionRetryMaxCount())
        }

        delay(interval)
    }
}

/**
 * 达到最大重试次数
 */
class FRetryExceptionRetryMaxCount : Exception()