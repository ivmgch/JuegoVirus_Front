package com.example.virus.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.virus.data.api.ServerConfigManager
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

    fun navigateTo(screen: AppScreen) {
        _uiState.update { it.copy(currentScreen = screen) }
    }

    fun toggleServerConfigDialog(show: Boolean) {
        _uiState.update { it.copy(showServerConfigDialog = show) }
    }

    fun testServerConnection() {
        viewModelScope.launch {
            _uiState.update { it.copy(isServerReachable = null, latencyMs = null) }
            val startMs = System.currentTimeMillis()
            val result = repository.testServerConnection()
            val elapsed = System.currentTimeMillis() - startMs
            result.onSuccess {
                _uiState.update {
                    it.copy(
                        isServerReachable = true,
                        latencyMs = elapsed,
                        errorMessage = null,
                        serverHost = ServerConfigManager.serverAddress
                    )
                }
            }.onFailure { error ->
                _uiState.update {
                    it.copy(
                        isServerReachable = false,
                        latencyMs = elapsed,
                        errorMessage = error.localizedMessage
                    )
                }
            }
        }
    }

    fun updateServerHost(newHost: String) {
        ServerConfigManager.serverAddress = newHost
        _uiState.update { it.copy(serverHost = ServerConfigManager.serverAddress) }
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
                            playerStatus = session.status,
                            sessionStatus = session.sessionStatus,
                            totalPlayers = session.totalPlayers,
                            minPlayersRequired = session.minPlayersRequired,
                            canStartGame = session.canStart
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
        val maxT = 120f
        val stepT = maxT / totalSteps

        for (i in 0..totalSteps) {
            val t = i * stepT
            val denominator = (update.a * exp(-update.r * t.toDouble())) + 1.0
            val pt = (update.k / denominator).toFloat()
            points.add(Pair(t, pt))
        }

        val isSessionActive = update.sessionStatus == "ACTIVE" || _uiState.value.sessionStatus == "ACTIVE"
        val isCompleted = update.sessionStatus == "COMPLETED"
        val wasWaiting = _uiState.value.sessionStatus == "WAITING_FOR_PLAYERS"

        _uiState.update {
            it.copy(
                metrics = metrics,
                curvePoints = points,
                sessionStatus = update.sessionStatus ?: it.sessionStatus,
                isGameFinished = isCompleted || it.isGameFinished,
                totalPlayers = if (update.totalPlayers > 0) update.totalPlayers else it.totalPlayers,
                minPlayersRequired = update.minPlayersRequired,
                canStartGame = update.canStart,
                // Si la sesión pasa a ACTIVE y estamos en LOBBY, pasar al Tablero
                currentScreen = if (isSessionActive && it.currentScreen == AppScreen.LOBBY) AppScreen.GAME_DASHBOARD else it.currentScreen
            )
        }

        // Sincronizar estado del jugador inmediatamente si la partida pasó a estar activa y seguimos en HEALTHY
        if (isSessionActive && (wasWaiting || _uiState.value.playerStatus == PlayerStatus.HEALTHY)) {
            syncPlayerStatus()
        }
    }

    private fun handleStateChange(stateChange: StateChangeDto) {
        // Validar que el mensaje pertenezca a este nodo si viene especificado
        val myUuid = _uiState.value.playerUuid
        if (myUuid != null && stateChange.playerUuid.isNotBlank() && !stateChange.playerUuid.equals(myUuid, ignoreCase = true)) {
            return
        }

        when (stateChange.newStatus) {
            "INFECTED" -> {
                _uiState.update {
                    it.copy(
                        playerStatus = PlayerStatus.INFECTED,
                        isCrimsonGlitchActive = true
                    )
                }
                hapticFeedbackManager.triggerTachycardiaVibration()
                repository.updatePlayerStatusLocally(PlayerStatus.INFECTED)
            }
            "PATIENT_ZERO" -> {
                _uiState.update {
                    it.copy(
                        playerStatus = PlayerStatus.PATIENT_ZERO,
                        isCrimsonGlitchActive = true
                    )
                }
                hapticFeedbackManager.triggerTachycardiaVibration()
                repository.updatePlayerStatusLocally(PlayerStatus.PATIENT_ZERO)
            }
            "HEALTHY" -> {
                _uiState.update { it.copy(playerStatus = PlayerStatus.HEALTHY) }
                repository.updatePlayerStatusLocally(PlayerStatus.HEALTHY)
            }
        }
    }

    fun syncPlayerStatus() {
        val sessionId = _uiState.value.sessionId ?: return
        val playerUuid = _uiState.value.playerUuid ?: return

        viewModelScope.launch {
            repository.fetchPlayerStatus(sessionId, playerUuid).onSuccess { status ->
                if (status != _uiState.value.playerStatus) {
                    val isInfectedOrPatientZero = status == PlayerStatus.PATIENT_ZERO || status == PlayerStatus.INFECTED
                    if (isInfectedOrPatientZero) {
                        hapticFeedbackManager.triggerTachycardiaVibration()
                    }
                    _uiState.update {
                        it.copy(
                            playerStatus = status,
                            isCrimsonGlitchActive = isInfectedOrPatientZero
                        )
                    }
                }
            }
        }
    }

    fun joinGameSession(sessionId: Long, fcmToken: String? = null) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            val result = repository.joinSession(sessionId, fcmToken)
            result.onSuccess {
                _uiState.update { state ->
                    state.copy(
                        isLoading = false,
                        currentScreen = if (state.sessionStatus == "ACTIVE") AppScreen.GAME_DASHBOARD else AppScreen.LOBBY
                    )
                }
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
                _uiState.update {
                    it.copy(
                        sessionId = session.sessionId,
                        sessionCode = session.sessionCode,
                        sessionStatus = session.status,
                        totalPlayers = session.totalPlayers,
                        canStartGame = session.canStart,
                        minPlayersRequired = session.minPlayersRequired,
                        isHost = true
                    )
                }
                joinGameSession(session.sessionId)
            }.onFailure { error ->
                _uiState.update { state ->
                    state.copy(isLoading = false, errorMessage = error.localizedMessage)
                }
            }
        }
    }

    fun startGameSession(sessionId: Long) {
        val total = _uiState.value.totalPlayers
        if (total < 3) {
            _uiState.update {
                it.copy(
                    errorMessage = "No se puede iniciar: se requieren al menos 3 personas conectadas. Actualmente hay $total."
                )
            }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            val result = repository.startSession(sessionId)
            result.onSuccess { session ->
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        sessionStatus = "ACTIVE",
                        totalPlayers = session.totalPlayers,
                        canStartGame = true,
                        currentScreen = AppScreen.GAME_DASHBOARD
                    )
                }
                syncPlayerStatus()
            }.onFailure { error ->
                _uiState.update {
                    it.copy(isLoading = false, errorMessage = error.localizedMessage)
                }
            }
        }
    }

    fun toggleLeaveDialog(show: Boolean) {
        _uiState.update { it.copy(showLeaveDialog = show) }
    }

    fun toggleFinishDialog(show: Boolean) {
        _uiState.update { it.copy(showFinishDialog = show) }
    }

    fun leaveGameSession() {
        val currentSessionId = _uiState.value.sessionId
        val currentUuid = _uiState.value.playerUuid
        _uiState.update { it.copy(showLeaveDialog = false, isLoading = true) }

        viewModelScope.launch {
            if (currentSessionId != null && currentUuid != null) {
                repository.leaveSession(currentSessionId, currentUuid)
            } else {
                repository.clearCurrentSession()
            }
            _uiState.update {
                it.copy(
                    isLoading = false,
                    sessionId = null,
                    sessionCode = "",
                    playerUuid = null,
                    playerStatus = PlayerStatus.HEALTHY,
                    sessionStatus = "WAITING_FOR_PLAYERS",
                    totalPlayers = 0,
                    canStartGame = false,
                    isHost = false,
                    isGameFinished = false,
                    currentScreen = AppScreen.WELCOME
                )
            }
        }
    }

    fun finishGameSession() {
        val currentSessionId = _uiState.value.sessionId ?: return
        _uiState.update { it.copy(showFinishDialog = false, isLoading = true) }

        viewModelScope.launch {
            val result = repository.finishSession(currentSessionId)
            result.onSuccess { session ->
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        sessionStatus = "COMPLETED",
                        isGameFinished = true
                    )
                }
            }.onFailure { error ->
                _uiState.update {
                    it.copy(isLoading = false, errorMessage = error.localizedMessage)
                }
            }
        }
    }

    fun returnToWelcomeAfterGame() {
        repository.clearCurrentSession()
        _uiState.update {
            it.copy(
                sessionId = null,
                sessionCode = "",
                playerUuid = null,
                playerStatus = PlayerStatus.HEALTHY,
                sessionStatus = "WAITING_FOR_PLAYERS",
                totalPlayers = 0,
                canStartGame = false,
                isHost = false,
                isGameFinished = false,
                currentScreen = AppScreen.WELCOME
            )
        }
    }

    fun dismissCrimsonGlitch() {
        _uiState.update { it.copy(isCrimsonGlitchActive = false) }
    }
}
