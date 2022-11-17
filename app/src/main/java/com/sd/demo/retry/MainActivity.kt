package com.sd.demo.retry

import android.os.Bundle
import android.util.Log
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import com.sd.demo.retry.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity(), View.OnClickListener {
    private val _binding by lazy { ActivityMainBinding.inflate(layoutInflater) }
    private val _retry by lazy { AppRetry(this) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(_binding.root)
    }

    override fun onClick(v: View) {
        when (v) {
            _binding.btnStart -> _retry.start()
            _binding.btnCancel -> _retry.cancel()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        _retry.cancel()
    }
}

inline fun logMsg(block: () -> String) {
    Log.i("FRetry-demo", block())
}