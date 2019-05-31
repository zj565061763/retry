package com.sd.lib.retry;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;

/**
 * 需要用到网络的重试帮助类
 */
public abstract class FNetRetryHandler extends FRetryHandler
{
    private NetworkReceiver mNetworkReceiver;

    public FNetRetryHandler(Context context, int maxRetryCount)
    {
        super(maxRetryCount);
        mNetworkReceiver = new NetworkReceiver(context);
        mNetworkReceiver.register();
    }

    @Override
    protected final void onRetry()
    {
        if (mNetworkReceiver == null)
            throw new RuntimeException("current instance has been destroyed");

        final NetworkInfo networkInfo = mNetworkReceiver.getActiveNetworkInfo();
        if (networkInfo != null && networkInfo.isConnected())
        {
            onRetryImpl();
        } else
        {
            onRetryWhenNetworkDisconnected();
        }
    }

    /**
     * 重试的时候网络不可用回调
     */
    protected void onRetryWhenNetworkDisconnected()
    {
    }

    /**
     * 网络可用回调
     *
     * @param networkInfo
     */
    protected void onNetworkConnected(NetworkInfo networkInfo)
    {
        retry(0);
    }

    /**
     * 执行重试任务（UI线程）
     */
    protected abstract void onRetryImpl();

    /**
     * 销毁
     */
    public final synchronized void destroy()
    {
        stop();
        if (mNetworkReceiver != null)
        {
            mNetworkReceiver.unregister();
            mNetworkReceiver = null;
        }
    }

    private final class NetworkReceiver extends BroadcastReceiver
    {
        private final Context mContext;

        public NetworkReceiver(Context context)
        {
            mContext = context.getApplicationContext();
        }

        @Override
        public void onReceive(Context context, Intent intent)
        {
            if (ConnectivityManager.CONNECTIVITY_ACTION.equals(intent.getAction()))
            {
                final NetworkInfo networkInfo = getActiveNetworkInfo();
                if (networkInfo != null && networkInfo.isConnected())
                {
                    onNetworkConnected(networkInfo);
                }
            }
        }

        public NetworkInfo getActiveNetworkInfo()
        {
            final ConnectivityManager manager = (ConnectivityManager) mContext.getSystemService(Context.CONNECTIVITY_SERVICE);
            return manager.getActiveNetworkInfo();
        }

        public void register()
        {
            final IntentFilter filter = new IntentFilter();
            filter.addAction(ConnectivityManager.CONNECTIVITY_ACTION);
            mContext.registerReceiver(this, filter);
        }

        public void unregister()
        {
            mContext.unregisterReceiver(this);
        }
    }
}
