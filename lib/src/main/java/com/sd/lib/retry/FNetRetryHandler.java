package com.sd.lib.retry;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.util.Log;

import androidx.annotation.NonNull;

/**
 * 需要用到网络的重试帮助类
 */
public abstract class FNetRetryHandler extends FRetryHandler {
    private final Context mContext;
    private NetworkReceiver mNetworkReceiver;

    public FNetRetryHandler(@NonNull Context context, int maxRetryCount) {
        super(maxRetryCount);
        mContext = context.getApplicationContext();
    }

    @Override
    protected void onStateChanged(boolean started) {
        super.onStateChanged(started);
        if (started) {
            registerReceiver();
        } else {
            unregisterReceiver();
        }
    }

    private void registerReceiver() {
        if (mNetworkReceiver == null) {
            mNetworkReceiver = new NetworkReceiver(mContext);
            mNetworkReceiver.register();
            Log.i(getClass().getSimpleName(), "registerReceiver");
        }
    }

    private void unregisterReceiver() {
        if (mNetworkReceiver != null) {
            mNetworkReceiver.unregister();
            mNetworkReceiver = null;
            Log.i(getClass().getSimpleName(), "unregisterReceiver");
        }
    }

    @Override
    protected boolean checkRetry() {
        if (mNetworkReceiver == null) {
            throw new RuntimeException("NetworkReceiver instance is null");
        }

        final NetworkInfo networkInfo = mNetworkReceiver.getActiveNetworkInfo();
        if (networkInfo == null) {
            return false;
        }

        if (!networkInfo.isConnected()) {
            return false;
        }

        return super.checkRetry();
    }

    /**
     * 网络可用回调
     *
     * @param networkInfo
     */
    protected void onNetworkConnected(NetworkInfo networkInfo) {
        synchronized (FNetRetryHandler.this) {
            final boolean isLoading = isLoading();
            Log.i(getClass().getSimpleName(), "onNetworkConnected isLoading:" + isLoading);
            if (!isLoading) {
                retry(0);
            }
        }
    }

    private final class NetworkReceiver extends BroadcastReceiver {
        private final Context mContext;

        public NetworkReceiver(Context context) {
            mContext = context.getApplicationContext();
        }

        @Override
        public void onReceive(Context context, Intent intent) {
            if (ConnectivityManager.CONNECTIVITY_ACTION.equals(intent.getAction())) {
                final NetworkInfo networkInfo = getActiveNetworkInfo();
                if (networkInfo != null && networkInfo.isConnected()) {
                    onNetworkConnected(networkInfo);
                }
            }
        }

        public NetworkInfo getActiveNetworkInfo() {
            final ConnectivityManager manager = (ConnectivityManager) mContext.getSystemService(Context.CONNECTIVITY_SERVICE);
            return manager.getActiveNetworkInfo();
        }

        public void register() {
            final IntentFilter filter = new IntentFilter();
            filter.addAction(ConnectivityManager.CONNECTIVITY_ACTION);
            mContext.registerReceiver(this, filter);
        }

        public void unregister() {
            mContext.unregisterReceiver(this);
        }
    }
}
