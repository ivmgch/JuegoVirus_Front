package com.example.virus.service.fcm

import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.media.RingtoneManager
import android.util.Log
import androidx.core.app.NotificationCompat
import com.example.virus.MainActivity
import com.example.virus.ViralApplication
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage

class ViralFirebaseMessagingService : FirebaseMessagingService() {

    private val tag = "ViralFCM"

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        Log.i(tag, "Nuevo token FCM generado: $token")
        // Podría sincronizarse con el backend si hay una sesión activa
    }

    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        super.onMessageReceived(remoteMessage)
        Log.w(tag, "Mensaje FCM recibido de: ${remoteMessage.from}")

        val data = remoteMessage.data
        val notification = remoteMessage.notification

        val title = notification?.title ?: data["title"] ?: "⚠️ ¡ALERTA CRÍTICA: PICO DE PROPAGACIÓN!"
        val body = notification?.body ?: data["body"] ?: "El virus superó el punto de inflexión poblacional (k/2)."

        showHighPriorityAlert(title, body)
    }

    private fun showHighPriorityAlert(title: String, message: String) {
        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }

        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val alarmSound = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
        val vibrationPattern = longArrayOf(0, 500, 200, 500, 200, 500)

        val notificationBuilder = NotificationCompat.Builder(this, ViralApplication.CRITICAL_CHANNEL_ID)
            .setSmallIcon(android.R.drawable.stat_sys_warning)
            .setContentTitle(title)
            .setContentText(message)
            .setStyle(NotificationCompat.BigTextStyle().bigText(message))
            .setAutoCancel(true)
            .setSound(alarmSound)
            .setVibrate(vibrationPattern)
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setContentIntent(pendingIntent)

        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(2001, notificationBuilder.build())
    }
}
