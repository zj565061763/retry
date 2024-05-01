package com.sd.lib.retry

import com.sd.lib.network.FNetwork
import com.sd.lib.network.FNetworkObserver
import com.sd.lib.network.NetworkState

/**
 * [isNetConnected]返回true，才会发起重试
 */
abstract class FNetRetry(
    /** 最大重试次数 */
    maxRetryCount: Int,
) : FRetry(maxRetryCount = maxRetryCount) {

    override fun checkRetry(): Boolean {
        if (!isNetConnected(FNetwork.currentNetwork)) {
            _networkObserver.register()
            return false
        }
        return super.checkRetry()
    }

    /**
     * 网络监听
     */
    private val _networkObserver = object : FNetworkObserver() {
        override fun onChange(networkState: NetworkState) {
            if (isNetConnected(networkState)) {
                unregister()
                resumeRetry()
            }
        }
    }

    /**
     * 返回网络是否已连接
     */
    protected open fun isNetConnected(networkState: NetworkState): Boolean {
        return networkState.isConnected()
    }

    override fun onStop() {
        super.onStop()
        // 取消网络监听
        _networkObserver.unregister()
    }
}