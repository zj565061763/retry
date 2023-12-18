package com.sd.demo.retry

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.sd.demo.retry.databinding.ActivityRetryKtxBinding
import com.sd.lib.coroutine.FScope
import com.sd.lib.retry.fNetRetry
import java.util.UUID

class RetryKtxActivity : AppCompatActivity() {
    private val _binding by lazy { ActivityRetryKtxBinding.inflate(layoutInflater) }

    private val _scope = FScope()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(_binding.root)
        _binding.btnStart.setOnClickListener {
            _scope.launch { retry() }
        }
        _binding.btnStop.setOnClickListener {
            _scope.cancel()
        }
    }

    private suspend fun retry() {
        val uuid = UUID.randomUUID().toString()
        logMsg { "start $uuid" }

        val result = try {
            fNetRetry(maxCount = 15) {
                if (retryCount >= 10) {
                    Result.success("hello")
                } else {
                    Result.failure(Throwable(""))
                }
            }
        } catch (e: Exception) {
            logMsg { "exception $uuid $e" }
            throw e
        }

        result.onSuccess {
            logMsg { "onSuccess $it $uuid" }
        }

        result.onFailure {
            logMsg { "onFailure $it $uuid" }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        _scope.cancel()
    }
}