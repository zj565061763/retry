package com.sd.demo.retry

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.util.Log
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.sd.demo.retry.ui.theme.AppTheme
import com.sd.lib.retry.FNetworkObserver

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        _networkObserver.register()
        setContent {
            AppTheme {
                Content()
            }
        }
    }

    private val _networkObserver = object : FNetworkObserver() {
        override fun onAvailable() {
            logMsg { "Network onAvailable" }
        }

        override fun onLost() {
            logMsg { "Network onLost" }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        _networkObserver.unregister()
    }
}

@Composable
private fun Content() {
    val activity = LocalContext.current as Activity
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(5.dp)
    ) {
        Button(
            onClick = {
                activity.startActivity(Intent(activity, RetryActivity::class.java))
            }
        ) {
            Text(text = "Retry")
        }

        Button(
            onClick = {

            }
        ) {
            Text(text = "RetryExt")
        }
    }
}

inline fun logMsg(block: () -> String) {
    Log.i("FRetry-demo", block())
}