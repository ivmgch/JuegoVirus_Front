package com.example.virus.data.api

import android.content.Context
import android.content.SharedPreferences

object ServerConfigManager {

    private const val PREFS_NAME = "viral_server_prefs"
    private const val KEY_SERVER_ADDRESS = "key_server_address"
    const val DEFAULT_ADDRESS = "192.168.1.18:8080"

    private var prefs: SharedPreferences? = null

    fun init(context: Context) {
        if (prefs == null) {
            prefs = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        }
    }

    var serverAddress: String
        get() = prefs?.getString(KEY_SERVER_ADDRESS, DEFAULT_ADDRESS) ?: DEFAULT_ADDRESS
        set(value) {
            val sanitized = sanitize(value)
            prefs?.edit()?.putString(KEY_SERVER_ADDRESS, sanitized)?.apply()
            RetrofitClient.onServerAddressChanged(sanitized)
        }

    fun sanitize(input: String): String {
        var clean = input.trim()
        if (clean.isBlank()) return DEFAULT_ADDRESS

        val isHttps = clean.startsWith("https://", ignoreCase = true)
        clean = clean.removePrefix("http://").removePrefix("https://").removePrefix("HTTP://").removePrefix("HTTPS://")

        while (clean.endsWith("/")) {
            clean = clean.dropLast(1).trim()
        }

        // Si es una IP o host simple sin puerto, colocar :8080 por defecto
        if (!clean.contains(":") && !clean.contains(".com") && !clean.contains(".io") && !clean.contains(".app") && !clean.contains(".org") && !clean.contains(".net")) {
            clean = "$clean:8080"
        }

        return if (isHttps) "https://$clean" else clean
    }

    fun getHttpBaseUrl(): String {
        val addr = serverAddress
        return if (addr.startsWith("https://", ignoreCase = true) || addr.startsWith("http://", ignoreCase = true)) {
            if (addr.endsWith("/")) addr else "$addr/"
        } else {
            "http://$addr/"
        }
    }

    fun getWebSocketUrl(): String {
        val addr = serverAddress
        return if (addr.startsWith("https://", ignoreCase = true)) {
            val hostPart = addr.removePrefix("https://").removeSuffix("/")
            "wss://$hostPart/ws-viral-native"
        } else {
            val hostPart = addr.removePrefix("http://").removeSuffix("/")
            "ws://$hostPart/ws-viral-native"
        }
    }
}
