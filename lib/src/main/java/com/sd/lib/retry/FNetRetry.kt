package com.sd.lib.retry

import com.sd.lib.network.FNetwork
import com.sd.lib.network.FNetworkObserver
import com.sd.lib.network.NetworkState

abstract class FNetRetry(maxRetryCount: Int) : FRetry(maxRetryCount) {

    override fun checkRetry(): Boolean {
        if (!FNetwork.currentNetwork.isConnected()) {
            _networkObserver.register()
            return false
        }
        return super.checkRetry()
    }

    override fun onStop() {
        super.onStop()
        _networkObserver.unregister()
    }

    private val _networkObserver = object : FNetworkObserver() {
        override fun onChange(networkState: NetworkState) {
            if (networkState.isConnected()) {
                resumeRetry()
            }
        }
    }
}