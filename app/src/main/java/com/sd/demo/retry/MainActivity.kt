package com.sd.demo.retry

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
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.sd.demo.retry.ui.theme.AppTheme
import com.sd.lib.retry.utils.FNetworkObserver

class MainActivity : ComponentActivity() {
    private val _retry = AppRetry(15)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        _networkObserver.register()
        setContent {
            AppTheme {
                Content(
                    onClickStart = {
                        _retry.start()
                    },
                    onClickCancel = {
                        _retry.cancel()
                    }
                )
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
private fun Content(
    onClickStart: () -> Unit,
    onClickCancel: () -> Unit,
) {
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(5.dp)
    ) {
        Button(
            onClick = onClickStart
        ) {
            Text(text = "start")
        }

        Button(
            onClick = onClickCancel
        ) {
            Text(text = "cancel")
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun DefaultPreview() {
    AppTheme {
        Content(
            onClickStart = {},
            onClickCancel = {},
        )
    }
}

inline fun logMsg(block: () -> String) {
    Log.i("FRetry-demo", block())
}