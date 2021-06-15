package com.sd.demo.retry

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {
    private lateinit var _retryHandler: MyRetryHandler

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        _retryHandler = MyRetryHandler(this)
        _retryHandler.start()
    }

    override fun onDestroy() {
        super.onDestroy()
        _retryHandler.cancel()
    }
}