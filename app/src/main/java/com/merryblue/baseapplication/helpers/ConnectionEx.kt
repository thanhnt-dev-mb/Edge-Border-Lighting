package com.merryblue.baseapplication.helpers

import android.content.Context
import android.content.Intent
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.provider.Settings

fun Context.openProperNetworkSettings() {
    when (getNetworkState()) {

        NetworkState.WIFI_NO_INTERNET -> {
            // Wifi connected nhưng không có internet → ưu tiên 4G
            startActivity(Intent(Settings.ACTION_DATA_ROAMING_SETTINGS))
        }

        NetworkState.CELLULAR_NO_INTERNET -> {
            // 4G bật nhưng không có internet
            startActivity(Intent(Settings.ACTION_DATA_ROAMING_SETTINGS))
        }

        NetworkState.NO_NETWORK -> {
            // Cả wifi & 4G đều tắt
            startActivity(Intent(Settings.ACTION_WIFI_SETTINGS))
        }

        else -> {
            // fallback
            startActivity(Intent(Settings.ACTION_WIRELESS_SETTINGS))
        }
    }
}


fun Context.getNetworkState(): NetworkState {
    val cm = getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
    val network = cm.activeNetwork ?: return NetworkState.NO_NETWORK
    val caps = cm.getNetworkCapabilities(network) ?: return NetworkState.NO_NETWORK

    val hasInternet =
        caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) &&
                caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)

    val isWifi = caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)
    val isCellular = caps.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR)

    return when {
        isWifi && !hasInternet -> NetworkState.WIFI_NO_INTERNET
        isWifi && hasInternet -> NetworkState.WIFI_OK
        isCellular && !hasInternet -> NetworkState.CELLULAR_NO_INTERNET
        isCellular && hasInternet -> NetworkState.CELLULAR_OK
        else -> NetworkState.NO_NETWORK
    }
}

enum class NetworkState {
    WIFI_OK,
    WIFI_NO_INTERNET,
    CELLULAR_OK,
    CELLULAR_NO_INTERNET,
    NO_NETWORK
}
