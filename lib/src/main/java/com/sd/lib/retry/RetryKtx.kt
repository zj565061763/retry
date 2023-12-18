package com.sd.lib.retry

suspend fun <T> fRetry(
    maxRetryCount: Int = Int.MAX_VALUE,
    retryInterval: Long = 3000,
    block: suspend () -> Result<T>,
): Result<T> {
    return Result.failure(Exception())
}