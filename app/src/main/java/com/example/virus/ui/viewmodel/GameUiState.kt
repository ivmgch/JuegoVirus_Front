package com.example.virus.ui.viewmodel

import com.example.virus.data.api.RetrofitClient
import com.example.virus.model.domain.LogisticMetrics
import com.example.virus.model.domain.PlayerStatus

enum class AppScreen {
    WELCOME,
    LOBBY,
    GAME_DASHBOARD
}

data class GameUiState(
    val currentScreen: AppScreen = AppScreen.WELCOME,
    val sessionId: Long? = null,
    val sessionCode: String = "",
    val playerUuid: String? = null,
    val playerStatus: PlayerStatus = PlayerStatus.HEALTHY,
    val sessionStatus: String = "WAITING_FOR_PLAYERS",
    val totalPlayers: Long = 0,
    val minPlayersRequired: Int = 3,
    val canStartGame: Boolean = false,
    val metrics: LogisticMetrics = LogisticMetrics(),
    val curvePoints: List<Pair<Float, Float>> = emptyList(),
    val isConnected: Boolean = false,
    val isCrimsonGlitchActive: Boolean = false,
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val serverHost: String = RetrofitClient.serverHost,
    val isServerReachable: Boolean? = null,
    val latencyMs: Long? = null,
    val showServerConfigDialog: Boolean = false
)
