package com.sd.lib.retry;

import android.os.Handler;
import android.os.Looper;

import androidx.annotation.NonNull;

/**
 * 重试帮助类
 */
public abstract class FRetryHandler {
    /** 最大重试次数 */
    private final int mMaxRetryCount;

    /** 重试是否已经开始 */
    private volatile boolean mIsStarted;

    /** 当前第几次重试 */
    private volatile int mRetryCount;

    /** 某一次重试是否正在加载中 */
    private volatile boolean mIsLoading;

    /** 重试间隔 */
    private volatile long mRetryInterval = 3 * 1000;

    private InternalLoadSession mLoadSession;
    private final Handler mHandler = new Handler(Looper.getMainLooper());

    public FRetryHandler(int maxRetryCount) {
        if (maxRetryCount <= 0) {
            throw new IllegalArgumentException("maxRetryCount must > 0");
        }
        mMaxRetryCount = maxRetryCount;
    }

    /**
     * 是否已经开始重试
     */
    public final boolean isStarted() {
        return mIsStarted;
    }

    /**
     * 返回当前第几次重试
     */
    public final int getRetryCount() {
        return mRetryCount;
    }

    /**
     * 某一次重试是否正在加载中
     */
    public final boolean isLoading() {
        return mIsLoading;
    }

    /**
     * 设置重试间隔，默认3000毫秒
     */
    public final void setRetryInterval(long interval) {
        if (interval < 0) {
            interval = 0;
        }
        mRetryInterval = interval;
    }

    /**
     * 开始重试
     */
    public final synchronized void start() {
        if (mIsStarted) {
            return;
        }
        setStarted(true);
        retry(0);
    }

    /**
     * 停止重试
     */
    public final void cancel() {
        cancelInternal(true);
    }

    /**
     * 重试
     *
     * @param delayMillis 延迟多少毫秒
     */
    final synchronized void retry(long delayMillis) {
        if (!mIsStarted) {
            return;
        }

        if (mRetryCount >= mMaxRetryCount) {
            // 达到最大重试次数
            cancelInternal(false);
            onRetryMaxCount();
            return;
        }

        if (mIsLoading) {
            throw new RuntimeException("can not retry while loading");
        }

        if (mLoadSession != null && !mLoadSession._isFinish) {
            throw new RuntimeException("last load session is not finished");
        }

        if (checkRetry()) {
            mHandler.removeCallbacks(mRetryRunnable);
            mHandler.postDelayed(mRetryRunnable, delayMillis);
        }
    }

    private final Runnable mRetryRunnable = new Runnable() {
        @Override
        public void run() {
            synchronized (FRetryHandler.this) {
                if (mIsStarted && checkRetry()) {
                    mRetryCount++;
                    mLoadSession = new InternalLoadSession();
                    onRetry(mLoadSession);
                }
            }
        }
    };

    private synchronized void cancelInternal(boolean cancelSession) {
        if (!mIsStarted) {
            return;
        }

        mHandler.removeCallbacks(mRetryRunnable);
        if (mLoadSession != null) {
            mLoadSession._isFinish = true;
            mLoadSession = null;
        }

        final boolean isLoading = mIsLoading;
        setStarted(false);

        if (isLoading && cancelSession) {
            cancelLoadSession();
        }
    }

    private void setStarted(boolean started) {
        if (mIsStarted != started) {
            mRetryCount = 0;
            mIsLoading = false;

            mIsStarted = started;
            onStateChanged(started);
        }
    }

    /**
     * 检查是否可以发起重试
     *
     * @return true-是；false-否
     */
    protected boolean checkRetry() {
        return true;
    }

    /**
     * 是否开始状态变化
     *
     * @param started true-开始；false-结束
     */
    protected void onStateChanged(boolean started) {
    }

    /**
     * 调用{@link #cancel()}的时候如果发现正在加载中，则会触发此方法取消加载
     */
    protected void cancelLoadSession() {
    }

    /**
     * 执行重试任务（UI线程）
     *
     * @param session
     */
    protected abstract void onRetry(@NonNull LoadSession session);

    /**
     * 达到最大重试次数
     */
    protected void onRetryMaxCount() {
    }

    private final class InternalLoadSession implements LoadSession {
        private volatile boolean _isFinish;

        @Override
        public void onLoading() {
            if (_isFinish) {
                return;
            }

            synchronized (FRetryHandler.this) {
                if (mIsStarted) {
                    mIsLoading = true;
                }
            }
        }

        @Override
        public void onLoadFinish() {
            if (_isFinish) {
                return;
            }
            _isFinish = true;

            cancelInternal(false);
        }

        @Override
        public void onLoadError() {
            if (_isFinish) {
                return;
            }
            _isFinish = true;

            synchronized (FRetryHandler.this) {
                mIsLoading = false;
                retry(mRetryInterval);
            }
        }
    }

    public interface LoadSession {
        void onLoading();

        void onLoadFinish();

        void onLoadError();
    }
}
