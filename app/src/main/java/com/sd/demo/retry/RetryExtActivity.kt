package com.sd.demo.retry

import android.os.Bundle
import androidx.activity.ComponentActivity
import com.sd.demo.retry.databinding.ActivityRetryExtBinding
import com.sd.lib.retry.fNetRetry
import kotlinx.coroutines.Job
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

class RetryExtActivity : ComponentActivity() {
    private val _binding by lazy { ActivityRetryExtBinding.inflate(layoutInflater) }

    private val _scope = MainScope()
    private var _retryJob: Job? = null
    private var _count = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(_binding.root)
        _binding.btnStart.setOnClickListener {
            startRetry()
        }
        _binding.btnStop.setOnClickListener {
            stopRetry()
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