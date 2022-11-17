package com.sd.lib.retry

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.os.Build
import androidx.annotation.RequiresApi
import java.util.concurrent.atomic.AtomicBoolean

abstract class NetworkObserver(context: Context) {
    private val _context = context.applicationContext

    private val _networkCallback by lazy { InternalNetworkCallback() }
    private val _networkReceiver by lazy { InternalNetworkReceiver() }

    private var _isNetworkAvailable: Boolean = isNetworkAvailable(context)
        set(value) {
            if (field != value) {
                field = value
                onNetworkChanged(value)
            }
        }

    val isNetworkAvailable: Boolean
        get() = _isNetworkAvailable

    fun register() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            _networkCallback.register()
        } else {
            _networkReceiver.register()
        }
    }

    fun unregister() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            _networkCallback.unregister()
        } else {
            _networkReceiver.unregister()
        }
    }

    abstract fun onNetworkChanged(isNetworkAvailable: Boolean)

    // Callback
    private inner class InternalNetworkCallback : ConnectivityManager.NetworkCallback() {
        private val _hasRegister = AtomicBoolean()

        override fun onAvailable(network: Network) {
            super.onAvailable(network)
            _isNetworkAvailable = true
        }

        override fun onUnavailable() {
            super.onUnavailable()
            _isNetworkAvailable = false
        }

        @RequiresApi(Build.VERSION_CODES.N)
        fun register() {
            if (_hasRegister.compareAndSet(false, true)) {
                val manager = _context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
                manager.registerDefaultNetworkCallback(this)
            }
        }

        fun unregister() {
            if (_hasRegister.compareAndSet(true, false)) {
                val manager = _context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
                manager.unregisterNetworkCallback(this)
            }
        }
    }

    // Receiver
    private inner class InternalNetworkReceiver : BroadcastReceiver() {
        private val _hasRegister = AtomicBoolean()

        init {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) error("Please use the new api.")
        }

        override fun onReceive(context: Context, intent: Intent) {
            if (ConnectivityManager.CONNECTIVITY_ACTION == intent.action) {
                _isNetworkAvailable = isNetworkAvailable(context, Int.MAX_VALUE)
            }
        }

        fun register() {
            if (_hasRegister.compareAndSet(false, true)) {
                _context.registerReceiver(this, IntentFilter().apply {
                    addAction(ConnectivityManager.CONNECTIVITY_ACTION)
                })
            }
        }

        fun unregister() {
            if (_hasRegister.compareAndSet(true, false)) {
                _context.unregisterReceiver(this)
            }
        }
    }

    companion object {
        private fun isNetworkAvailable(context: Context, useNewApiLevel: Int = Build.VERSION_CODES.M): Boolean {
            require(useNewApiLevel >= Build.VERSION_CODES.M)
            val manager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
            if (Build.VERSION.SDK_INT >= useNewApiLevel) {
                val network = manager.activeNetwork ?: return false
                val capabilities = manager.getNetworkCapabilities(network) ?: return false
                return when {
                    capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> true
                    capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> true
                    capabilities.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) -> true
                    capabilities.hasTransport(NetworkCapabilities.TRANSPORT_BLUETOOTH) -> true
                    else -> false
                }
            } else {
                return manager.activeNetworkInfo?.isConnected ?: return false
            }
        }
    }
}