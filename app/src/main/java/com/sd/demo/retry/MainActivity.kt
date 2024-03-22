package com.sd.demo.retry

import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import com.sd.demo.retry.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {
    private val _binding by lazy { ActivityMainBinding.inflate(layoutInflater) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(_binding.root)

        _binding.btnSampleRetry.setOnClickListener {
            startActivity(Intent(this, SampleRetry::class.java))
        }

        _binding.btnSampleRetryKtx.setOnClickListener {
            startActivity(Intent(this, SampleRetryKtx::class.java))
        }
    }
}

inline fun logMsg(block: () -> String) {
    Log.i("retry-demo", block())
}