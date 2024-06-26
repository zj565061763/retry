package com.sd.lib.retry

import com.sd.lib.network.FNetwork
import com.sd.lib.network.FNetworkObserver
import com.sd.lib.network.NetworkState

/**
 * [isNetConnected]返回true，才会发起重试
 */
abstract class FNetRetry @JvmOverloads constructor(
    /** 最大重试次数 */
    maxRetryCount: Int = 5,
) : FRetry(maxRetryCount = maxRetryCount) {

    private var _networkObserver: FNetworkObserver? = null

    override fun canRetry(): Boolean {
        if (!isNetConnected(FNetwork.currentNetwork)) {
            registerObserver()
            return false
        }
        return super.canRetry()
    }

    /**
     * 返回网络是否已连接
     */
    protected open fun isNetConnected(networkState: NetworkState): Boolean {
        return networkState.isConnected()
    }

    override fun onStop() {
        super.onStop()
        unregisterObserver()
    }

    private fun registerObserver() {
        if (_networkObserver == null) {
            _networkObserver = object : FNetworkObserver() {
                override fun onChange(networkState: NetworkState) {
                    if (isNetConnected(networkState)) {
                        unregister()
                        resumeRetry()
                    }
                }
            }
        }
        _networkObserver!!.register()
    }

    private fun unregisterObserver() {
        _networkObserver?.unregister()
        _networkObserver = null
    }
}