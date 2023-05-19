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
import com.sd.lib.retry.fNetRetry
import kotlinx.coroutines.Job
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

class RetryExtActivity : ComponentActivity() {
    private val _scope = MainScope()
    private var _retryJob: Job? = null
    private var _count = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            AppTheme {
                Content(
                    onClickStart = {
                        startRetry()
                    },
                    onClickStop = {
                        stopRetry()
                    }
                )
            }
        }
    }

    private fun startRetry() {
        stopRetry()
        _scope.launch {
            val result = fNetRetry(
                maxRetryCount = 15,
                retryInterval = 1000,
                onStart = { logMsg { "retry onStart" } },
                onPause = { logMsg { "retry onPause" } },
                onStop = { logMsg { "retry onStop" } },
                onRetryMaxCount = { logMsg { "retry onRetryMaxCount" } },
            ) {
                _count++
                logMsg { "retry $_count" }
                if (_count >= 10) {
                    Result.success("success")
                } else {
                    Result.failure(Exception("failure"))
                }
            }
            result.onSuccess {
                logMsg { "retry success $it" }
            }
            result.onFailure {
                logMsg { "retry failure $it" }
            }
        }.also {
            _retryJob = it
        }
    }

    private fun stopRetry() {
        _retryJob?.cancel()
        _count = 0
    }

    override fun onDestroy() {
        super.onDestroy()
        _scope.cancel()
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