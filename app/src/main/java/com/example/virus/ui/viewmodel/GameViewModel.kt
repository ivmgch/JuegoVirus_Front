package com.example.virus.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.virus.data.repository.GameRepository
import com.example.virus.model.domain.LogisticMetrics
import com.example.virus.model.domain.PlayerStatus
import com.example.virus.model.dto.BoardUpdateDto
import com.example.virus.model.dto.StateChangeDto
import com.example.virus.util.HapticFeedbackManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlin.math.exp

class GameViewModel(
    private val repository: GameRepository,
    private val hapticFeedbackManager: HapticFeedbackManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(GameUiState())
    val uiState: StateFlow<GameUiState> = _uiState.asStateFlow()

    init {
        observeRepositoryFlows()
        testServerConnection()
    }

    fun testServerConnection() {
        viewModelScope.launch {
            _uiState.update { it.copy(isServerReachable = null) }
            val result = repository.testServerConnection()
            result.onSuccess {
                _uiState.update { it.copy(isServerReachable = true, errorMessage = null) }
            }.onFailure { error ->
                _uiState.update { it.copy(isServerReachable = false, errorMessage = error.localizedMessage) }
            }
        }
    }

    fun updateServerHost(newHost: String) {
        val sanitized = newHost.trim().removePrefix("http://").removePrefix("https://").removeSuffix("/")
        com.example.virus.data.api.RetrofitClient.serverHost = sanitized
        _uiState.update { it.copy(serverHost = sanitized) }
        testServerConnection()
    }

    fun clearError() {
        _uiState.update { it.copy(errorMessage = null) }
    }

    private fun observeRepositoryFlows() {
        // 1. Observar estado de conexión del WebSocket
        viewModelScope.launch {
            repository.isConnected.collect { connected ->
                _uiState.update { it.copy(isConnected = connected) }
            }
        }

        // 2. Observar BOARD_UPDATE periódico (cada 5s) para el Tablero y Vico
        viewModelScope.launch {
            repository.boardUpdates.collect { update ->
                handleBoardUpdate(update)
            }
        }

        // 3. Observar STATE_CHANGE Unicast (Infección confirmada por el servidor)
        viewModelScope.launch {
            repository.stateChanges.collect { stateChange ->
                handleStateChange(stateChange)
            }
        }

        // 4. Observar datos de sesión activa
        viewModelScope.launch {
            repository.currentSession.collect { session ->
                if (session != null) {
                    _uiState.update {
                        it.copy(
                            sessionId = session.sessionId,
                            sessionCode = session.sessionCode,
                            playerUuid = session.playerUuid,
                            playerStatus = session.status
                        )
                    }
                }
            }
        }
    }

    private fun handleBoardUpdate(update: BoardUpdateDto) {
        val metrics = LogisticMetrics(
            carryingCapacityK = update.k,
            integrationConstantA = update.a,
            growthRateR = update.r,
            totalInfected = update.totalInfected,
            totalHealthy = update.totalHealthy,
            theoreticalInfectedPt = update.theoreticalInfectedPt,
            timestampMs = update.timestampMs
        )

        // Generar puntos continuos para la gráfica Vico: P(t) = k / (A * e^(-r*t) + 1)
        val points = mutableListOf<Pair<Float, Float>>()
        val totalSteps = 25
        val maxT = 120f // Ventana de proyección temporal (segundos)
        val stepT = maxT / totalSteps

        for (i in 0..totalSteps) {
            val t = i * stepT
            val denominator = (update.a * exp(-update.r * t.toDouble())) + 1.0
            val pt = (update.k / denominator).toFloat()
            points.add(Pair(t, pt))
        }

        _uiState.update {
            it.copy(
                metrics = metrics,
                curvePoints = points
            )
        }
    }

    private fun handleStateChange(stateChange: StateChangeDto) {
        if (stateChange.newStatus == "INFECTED") {
            _uiState.update {
                it.copy(
                    playerStatus = PlayerStatus.INFECTED,
                    isCrimsonGlitchActive = true
                )
            }

            // Disparar feedback háptico simulando taquicardia
            hapticFeedbackManager.triggerTachycardiaVibration()

            // Actualizar persistencia en el repositorio local
            repository.updatePlayerStatusLocally(PlayerStatus.INFECTED)
        }
    }

    fun joinGameSession(sessionId: Long, fcmToken: String? = null) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            val result = repository.joinSession(sessionId, fcmToken)
            result.onSuccess {
                _uiState.update { state -> state.copy(isLoading = false) }
            }.onFailure { error ->
                _uiState.update { state ->
                    state.copy(isLoading = false, errorMessage = error.localizedMessage)
                }
            }
        }
    }

    fun createGameSession(code: String, initialP0: Int) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            val result = repository.createSession(code, initialP0)
            result.onSuccess { session ->
                joinGameSession(session.sessionId)
            }.onFailure { error ->
                _uiState.update { state ->
                    state.copy(isLoading = false, errorMessage = error.localizedMessage)
                }
            }
        }
    }

    fun dismissCrimsonGlitch() {
        _uiState.update { it.copy(isCrimsonGlitchActive = false) }
    }
}
