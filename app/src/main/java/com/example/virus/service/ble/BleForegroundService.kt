package com.example.virus.service.ble

import android.app.Notification
import android.app.PendingIntent
import android.app.Service
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
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
    private var currentPlayerUuid: String? = null

    private val bluetoothStateReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent?.action == BluetoothAdapter.ACTION_STATE_CHANGED) {
                val state = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, BluetoothAdapter.ERROR)
                when (state) {
                    BluetoothAdapter.STATE_ON -> {
                        Log.i(tag, "Bluetooth activado en el sistema. Inicializando operaciones BLE.")
                        val uuid = currentPlayerUuid
                        if (uuid != null) {
                            startBleOperations(uuid)
                        }
                    }
                    BluetoothAdapter.STATE_TURNING_OFF, BluetoothAdapter.STATE_OFF -> {
                        Log.w(tag, "Bluetooth desactivado en el sistema. Pausando operaciones BLE.")
                        stopBleOperations()
                    }
                }
            }
        }
    }

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
            // Descartar balizas emitidas por el propio dispositivo
            val myUuid = currentPlayerUuid
            if (myUuid == null || !beaconUuid.equals(myUuid, ignoreCase = true)) {
                gameRepository.sendExposureReport(beaconUuid, durationSec, distanceM)
            }
        }

        try {
            registerReceiver(bluetoothStateReceiver, IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED))
        } catch (e: Exception) {
            Log.e(tag, "Error al registrar bluetoothStateReceiver: ${e.message}")
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START_TRACKING -> {
                val playerUuid = intent.getStringExtra(EXTRA_PLAYER_UUID) ?: "anonymous-uuid"
                currentPlayerUuid = playerUuid
                startForegroundWithNotification()
                startBleOperations(playerUuid)
                startPeriodicHeartbeat()
            }
            ACTION_STOP_TRACKING -> {
                stopBleOperations()
                ServiceCompat.stopForeground(this, ServiceCompat.STOP_FOREGROUND_REMOVE)
                stopSelf()
            }
        }
        return START_NOT_STICKY
    }

    override fun onTaskRemoved(rootIntent: Intent?) {
        super.onTaskRemoved(rootIntent)
        Log.d(tag, "App cerrada desde la lista de recientes (onTaskRemoved). Deteniendo servicio BLE.")
        stopBleOperations()
        ServiceCompat.stopForeground(this, ServiceCompat.STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    private fun startForegroundWithNotification() {
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val stopIntent = Intent(this, BleForegroundService::class.java).apply {
            action = ACTION_STOP_TRACKING
        }
        val stopPendingIntent = PendingIntent.getService(
            this,
            1,
            stopIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val notification: Notification = NotificationCompat.Builder(this, ViralApplication.BLE_CHANNEL_ID)
            .setContentTitle("Simulación Viral Activa 🦠")
            .setContentText("Rastreando proximidad Bluetooth y emitiendo telemetría...")
            .setSmallIcon(android.R.drawable.stat_notify_sync)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .addAction(android.R.drawable.ic_menu_close_clear_cancel, "Detener", stopPendingIntent)
            .build()

        val foregroundType = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            ServiceInfo.FOREGROUND_SERVICE_TYPE_CONNECTED_DEVICE
        } else {
            0
        }

        ServiceCompat.startForeground(this, NOTIFICATION_ID, notification, foregroundType)
    }

    private fun startBleOperations(playerUuid: String) {
        val bluetoothManager = getSystemService(Context.BLUETOOTH_SERVICE) as? BluetoothManager
        val adapter = bluetoothManager?.adapter
        if (adapter != null && adapter.isEnabled) {
            if (advertiserManager == null) {
                advertiserManager = BleAdvertiserManager(adapter)
            }
            if (scannerManager == null) {
                scannerManager = BleScannerManager(adapter) { beaconUuid, durationSec, distanceM ->
                    val myUuid = currentPlayerUuid
                    if (myUuid == null || !beaconUuid.equals(myUuid, ignoreCase = true)) {
                        gameRepository.sendExposureReport(beaconUuid, durationSec, distanceM)
                    }
                }
            }
            advertiserManager?.startAdvertising(playerUuid)
            scannerManager?.startScanning()
        } else {
            Log.w(tag, "startBleOperations: Bluetooth no disponible o apagado.")
        }
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
        try {
            unregisterReceiver(bluetoothStateReceiver)
        } catch (_: Exception) {}
        stopBleOperations()
        ServiceCompat.stopForeground(this, ServiceCompat.STOP_FOREGROUND_REMOVE)
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
