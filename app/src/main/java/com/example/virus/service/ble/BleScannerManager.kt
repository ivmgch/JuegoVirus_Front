package com.example.virus.service.ble

import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.le.BluetoothLeScanner
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanFilter
import android.bluetooth.le.ScanResult
import android.bluetooth.le.ScanSettings
import android.os.ParcelUuid
import android.util.Log
import java.nio.ByteBuffer
import java.nio.charset.StandardCharsets
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import kotlin.math.pow

class BleScannerManager(
    private val bluetoothAdapter: BluetoothAdapter?,
    private val onProlongedExposure: (beaconUuid: String, durationSec: Int, distanceM: Double) -> Unit
) {
    private val tag = "BleScanner"
    private var scanner: BluetoothLeScanner? = null
    private var scanCallback: ScanCallback? = null

    // Registra la primera vez que un beacon estuvo a <= 2m continuos: <BeaconUuid, FirstSeenEpochMs>
    private val exposureStartMap = ConcurrentHashMap<String, Long>()
    // Registra la última vez visto para limpiar contactos desconectados: <BeaconUuid, LastSeenEpochMs>
    private val lastSeenMap = ConcurrentHashMap<String, Long>()
    // Registra el último reporte enviado para evitar spam pero permitir reintento si es necesario: <BeaconUuid, LastReportEpochMs>
    private val alreadyReportedBeacons = ConcurrentHashMap<String, Long>()

    @SuppressLint("MissingPermission")
    fun startScanning() {
        if (bluetoothAdapter == null || !bluetoothAdapter.isEnabled) {
            Log.w(tag, "Bluetooth no disponible o desactivado para escanear.")
            return
        }

        scanner = bluetoothAdapter.bluetoothLeScanner
        if (scanner == null) {
            Log.e(tag, "BluetoothLeScanner no disponible.")
            return
        }

        val filter = ScanFilter.Builder()
            .setServiceUuid(ParcelUuid(BleAdvertiserManager.VIRAL_SERVICE_UUID))
            .build()

        val settings = ScanSettings.Builder()
            .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
            .setReportDelay(0)
            .build()

        scanCallback = object : ScanCallback() {
            override fun onScanResult(callbackType: Int, result: ScanResult?) {
                result?.let { processScanResult(it) }
            }

            override fun onBatchScanResults(results: MutableList<ScanResult>?) {
                results?.forEach { processScanResult(it) }
            }

            override fun onScanFailed(errorCode: Int) {
                Log.e(tag, "Fallo al iniciar BLE Scanning. Error: $errorCode")
            }
        }

        try {
            scanner?.startScan(listOf(filter), settings, scanCallback)
            Log.i(tag, "Escáner BLE activado en modo LOW_LATENCY con filtro de UUID viral.")
        } catch (e: Exception) {
            Log.e(tag, "Excepción al iniciar scanner BLE: ${e.message}")
        }
    }

    private fun processScanResult(result: ScanResult) {
        val scanRecord = result.scanRecord ?: return
        val serviceData = scanRecord.getServiceData(ParcelUuid(BleAdvertiserManager.VIRAL_SERVICE_UUID))

        // Decodificar UUID de 16 bytes crudos o formato de texto
        val beaconUuid = if (serviceData != null && serviceData.size == 16) {
            try {
                val bb = ByteBuffer.wrap(serviceData)
                UUID(bb.long, bb.long).toString()
            } catch (e: Exception) {
                String(serviceData, StandardCharsets.UTF_8).trim()
            }
        } else if (serviceData != null) {
            String(serviceData, StandardCharsets.UTF_8).trim()
        } else {
            result.device.address
        }

        val rssi = result.rssi
        val distanceMeters = estimateDistanceMeters(rssi)
        val now = System.currentTimeMillis()

        lastSeenMap[beaconUuid] = now

        // Regla RN04: Distancia equivalente a <= 2.0 metros (o intensidad de señal cercana RSSI >= -75 dBm)
        val isCloseProximity = distanceMeters <= 2.0 || rssi >= -75

        if (isCloseProximity) {
            val startTime = exposureStartMap.putIfAbsent(beaconUuid, now) ?: now
            val elapsedMs = now - startTime
            val durationSec = (elapsedMs / 1000).toInt()

            // Regla RN04: Exposición continua >= 15 segundos
            if (durationSec >= 15) {
                val lastReportTime = alreadyReportedBeacons[beaconUuid] ?: 0L
                // Espaciar reportes 10s si el backend aún no ha procesado el contagio
                if (now - lastReportTime > 10_000L) {
                    alreadyReportedBeacons[beaconUuid] = now
                    val reportedDistance = minOf(2.0, distanceMeters)
                    Log.w(tag, "⚠️ UMBRAL RN04 CUMPLIDO: Exposición >= 15s a beacon $beaconUuid (distancia: ${String.format(java.util.Locale.US, "%.2f", reportedDistance)}m, RSSI: $rssi dBm)")
                    onProlongedExposure(beaconUuid, durationSec, reportedDistance)
                }
            }
        } else if (distanceMeters > 3.0 && rssi < -82) {
            // El dispositivo se alejó claramente: reiniciar ventana de exposición
            exposureStartMap.remove(beaconUuid)
        }

        // Limpiar dispositivos que no se ven hace más de 10 segundos
        val staleThreshold = now - 10_000
        lastSeenMap.entries.removeIf { (id, timestamp) ->
            if (timestamp < staleThreshold) {
                exposureStartMap.remove(id)
                true
            } else false
        }
    }

    /**
     * Modelo de pérdida de propagación log-normal para estimación de distancia por RSSI.
     * En radiofrecuencia BLE a 2.4 GHz en interiores, la potencia de referencia recibida a 1 metro
     * (RSSI a 1 metro) oscila entre -59 dBm y -65 dBm (calibrado estándar en -62 dBm).
     */
    private fun estimateDistanceMeters(rssi: Int): Double {
        val rssiAtOneMeter = -62.0 // Potencia de referencia recibida a 1 metro
        val pathLossExponent = 2.2 // Coeficiente de atenuación en interiores (campus)
        val ratio = (rssiAtOneMeter - rssi) / (10.0 * pathLossExponent)
        return 10.0.pow(ratio)
    }

    fun getActiveNearbyDevicesCount(): Int {
        val now = System.currentTimeMillis()
        val recentThreshold = now - 30_000
        return lastSeenMap.values.count { it >= recentThreshold }
    }

    @SuppressLint("MissingPermission")
    fun stopScanning() {
        try {
            scanCallback?.let { scanner?.stopScan(it) }
            scanner = null
            scanCallback = null
            exposureStartMap.clear()
            lastSeenMap.clear()
            Log.i(tag, "Escáner BLE detenido.")
        } catch (e: Exception) {
            Log.e(tag, "Error al detener scanner BLE: ${e.message}")
        }
    }
}
