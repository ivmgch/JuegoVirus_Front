package com.example.virus.util

import android.content.Context
import android.provider.Settings
import java.security.MessageDigest

object DeviceIdHelper {

    fun getAnonymizedDeviceHash(context: Context): String {
        val rawId = Settings.Secure.getString(
            context.contentResolver,
            Settings.Secure.ANDROID_ID
        ) ?: "fallback_device_${System.currentTimeMillis()}"

        val digest = MessageDigest.getInstance("SHA-256")
        val hashBytes = digest.digest(rawId.toByteArray(Charsets.UTF_8))
        return hashBytes.joinToString("") { "%02x".format(it) }
    }
}
