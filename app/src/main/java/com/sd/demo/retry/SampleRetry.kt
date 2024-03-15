package com.sd.demo.retry

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.sd.demo.retry.databinding.SampleRetryBinding
import com.sd.lib.retry.FNetRetry
import com.sd.lib.retry.FRetry

class SampleRetry : AppCompatActivity() {
    private val _binding by lazy { SampleRetryBinding.inflate(layoutInflater) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(_binding.root)
        _binding.btnStart.setOnClickListener {
            FRetry.start(AppRetry::class.java)
        }
        _binding.btnStop.setOnClickListener {
            FRetry.stop(AppRetry::class.java)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        FRetry.stop(AppRetry::class.java)
    }
}

internal class AppRetry : FNetRetry(15) {

    init {
        setRetryInterval(1000)
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