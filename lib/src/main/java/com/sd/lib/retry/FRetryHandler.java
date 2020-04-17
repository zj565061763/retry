package com.sd.lib.retry;

import android.os.Handler;
import android.os.Looper;

/**
 * 重试帮助类
 */
public abstract class FRetryHandler
{
    /**
     * 最大重试次数
     */
    private final int mMaxRetryCount;

    /**
     * 重试是否已经开始
     */
    private volatile boolean mIsStarted;
    /**
     * 当前第几次重试
     */
    private volatile int mRetryCount;
    /**
     * 某一次重试是否正在加载中
     */
    private volatile boolean mIsLoading;
    /**
     * 重试间隔
     */
    private long mRetryInterval = 3 * 1000;

    private final Handler mHandler = new Handler(Looper.getMainLooper());

    public FRetryHandler(int maxRetryCount)
    {
        if (maxRetryCount <= 0)
            throw new IllegalArgumentException("maxRetryCount must > 0");
        mMaxRetryCount = maxRetryCount;
    }

    /**
     * 是否已经开始重试
     *
     * @return
     */
    public final boolean isStarted()
    {
        return mIsStarted;
    }

    /**
     * 返回当前第几次重试
     *
     * @return
     */
    public final int getRetryCount()
    {
        return mRetryCount;
    }

    /**
     * 某一次重试是否正在加载中
     *
     * @return
     */
    public final boolean isLoading()
    {
        return mIsLoading;
    }

    /**
     * 设置重试间隔，默认3000毫秒
     *
     * @param retryInterval
     */
    public synchronized void setRetryInterval(long retryInterval)
    {
        if (retryInterval < 0)
            retryInterval = 0;
        mRetryInterval = retryInterval;
    }

    /**
     * 开始重试
     */
    public final synchronized void start()
    {
        if (mIsStarted)
            return;

        setStarted(true);
        retry(0);
    }

    /**
     * 重试，只有{@link #isStarted()}为true，此方法才有效
     *
     * @param delayMillis 延迟多少毫秒
     * @return true-成功发起了一次重试
     */
    final synchronized boolean retry(long delayMillis)
    {
        if (!mIsStarted)
            return false;

        if (checkIsMaxRetry())
            return false;

        if (mIsLoading)
            throw new RuntimeException("can not retry while loading");

        if (!checkRetry())
            return false;

        mHandler.removeCallbacks(mRetryRunnable);
        mHandler.postDelayed(mRetryRunnable, delayMillis);
        return true;
    }

    /**
     * 检查是否可以发起重试
     *
     * @return
     */
    protected boolean checkRetry()
    {
        return true;
    }

    private InternalLoadSession mLoadSession;

    private final Runnable mRetryRunnable = new Runnable()
    {
        @Override
        public void run()
        {
            synchronized (FRetryHandler.this)
            {
                if (!mIsStarted)
                    return;

                if (mLoadSession != null)
                {
                    if (!mLoadSession.nIsFinish)
                        throw new RuntimeException("last load session is not finished");
                }

                mRetryCount++;
                mLoadSession = new InternalLoadSession();
                onRetry(mLoadSession);
            }
        }
    };

    /**
     * 检查是否达到最大重试次数，如果达到的话会停止重试，并回调{@link #onRetryMaxCount()}方法
     *
     * @return true-达到最大次数
     */
    private boolean checkIsMaxRetry()
    {
        if (mRetryCount >= mMaxRetryCount)
        {
            // 达到最大重试次数
            cancelInternal(false);
            onRetryMaxCount();
            return true;
        }
        return false;
    }

    /**
     * 停止重试
     */
    public final void cancel()
    {
        cancelInternal(true);
    }

    private synchronized void cancelInternal(boolean cancelSession)
    {
        if (mIsStarted)
        {
            mHandler.removeCallbacks(mRetryRunnable);

            if (mLoadSession != null)
            {
                mLoadSession.nIsFinish = true;
                mLoadSession = null;
            }

            final boolean isLoading = mIsLoading;
            setStarted(false);

            if (isLoading && cancelSession)
                cancelLoadSession();
        }
    }

    private void setStarted(boolean started)
    {
        if (mIsStarted != started)
        {
            mRetryCount = 0;
            mIsLoading = false;

            mIsStarted = started;
            onStateChanged(started);
        }
    }

    protected void onStateChanged(boolean started)
    {
    }

    /**
     * 调用{@link #cancel()}的时候如果发现正在加载中，则会触发此方法取消加载
     */
    protected void cancelLoadSession()
    {
    }

    /**
     * 执行重试任务（UI线程）
     *
     * @param session
     */
    protected abstract void onRetry(LoadSession session);

    /**
     * 达到最大重试次数
     */
    protected void onRetryMaxCount()
    {
    }

    private final class InternalLoadSession implements LoadSession
    {
        private volatile boolean nIsFinish;

        @Override
        public void onLoading()
        {
            if (nIsFinish)
                return;

            synchronized (FRetryHandler.this)
            {
                if (mIsStarted)
                    mIsLoading = true;
            }
        }

        @Override
        public void onLoadSuccess()
        {
            if (nIsFinish)
                return;
            nIsFinish = true;

            cancelInternal(false);
        }

        @Override
        public void onLoadError()
        {
            if (nIsFinish)
                return;
            nIsFinish = true;

            synchronized (FRetryHandler.this)
            {
                mIsLoading = false;
                retry(mRetryInterval);
            }
        }
    }

    public interface LoadSession
    {
        void onLoading();

        void onLoadSuccess();

        void onLoadError();
    }
}
