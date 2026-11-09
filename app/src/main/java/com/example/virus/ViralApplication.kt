package com.example.virus

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build
import com.example.virus.data.repository.GameRepository
import com.example.virus.data.repository.GameRepositoryImpl
import com.example.virus.util.HapticFeedbackManager

class ViralApplication : Application() {

    lateinit var gameRepository: GameRepository
        private set

    lateinit var hapticFeedbackManager: HapticFeedbackManager
        private set

    override fun onCreate() {
        super.onCreate()
        createNotificationChannels()

        hapticFeedbackManager = HapticFeedbackManager(this)
        gameRepository = GameRepositoryImpl(this)
    }

    private fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notificationManager = getSystemService(NotificationManager::class.java)

            // Canal para el Foreground Service BLE
            val bleChannel = NotificationChannel(
                BLE_CHANNEL_ID,
                "Servicio de Rastreo BLE",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Monitorea la proximidad física pasiva de otros jugadores"
            }

            // Canal para Alertas Críticas FCM de Inflexión (k/2)
            val criticalChannel = NotificationChannel(
                CRITICAL_CHANNEL_ID,
                "Alertas Críticas de Propagación",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Notificaciones de emergencia cuando el contagio alcanza velocidad máxima"
                enableVibration(true)
                enableLights(true)
            }

            notificationManager.createNotificationChannel(bleChannel)
            notificationManager.createNotificationChannel(criticalChannel)
        }
    }

    companion object {
        const val BLE_CHANNEL_ID = "viral_ble_tracking_channel"
        const val CRITICAL_CHANNEL_ID = "viral_critical_alert_channel"
    }
}
