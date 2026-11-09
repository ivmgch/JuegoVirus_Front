package com.example.virus.data.repository

import android.content.Context
import com.example.virus.data.api.RetrofitClient
import com.example.virus.data.websocket.ViralWebSocketClient
import com.example.virus.model.domain.PlayerStatus
import com.example.virus.model.domain.SessionInfo
import com.example.virus.model.dto.BoardUpdateDto
import com.example.virus.model.dto.CreateSessionRequest
import com.example.virus.model.dto.ExposureReportPayload
import com.example.virus.model.dto.JoinResponse
import com.example.virus.model.dto.JoinSessionRequest
import com.example.virus.model.dto.SessionResponse
import com.example.virus.model.dto.StateChangeDto
import com.example.virus.util.DeviceIdHelper
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class GameRepositoryImpl(
    private val context: Context,
    private val webSocketClient: ViralWebSocketClient = ViralWebSocketClient()
) : GameRepository {

    private val api get() = RetrofitClient.getApi()

    private val _currentSession = MutableStateFlow<SessionInfo?>(null)
    override val currentSession: StateFlow<SessionInfo?> = _currentSession.asStateFlow()

    override val isConnected: StateFlow<Boolean> = webSocketClient.isConnected
    override val boardUpdates: SharedFlow<BoardUpdateDto> = webSocketClient.boardUpdates
    override val stateChanges: SharedFlow<StateChangeDto> = webSocketClient.stateChanges

    override suspend fun testServerConnection(): Result<Boolean> {
        return try {
            val response = api.checkHealth()
            if (response.isSuccessful && response.body()?.get("status") == "UP") {
                Result.success(true)
            } else {
                Result.failure(Exception("Servidor respondió con código: ${response.code()}"))
            }
        } catch (e: Exception) {
            Result.failure(Exception("No se pudo conectar a http://${RetrofitClient.serverHost}: ${e.localizedMessage ?: e.javaClass.simpleName}"))
        }
    }

    override suspend fun createSession(sessionCode: String, initialP0: Int): Result<SessionResponse> {
        return try {
            val response = api.createSession(CreateSessionRequest(sessionCode, initialP0))
            if (response.isSuccessful && response.body() != null) {
                Result.success(response.body()!!)
            } else {
                Result.failure(Exception("Error al crear sesión: ${response.code()} ${response.message()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun joinSession(sessionId: Long, fcmToken: String?): Result<JoinResponse> {
        return try {
            val deviceMacHash = DeviceIdHelper.getAnonymizedDeviceHash(context)
            val response = api.joinSession(sessionId, JoinSessionRequest(deviceMacHash, fcmToken))
            if (response.isSuccessful && response.body() != null) {
                val joinResp = response.body()!!
                val status = when (joinResp.assignedStatus) {
                    "PATIENT_ZERO" -> PlayerStatus.PATIENT_ZERO
                    "INFECTED" -> PlayerStatus.INFECTED
                    "IMMUNE" -> PlayerStatus.IMMUNE
                    else -> PlayerStatus.HEALTHY
                }

                _currentSession.value = SessionInfo(
                    sessionId = sessionId,
                    sessionCode = "SESIÓN #$sessionId",
                    playerUuid = joinResp.playerUuid,
                    status = status
                )

                // Conectar automáticamente al canal WebSocket de tiempo real
                connectRealtime(sessionId, joinResp.playerUuid)
                Result.success(joinResp)
            } else {
                Result.failure(Exception("Error al unirse: ${response.code()} ${response.message()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override fun connectRealtime(sessionId: Long, playerUuid: String) {
        webSocketClient.connect(sessionId, playerUuid)
    }

    override fun sendHeartbeat(bleNearbyCount: Int) {
        val session = _currentSession.value ?: return
        webSocketClient.sendHeartbeat(session.sessionId, session.playerUuid, bleNearbyCount)
    }

    override fun sendExposureReport(infectedBeaconUuid: String, durationSec: Int, distanceM: Double) {
        val session = _currentSession.value ?: return
        val report = ExposureReportPayload(
            sessionId = session.sessionId,
            healthyPlayerUuid = session.playerUuid,
            infectedBeaconUuid = infectedBeaconUuid,
            exposureDurationSeconds = durationSec,
            estimatedDistanceMeters = distanceM
        )
        webSocketClient.sendExposureReport(report)
    }

    override fun updatePlayerStatusLocally(newStatus: PlayerStatus) {
        val current = _currentSession.value ?: return
        _currentSession.value = current.copy(status = newStatus)
    }
}
