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
import com.sd.demo.retry.ui.theme.AppTheme
import com.sd.lib.retry.FNetRetry

class RetryActivity : ComponentActivity() {
    private val _retry = AppRetry(15)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            AppTheme {
                Content(
                    onClickStart = {
                        _retry.startRetry()
                    },
                    onClickStop = {
                        _retry.stopRetry()
                    }
                )
            }
        }
    }
}

@Composable
private fun Content(
    onClickStart: () -> Unit,
    onClickStop: () -> Unit,
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
            onClick = onClickStop
        ) {
            Text(text = "stop")
        }
    }
}

private class AppRetry(maxRetryCount: Int) : FNetRetry(maxRetryCount) {

    init {
        setRetryInterval(1000)
    }

    override fun onStart() {
        super.onStart()
        logMsg { "AppRetry onStart" }
    }

    override fun onPause() {
        super.onPause()
        logMsg { "AppRetry onPause" }
    }

    override fun onStop() {
        super.onStop()
        logMsg { "AppRetry onStop" }
    }

    override fun onRetry(session: Session): Boolean {
        logMsg { "AppRetry onRetry $retryCount" }

        if (retryCount >= 10) {
            session.finish()
        } else {
            session.retry()
        }

        return true
    }

    override fun onRetryMaxCount() {
        super.onRetryMaxCount()
        logMsg { "AppRetry onRetryMaxCount" }
    }
}