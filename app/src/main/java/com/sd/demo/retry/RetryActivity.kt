package com.sd.demo.retry

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.sd.demo.retry.databinding.ActivityRetryBinding
import com.sd.lib.retry.FNetRetry

class RetryActivity : AppCompatActivity() {
    private val _binding by lazy { ActivityRetryBinding.inflate(layoutInflater) }
    private val _retry = AppRetry(15)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(_binding.root)
        _binding.btnStart.setOnClickListener {
            _retry.startRetry()
        }
        _binding.btnStop.setOnClickListener {
            _retry.stopRetry()
        }
    }
}

private class AppRetry(maxRetryCount: Int) : FNetRetry(maxRetryCount) {

    init {
        setRetryInterval(1000)
    }

    override fun onStart() {
        super.onStart()
        logMsg { "onStart" }
    }

    override fun onPause() {
        super.onPause()
        logMsg { "onPause" }
    }

    override fun onStop() {
        super.onStop()
        logMsg { "onStop" }
    }

    override fun onRetry(session: Session): Boolean {
        logMsg { "onRetry $retryCount" }

        if (retryCount >= 10) {
            session.finish()
        } else {
            session.retry()
        }

        return true
    }

    override fun onRetryMaxCount() {
        super.onRetryMaxCount()
        logMsg { "onRetryMaxCount" }
    }
}