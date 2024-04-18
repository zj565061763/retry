[![](https://jitpack.io/v/zj565061763/retry.svg)](https://jitpack.io/#zj565061763/retry)

# About

对重试逻辑进行封装，提供健壮的重试逻辑，如果你想要使用协程的重试Api，可以查看[retry-ktx](https://github.com/zj565061763/retry-ktx)

* 支持设置重试间隔，最大重试次数
* 支持自定义重试的条件，例如网络可用才发起重试
* 支持重试状态监听，例如开始，暂停，结束等

# Sample

#### 普通用法

1. 定义重试类

```kotlin
class AppRetry : FRetry(
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

2. 开始和停止重试

```kotlin
// 开始重试
FRetry.start(AppRetry::class.java)

// 停止重试
FRetry.stop(AppRetry::class.java)
```