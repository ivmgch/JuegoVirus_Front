package com.example.virus.service.ble

import android.app.Notification
import android.app.PendingIntent
import android.app.Service
import android.bluetooth.BluetoothManager
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat
import com.example.virus.MainActivity
import com.example.virus.ViralApplication
import com.example.virus.data.repository.GameRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

class BleForegroundService : Service() {

    private val tag = "BleForegroundService"
    private val serviceScope = CoroutineScope(Dispatchers.IO + Job())
    private var heartbeatJob: Job? = null

    private lateinit var gameRepository: GameRepository
    private var advertiserManager: BleAdvertiserManager? = null
    private var scannerManager: BleScannerManager? = null

    companion object {
        const val ACTION_START_TRACKING = "com.example.virus.START_TRACKING"
        const val ACTION_STOP_TRACKING = "com.example.virus.STOP_TRACKING"
        const val EXTRA_PLAYER_UUID = "extra_player_uuid"
        private const val NOTIFICATION_ID = 1001

        fun start(context: Context, playerUuid: String) {
            val intent = Intent(context, BleForegroundService::class.java).apply {
                action = ACTION_START_TRACKING
                putExtra(EXTRA_PLAYER_UUID, playerUuid)
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }

        fun stop(context: Context) {
            val intent = Intent(context, BleForegroundService::class.java).apply {
                action = ACTION_STOP_TRACKING
            }
            context.startService(intent)
        }
    }

    override fun onCreate() {
        super.onCreate()
        gameRepository = (application as ViralApplication).gameRepository

        val bluetoothManager = getSystemService(Context.BLUETOOTH_SERVICE) as? BluetoothManager
        val adapter = bluetoothManager?.adapter

        advertiserManager = BleAdvertiserManager(adapter)
        scannerManager = BleScannerManager(adapter) { beaconUuid, durationSec, distanceM ->
            // Disparar EXPOSURE_REPORT hacia el backend por WebSocket
            gameRepository.sendExposureReport(beaconUuid, durationSec, distanceM)
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START_TRACKING -> {
                val playerUuid = intent.getStringExtra(EXTRA_PLAYER_UUID) ?: "anonymous-uuid"
                startForegroundWithNotification()
                startBleOperations(playerUuid)
                startPeriodicHeartbeat()
            }
            ACTION_STOP_TRACKING -> {
                stopBleOperations()
                stopSelf()
            }
        }
        return START_STICKY
    }

    private fun startForegroundWithNotification() {
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val notification: Notification = NotificationCompat.Builder(this, ViralApplication.BLE_CHANNEL_ID)
            .setContentTitle("Simulación Viral Activa 🦠")
            .setContentText("Rastreando proximidad Bluetooth y emitiendo telemetría...")
            .setSmallIcon(android.R.drawable.stat_notify_sync)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()

        val foregroundType = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            ServiceInfo.FOREGROUND_SERVICE_TYPE_CONNECTED_DEVICE
        } else {
            0
        }

        ServiceCompat.startForeground(this, NOTIFICATION_ID, notification, foregroundType)
    }

    private fun startBleOperations(playerUuid: String) {
        advertiserManager?.startAdvertising(playerUuid)
        scannerManager?.startScanning()
    }

    private fun stopBleOperations() {
        heartbeatJob?.cancel()
        advertiserManager?.stopAdvertising()
        scannerManager?.stopScanning()
    }

    /**
     * Tarea periódica de envío de TELEMETRY_HEARTBEAT cada 30 segundos
     */
    private fun startPeriodicHeartbeat() {
        heartbeatJob?.cancel()
        heartbeatJob = serviceScope.launch {
            while (isActive) {
                val nearbyCount = scannerManager?.getActiveNearbyDevicesCount() ?: 0
                Log.d(tag, "Enviando TELEMETRY_HEARTBEAT (dispositivos BLE detectados: $nearbyCount)")
                gameRepository.sendHeartbeat(nearbyCount)
                delay(30_000L) // 30 segundos
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        stopBleOperations()
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
