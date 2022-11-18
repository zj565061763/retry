package com.sd.demo.retry

import android.os.Bundle
import android.util.Log
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import com.sd.demo.retry.databinding.ActivityMainBinding
import com.sd.lib.retry.utils.FNetworkObserver

class MainActivity : AppCompatActivity(), View.OnClickListener {
    private val _binding by lazy { ActivityMainBinding.inflate(layoutInflater) }

    private val _retry = AppRetry(15)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(_binding.root)
        _networkObserver.register()
    }

    override fun onClick(v: View) {
        when (v) {
            _binding.btnStart -> _retry.start()
            _binding.btnCancel -> _retry.cancel()
        }
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
        _retry.cancel()
        _networkObserver.unregister()
        logMsg { "onDestroy isNetworkAvailable:${FNetworkObserver.isNetworkAvailable()}" }
    }
}

inline fun logMsg(block: () -> String) {
    Log.i("FRetry-demo", block())
}