[![](https://jitpack.io/v/zj565061763/retry.svg)](https://jitpack.io/#zj565061763/retry)

# 关于

封装重试功能，提供健壮的重试逻辑。

* 支持设置重试间隔，最大重试次数
* 支持自定义重试的条件，例如网络可用才发起重试
* 支持重试状态监听，例如开始，暂停，结束等

网络相关的功能用到了[network](https://github.com/zj565061763/network)

# 普通重试

```kotlin
/**
 * 网络已连接才发起重试
 */
class AppRetry : FNetRetry(
    // 设置最大重试次数
    maxRetryCount = 15
) {

    init {
        // 设置重试间隔
        setRetryInterval(1000)
    }

    override fun onStart() {
        // 开始回调(主线程)
    }

    override fun onPause() {
        // 暂停回调(主线程)
    }

    override fun onStop() {
        // 结束回调(主线程)
    }

    override fun onRetry(session: Session): Boolean {
        // 重试回调(主线程)

        // 继续重试
        session.retry()

        // 结束重试
        session.finish()

        // 返回false停止重试
        return true
    }

    override fun onRetryMaxCount() {
        // 达到最大重试次数回调(主线程)
    }
}
```

```kotlin
// 开始重试
FRetry.start(AppRetry::class.java)

// 停止重试
FRetry.stop(AppRetry::class.java)
```

# 协程重试

```kotlin
/**
 * 网络已连接才会执行，具体逻辑参考[fRetry]
 */
suspend fun <T> fNetRetry(
    /** 最大执行次数 */
    maxCount: Int = Int.MAX_VALUE,

    /** 执行间隔(毫秒) */
    interval: Long = 5_000,

    /** 失败回调 */
    onFailure: FRetryScope.(Throwable) -> Unit = {},

    /** 在[block]回调之前执行 */
    beforeBlock: suspend FRetryScope.() -> Unit = {},

    /** 执行回调 */
    block: suspend FRetryScope.() -> T,
): Result<T>
```

```kotlin
/**
 * 执行[block]，[block]发生的异常会被捕获([CancellationException]异常除外)并通知[onFailure]，[onFailure]的异常不会被捕获，
 * [block]发生异常之后，如果未达到最大执行次数[maxCount]，延迟[interval]之后继续执行[block]；
 * 如果达到最大执行次数[maxCount]，则返回的[Result]异常为[FRetryExceptionMaxCount]并携带最后一次的异常，
 * [beforeBlock]在[block]回调之前执行，[beforeBlock]发生的异常不会被捕获
 */
suspend fun <T> fRetry(
    /** 最大执行次数 */
    maxCount: Int = Int.MAX_VALUE,

    /** 执行间隔(毫秒) */
    interval: Long = 5_000,

    /** 失败回调 */
    onFailure: FRetryScope.(Throwable) -> Unit = {},

    /** 在[block]回调之前执行 */
    beforeBlock: suspend FRetryScope.() -> Unit = {},

    /** 执行回调 */
    block: suspend FRetryScope.() -> T,
): Result<T>
```

```kotlin
interface FRetryScope {
    /** 当前执行次数 */
    val currentCount: Int
}
```

```kotlin
/**
 * 达到最大执行次数
 */
class FRetryExceptionMaxCount(cause: Throwable) : Exception(cause)
```