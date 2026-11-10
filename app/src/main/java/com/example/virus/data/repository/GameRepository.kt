package com.example.virus.data.repository

import com.example.virus.model.domain.SessionInfo
import com.example.virus.model.dto.BoardUpdateDto
import com.example.virus.model.dto.JoinResponse
import com.example.virus.model.dto.SessionResponse
import com.example.virus.model.dto.StateChangeDto
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow

interface GameRepository {
    val isConnected: StateFlow<Boolean>
    val currentSession: StateFlow<SessionInfo?>
    val boardUpdates: SharedFlow<BoardUpdateDto>
    val stateChanges: SharedFlow<StateChangeDto>

    suspend fun createSession(sessionCode: String, initialP0: Int): Result<SessionResponse>
    suspend fun joinSession(sessionId: Long, fcmToken: String? = null): Result<JoinResponse>
    suspend fun startSession(sessionId: Long): Result<SessionResponse>
    suspend fun testServerConnection(): Result<Boolean>
    fun connectRealtime(sessionId: Long, playerUuid: String)
    fun sendHeartbeat(bleNearbyCount: Int)
    fun sendExposureReport(infectedBeaconUuid: String, durationSec: Int, distanceM: Double)
    fun updatePlayerStatusLocally(newStatus: com.example.virus.model.domain.PlayerStatus)
}
