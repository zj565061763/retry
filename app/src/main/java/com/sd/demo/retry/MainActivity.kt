package com.sd.demo.retry

import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import com.sd.demo.retry.databinding.ActivityMainBinding
import com.sd.lib.retry.FNetworkObserver

class MainActivity : AppCompatActivity() {
    private val _binding by lazy { ActivityMainBinding.inflate(layoutInflater) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(_binding.root)

        _binding.btnRetry.setOnClickListener {
            startActivity(Intent(this, RetryActivity::class.java))
        }
        _binding.btnRetryExt.setOnClickListener {
            startActivity(Intent(this, RetryExtActivity::class.java))
        }

        _networkObserver.register()
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

inline fun logMsg(block: () -> String) {
    Log.i("retry-demo", block())
}