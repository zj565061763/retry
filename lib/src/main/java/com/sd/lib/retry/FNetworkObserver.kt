package com.sd.lib.retry

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.os.Build
import android.os.Handler
import android.os.Looper
import com.sd.lib.ctx.fContext
import java.util.concurrent.atomic.AtomicBoolean

abstract class FNetworkObserver {
    private val _context get() = fContext

    private val _networkCallback by lazy { InternalNetworkCallback() }
    private val _networkReceiver by lazy { InternalNetworkReceiver() }

    private var _isNetworkAvailable: Boolean? = null

    fun register() {
        notifyNetworkAvailable(isNetworkAvailable())

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            _networkCallback.register(_context)
        } else {
            _networkReceiver.register(_context)
        }
    }

    fun unregister() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            _networkCallback.unregister(_context)
        } else {
            _networkReceiver.unregister(_context)
        }

        notifyNetworkAvailable(null)
    }

    private fun notifyNetworkAvailable(isAvailable: Boolean?) {
        synchronized(this@FNetworkObserver) {
            if (_isNetworkAvailable != isAvailable) {
                _isNetworkAvailable = isAvailable
                if (isAvailable != null) {
                    Handler(Looper.getMainLooper()).post {
                        if (isAvailable) onAvailable() else onLost()
                    }
                }
            }
        }
    }

    abstract fun onAvailable()

    abstract fun onLost()

    // Callback
    private inner class InternalNetworkCallback : ConnectivityManager.NetworkCallback() {
        private val _hasRegister = AtomicBoolean()

        override fun onAvailable(network: Network) {
            super.onAvailable(network)
            notifyNetworkAvailable(true)
        }

        override fun onLost(network: Network) {
            super.onLost(network)
            notifyNetworkAvailable(false)
        }

        fun register(context: Context) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                if (_hasRegister.compareAndSet(false, true)) {
                    val manager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
                    manager.registerDefaultNetworkCallback(this)
                }
            }
        }

        fun unregister(context: Context) {
            if (_hasRegister.compareAndSet(true, false)) {
                val manager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
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
                notifyNetworkAvailable(isNetworkAvailable())
            }
        }

        fun register(context: Context) {
            if (_hasRegister.compareAndSet(false, true)) {
                context.registerReceiver(this, IntentFilter().apply {
                    addAction(ConnectivityManager.CONNECTIVITY_ACTION)
                })
            }
        }

        fun unregister(context: Context) {
            if (_hasRegister.compareAndSet(true, false)) {
                context.unregisterReceiver(this)
            }
        }
    }

    companion object {
        @JvmStatic
        fun isNetworkAvailable(): Boolean {
            val manager = fContext.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
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