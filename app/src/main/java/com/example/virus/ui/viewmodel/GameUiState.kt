package com.example.virus.ui.viewmodel

import com.example.virus.data.api.RetrofitClient
import com.example.virus.model.domain.LogisticMetrics
import com.example.virus.model.domain.PlayerStatus

data class GameUiState(
    val sessionId: Long? = null,
    val sessionCode: String = "",
    val playerUuid: String? = null,
    val playerStatus: PlayerStatus = PlayerStatus.HEALTHY,
    val metrics: LogisticMetrics = LogisticMetrics(),
    val curvePoints: List<Pair<Float, Float>> = emptyList(),
    val isConnected: Boolean = false,
    val isCrimsonGlitchActive: Boolean = false,
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val serverHost: String = RetrofitClient.serverHost,
    val isServerReachable: Boolean? = null // null: comprobando, true: OK, false: inalcanzable
)
