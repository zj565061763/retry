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
import androidx.lifecycle.lifecycleScope
import com.sd.demo.retry.theme.AppTheme
import com.sd.lib.retry.fNetRetry
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import java.util.UUID

class SampleRetryKtx : ComponentActivity() {

    private var _retryJob: Job? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            AppTheme {
                ContentView(
                    onClickStart = {
                        cancelRetry()
                        _retryJob = lifecycleScope.launch { retry() }
                    },
                    onClickCancel = {
                        cancelRetry()
                    },
                )
            }
        }
    }

    private suspend fun retry() {
        val uuid = UUID.randomUUID().toString()
        logMsg { "$uuid start" }
        fNetRetry(
            maxCount = 15,
            interval = 1_000,
            onFailure = { logMsg { "onFailure:$it" } },
        ) {
            logMsg { "retry $currentCount" }
            if (currentCount >= 10) {
                "hello"
            } else {
                error("failure $currentCount")
            }
        }.onSuccess {
            logMsg { "$uuid onSuccess $it" }
        }.onFailure {
            logMsg { "$uuid onFailure $it" }
        }
    }

    private fun cancelRetry() {
        _retryJob?.cancel()
    }
}

@Composable
private fun ContentView(
    modifier: Modifier = Modifier,
    onClickStart: () -> Unit,
    onClickCancel: () -> Unit,
) {
    Column(
        modifier = modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Button(onClick = onClickStart) {
            Text(text = "Start")
        }

        Button(onClick = onClickCancel) {
            Text(text = "Cancel")
        }
    }
}