package com.example.virus.model.domain

enum class PlayerStatus {
    HEALTHY,
    PATIENT_ZERO,
    INFECTED,
    IMMUNE
}

data class SessionInfo(
    val sessionId: Long,
    val sessionCode: String,
    val playerUuid: String,
    val status: PlayerStatus,
    val sessionStatus: String = "WAITING_FOR_PLAYERS",
    val totalPlayers: Long = 0,
    val minPlayersRequired: Int = 3,
    val canStart: Boolean = false
)

data class LogisticMetrics(
    val carryingCapacityK: Double = 0.0,
    val integrationConstantA: Double = 0.0,
    val growthRateR: Double = 0.04,
    val totalInfected: Long = 0,
    val totalHealthy: Long = 0,
    val theoreticalInfectedPt: Double = 0.0,
    val timestampMs: Long = System.currentTimeMillis()
)
