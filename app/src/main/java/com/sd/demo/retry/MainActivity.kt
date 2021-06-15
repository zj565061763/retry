package com.sd.demo.retry

import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import com.sd.demo.retry.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity(), View.OnClickListener {
    private lateinit var _binding: ActivityMainBinding
    private lateinit var _retryHandler: MyRetryHandler

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        _binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(_binding.root)
        _retryHandler = MyRetryHandler(this)
    }

    override fun onClick(v: View) {
        when (v) {
            _binding.btnStart -> _retryHandler.start()
            _binding.btnStop -> _retryHandler.cancel()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        _retryHandler.cancel()
    }
}