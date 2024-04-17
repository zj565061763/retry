package com.sd.demo.retry

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.sd.demo.retry.theme.AppTheme
import com.sd.lib.network.FNetwork
import com.sd.lib.network.FNetworkObserver
import com.sd.lib.network.NetworkState
import com.sd.lib.retry.FRetry

class SampleRetry : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // 初始化网络库
        FNetwork.init(this)

        setContent {
            AppTheme {
                ContentView(
                    onClickRetry = {
                        FRetry.start(AppRetry::class.java)
                    },
                    onClickCancel = {
                        FRetry.stop(AppRetry::class.java)
                    },
                )
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        FRetry.stop(AppRetry::class.java)
    }
}

@Composable
private fun ContentView(
    modifier: Modifier = Modifier,
    onClickRetry: () -> Unit,
    onClickCancel: () -> Unit,
) {
    Column(
        modifier = modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Button(onClick = onClickRetry) {
            Text(text = "Retry")
        }

        Button(onClick = onClickCancel) {
            Text(text = "Cancel")
        }
    }
}

internal class AppRetry : FRetry(15) {

    init {
        setRetryInterval(1000)
    }

    override fun checkRetry(): Boolean {
        if (!FNetwork.currentNetwork.isConnected()) {
            // 如果网络未连接，则注册网络监听，并返回false，暂停重试
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
            if (networkState.isConnected()) {
                // 网络已连接，恢复重试
                resumeRetry()
            }
        }
    }

    override fun onStart() {
        super.onStart()
        logMsg { "$this onStart" }
    }

    override fun onPause() {
        super.onPause()
        logMsg { "$this onPause" }
    }

    override fun onStop() {
        super.onStop()
        logMsg { "$this onStop" }

        // 取消网络监听
        _networkObserver.unregister()
    }

    override fun onRetry(session: Session): Boolean {
        logMsg { "$this onRetry $retryCount" }

        if (retryCount >= 10) {
            session.finish()
        } else {
            session.retry()
        }

        return true
    }

    override fun onRetryMaxCount() {
        super.onRetryMaxCount()
        logMsg { "$this onRetryMaxCount" }
    }
}