package com.example.virus.model.dto

import com.google.gson.annotations.SerializedName

// --- DTOs REST ---
data class CreateSessionRequest(
    @SerializedName("sessionCode") val sessionCode: String,
    @SerializedName("initialP0") val initialP0: Int
)

data class JoinSessionRequest(
    @SerializedName("deviceMacHash") val deviceMacHash: String,
    @SerializedName("fcmToken") val fcmToken: String?
)

data class SessionResponse(
    @SerializedName("sessionId") val sessionId: Long,
    @SerializedName("sessionCode") val sessionCode: String,
    @SerializedName("status") val status: String,
    @SerializedName("p0") val p0: Int,
    @SerializedName("initialK") val initialK: Double,
    @SerializedName("initialA") val initialA: Double,
    @SerializedName("totalPlayers") val totalPlayers: Long = 0,
    @SerializedName("minPlayersRequired") val minPlayersRequired: Int = 3,
    @SerializedName("canStart") val canStart: Boolean = false,
    @SerializedName("playerUuids") val playerUuids: List<String> = emptyList()
)

data class JoinResponse(
    @SerializedName("playerUuid") val playerUuid: String,
    @SerializedName("sessionId") val sessionId: Long,
    @SerializedName("assignedStatus") val assignedStatus: String,
    @SerializedName("joinedAt") val joinedAt: String
)

// --- DTOs WebSockets ---
data class BoardUpdateDto(
    @SerializedName("sessionId") val sessionId: Long,
    @SerializedName("k") val k: Double,
    @SerializedName("a") val a: Double,
    @SerializedName("r") val r: Double,
    @SerializedName("totalInfected") val totalInfected: Long,
    @SerializedName("totalHealthy") val totalHealthy: Long,
    @SerializedName("theoreticalInfectedPt") val theoreticalInfectedPt: Double,
    @SerializedName("timestampMs") val timestampMs: Long,
    @SerializedName("sessionStatus") val sessionStatus: String? = null,
    @SerializedName("totalPlayers") val totalPlayers: Long = 0,
    @SerializedName("minPlayersRequired") val minPlayersRequired: Int = 3,
    @SerializedName("canStart") val canStart: Boolean = false
)

data class StateChangeDto(
    @SerializedName("playerUuid") val playerUuid: String,
    @SerializedName("newStatus") val newStatus: String,
    @SerializedName("infectedByBeaconUuid") val infectedByBeaconUuid: String?,
    @SerializedName("hapticFeedbackPattern") val hapticFeedbackPattern: String?,
    @SerializedName("timestamp") val timestamp: String
)

data class HeartbeatPayload(
    @SerializedName("sessionId") val sessionId: Long,
    @SerializedName("playerUuid") val playerUuid: String,
    @SerializedName("bleNearbyCount") val bleNearbyCount: Int
)

data class ExposureReportPayload(
    @SerializedName("sessionId") val sessionId: Long,
    @SerializedName("healthyPlayerUuid") val healthyPlayerUuid: String,
    @SerializedName("infectedBeaconUuid") val infectedBeaconUuid: String,
    @SerializedName("exposureDurationSeconds") val exposureDurationSeconds: Int,
    @SerializedName("estimatedDistanceMeters") val estimatedDistanceMeters: Double
)
