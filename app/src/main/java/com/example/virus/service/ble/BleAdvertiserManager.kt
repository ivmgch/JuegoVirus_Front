package com.example.virus.service.ble

import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.le.AdvertiseCallback
import android.bluetooth.le.AdvertiseData
import android.bluetooth.le.AdvertiseSettings
import android.bluetooth.le.BluetoothLeAdvertiser
import android.os.ParcelUuid
import android.util.Log
import java.nio.ByteBuffer
import java.nio.charset.StandardCharsets
import java.util.UUID

class BleAdvertiserManager(
    private val bluetoothAdapter: BluetoothAdapter?
) {
    private val tag = "BleAdvertiser"
    private var advertiser: BluetoothLeAdvertiser? = null
    private var advertiseCallback: AdvertiseCallback? = null

    companion object {
        // UUID de servicio reservado para la simulación viral
        val VIRAL_SERVICE_UUID: UUID = UUID.fromString("0000FEAA-0000-1000-8000-00805F9B34FB")
    }

    @SuppressLint("MissingPermission")
    fun startAdvertising(playerUuid: String) {
        if (bluetoothAdapter == null || !bluetoothAdapter.isEnabled) {
            Log.w(tag, "Bluetooth no disponible o desactivado para emitir.")
            return
        }

        // Si ya había una emisión previa, detenerla limpiamente
        stopAdvertising()

        advertiser = bluetoothAdapter.bluetoothLeAdvertiser
        if (advertiser == null) {
            Log.e(tag, "El hardware de este dispositivo no soporta BLE Advertising.")
            return
        }

        val settings = AdvertiseSettings.Builder()
            .setAdvertiseMode(AdvertiseSettings.ADVERTISE_MODE_LOW_LATENCY)
            .setTxPowerLevel(AdvertiseSettings.ADVERTISE_TX_POWER_HIGH)
            .setConnectable(false)
            .setTimeout(0)
            .build()

        // Serializar el UUID de 128 bits en exactamente 16 bytes crudos para respetar el límite estricto de 31 bytes BLE
        val payloadBytes = try {
            val uuid = UUID.fromString(playerUuid)
            val bb = ByteBuffer.allocate(16)
            bb.putLong(uuid.mostSignificantBits)
            bb.putLong(uuid.leastSignificantBits)
            bb.array()
        } catch (e: Exception) {
            playerUuid.take(16).toByteArray(StandardCharsets.UTF_8)
        }

        // NO incluir TxPowerLevel para garantizar que el paquete no sobrepase los 31 bytes máximos
        val data = AdvertiseData.Builder()
            .setIncludeDeviceName(false)
            .setIncludeTxPowerLevel(false)
            .addServiceUuid(ParcelUuid(VIRAL_SERVICE_UUID))
            .addServiceData(ParcelUuid(VIRAL_SERVICE_UUID), payloadBytes)
            .build()

        advertiseCallback = object : AdvertiseCallback() {
            override fun onStartSuccess(settingsInEffect: AdvertiseSettings?) {
                Log.i(tag, "Baliza BLE emitida exitosamente. UUID emitido: $playerUuid (payload: ${payloadBytes.size} bytes)")
            }

            override fun onStartFailure(errorCode: Int) {
                val reason = when (errorCode) {
                    AdvertiseCallback.ADVERTISE_FAILED_DATA_TOO_LARGE -> "DATA_TOO_LARGE (excede 31 bytes)"
                    AdvertiseCallback.ADVERTISE_FAILED_TOO_MANY_ADVERTISERS -> "TOO_MANY_ADVERTISERS"
                    AdvertiseCallback.ADVERTISE_FAILED_ALREADY_STARTED -> "ALREADY_STARTED"
                    AdvertiseCallback.ADVERTISE_FAILED_INTERNAL_ERROR -> "INTERNAL_ERROR"
                    AdvertiseCallback.ADVERTISE_FAILED_FEATURE_UNSUPPORTED -> "FEATURE_UNSUPPORTED"
                    else -> "Error código $errorCode"
                }
                Log.e(tag, "Fallo al iniciar BLE Advertising: $reason")
            }
        }

        try {
            advertiser?.startAdvertising(settings, data, advertiseCallback)
        } catch (e: Exception) {
            Log.e(tag, "Excepción al iniciar Advertising: ${e.message}")
        }
    }

    @SuppressLint("MissingPermission")
    fun stopAdvertising() {
        try {
            advertiseCallback?.let { advertiser?.stopAdvertising(it) }
            advertiser = null
            advertiseCallback = null
            Log.i(tag, "Emisión BLE detenida.")
        } catch (e: Exception) {
            Log.e(tag, "Error al detener Advertising: ${e.message}")
        }
    }
}
