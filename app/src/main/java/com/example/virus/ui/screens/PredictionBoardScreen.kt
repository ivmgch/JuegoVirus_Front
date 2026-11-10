package com.example.virus.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.BluetoothSearching
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.filled.Coronavirus
import androidx.compose.material.icons.filled.Dangerous
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.HealthAndSafety
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.virus.data.api.ServerConfigManager
import com.example.virus.model.domain.PlayerStatus
import com.example.virus.ui.theme.BeigeBackground
import com.example.virus.ui.theme.BeigeCream
import com.example.virus.ui.theme.BeigeDeep
import com.example.virus.ui.theme.BeigeGold
import com.example.virus.ui.theme.BeigeSurface
import com.example.virus.ui.theme.BeigeSurfaceAlt
import com.example.virus.ui.theme.BorderBeige
import com.example.virus.ui.theme.BorderLavender
import com.example.virus.ui.theme.PurpleDark
import com.example.virus.ui.theme.PurpleDeep
import com.example.virus.ui.theme.PurpleMedium
import com.example.virus.ui.theme.PurplePrimary
import com.example.virus.ui.theme.PurpleSoftTint
import com.example.virus.ui.theme.StatusGreenBg
import com.example.virus.ui.theme.StatusGreenBorder
import com.example.virus.ui.theme.StatusGreenDot
import com.example.virus.ui.theme.StatusGreenText
import com.example.virus.ui.theme.StatusRedBg
import com.example.virus.ui.theme.StatusRedBorder
import com.example.virus.ui.theme.StatusRedDot
import com.example.virus.ui.theme.StatusRedText
import com.example.virus.ui.theme.StatusYellowBg
import com.example.virus.ui.theme.StatusYellowBorder
import com.example.virus.ui.theme.StatusYellowDot
import com.example.virus.ui.theme.StatusYellowText
import com.example.virus.ui.theme.TextMuted
import com.example.virus.ui.theme.TextPrimary
import com.example.virus.ui.theme.TextSecondary
import com.example.virus.ui.viewmodel.AppScreen
import com.example.virus.ui.viewmodel.GameViewModel
import com.patrykandpatrick.vico.compose.axis.horizontal.rememberBottomAxis
import com.patrykandpatrick.vico.compose.axis.vertical.rememberStartAxis
import com.patrykandpatrick.vico.compose.chart.Chart
import com.patrykandpatrick.vico.compose.chart.line.lineChart
import com.patrykandpatrick.vico.core.entry.ChartEntryModelProducer
import com.patrykandpatrick.vico.core.entry.entryOf

@Composable
fun PredictionBoardScreen(
    viewModel: GameViewModel,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()
    val chartEntryModelProducer = remember { ChartEntryModelProducer() }

    LaunchedEffect(uiState.curvePoints) {
        if (uiState.curvePoints.isNotEmpty()) {
            val entries = uiState.curvePoints.map { (x, y) -> entryOf(x, y) }
            chartEntryModelProducer.setEntries(entries)
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(BeigeBackground)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
                .verticalScroll(rememberScrollState())
        ) {
            // 1. Barra Superior de Conexión y Navegación
            HeaderSection(
                isConnected = uiState.isConnected,
                sessionCode = uiState.sessionCode,
                sessionId = uiState.sessionId,
                sessionStatus = uiState.sessionStatus,
                isServerReachable = uiState.isServerReachable,
                onBackToWelcome = { viewModel.navigateTo(AppScreen.WELCOME) }
            )

            Spacer(modifier = Modifier.height(12.dp))

            // 1.1 Tarjeta de Estado y Configuración del Servidor
            ServerConnectionCard(
                serverHost = uiState.serverHost,
                isServerReachable = uiState.isServerReachable,
                latencyMs = uiState.latencyMs,
                onUpdateHost = { viewModel.updateServerHost(it) },
                onTestConnection = { viewModel.testServerConnection() }
            )

            // 1.2 Banner de Error
            if (uiState.errorMessage != null) {
                Spacer(modifier = Modifier.height(8.dp))
                ErrorMessageCard(
                    message = uiState.errorMessage!!,
                    onDismiss = { viewModel.clearError() }
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            // 2. Control de flujo según estado de sesión
            if (uiState.sessionId == null) {
                // Caso A: No hay sesión -> Crear o Unirse
                JoinSessionCard(
                    isLoading = uiState.isLoading,
                    onJoin = { id -> viewModel.joinGameSession(id) },
                    onCreate = { code, p0 -> viewModel.createGameSession(code, p0) }
                )
            } else if (uiState.sessionStatus == "WAITING_FOR_PLAYERS") {
                // Caso B: Sesión en Sala de Espera (Lobby) -> Ver cuántas personas hay conectadas (Mínimo 3)
                LobbyWaitingCard(
                    sessionId = uiState.sessionId!!,
                    sessionCode = uiState.sessionCode,
                    totalPlayers = uiState.totalPlayers,
                    minPlayersRequired = uiState.minPlayersRequired,
                    canStart = uiState.canStartGame || uiState.totalPlayers >= 3,
                    isHost = uiState.isHost,
                    isLoading = uiState.isLoading,
                    onStartGame = { viewModel.startGameSession(uiState.sessionId!!) },
                    onLeaveClick = { viewModel.toggleLeaveDialog(true) },
                    onFinishClick = { viewModel.toggleFinishDialog(true) }
                )
            } else {
                // Caso C: Partida Activa -> Mostrar Tablero Logístico, Métricas y Radar
                PlayerStatusBanner(status = uiState.playerStatus)

                Spacer(modifier = Modifier.height(16.dp))

                MetricsGridSection(
                    k = uiState.metrics.carryingCapacityK,
                    r = uiState.metrics.growthRateR,
                    a = uiState.metrics.integrationConstantA,
                    infected = uiState.metrics.totalInfected,
                    healthy = uiState.metrics.totalHealthy,
                    theoreticalPt = uiState.metrics.theoreticalInfectedPt
                )

                Spacer(modifier = Modifier.height(20.dp))

                LogisticChartCard(
                    chartEntryModelProducer = chartEntryModelProducer,
                    hasData = uiState.curvePoints.isNotEmpty()
                )

                Spacer(modifier = Modifier.height(20.dp))

                BleRadarIndicator()

                Spacer(modifier = Modifier.height(20.dp))

                GameControlActionsCard(
                    isHost = uiState.isHost,
                    onLeaveClick = { viewModel.toggleLeaveDialog(true) },
                    onFinishClick = { viewModel.toggleFinishDialog(true) }
                )

                Spacer(modifier = Modifier.height(30.dp))
            }
        }

        // 3. Efecto Alerta al ser Infectado
        CrimsonGlitchOverlay(
            isActive = uiState.isCrimsonGlitchActive,
            onDismiss = { viewModel.dismissCrimsonGlitch() }
        )

        // 4. Diálogos de Confirmación y Fin de Partida
        if (uiState.showLeaveDialog) {
            LeaveConfirmDialog(
                onConfirm = { viewModel.leaveGameSession() },
                onDismiss = { viewModel.toggleLeaveDialog(false) }
            )
        }

        if (uiState.showFinishDialog) {
            FinishConfirmDialog(
                onConfirm = { viewModel.finishGameSession() },
                onDismiss = { viewModel.toggleFinishDialog(false) }
            )
        }

        if (uiState.isGameFinished || uiState.sessionStatus == "COMPLETED") {
            GameOverSummaryDialog(
                playerStatus = uiState.playerStatus,
                totalInfected = uiState.metrics.totalInfected,
                totalHealthy = uiState.metrics.totalHealthy,
                totalPlayers = uiState.totalPlayers,
                onReturnHome = { viewModel.returnToWelcomeAfterGame() }
            )
        }
    }
}

@Composable
private fun HeaderSection(
    isConnected: Boolean,
    sessionCode: String,
    sessionId: Long?,
    sessionStatus: String,
    isServerReachable: Boolean?,
    onBackToWelcome: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Surface(
                shape = CircleShape,
                color = PurpleSoftTint,
                border = BorderStroke(1.dp, BorderLavender),
                modifier = Modifier
                    .size(38.dp)
                    .clickable { onBackToWelcome() }
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Volver",
                        tint = PurpleDeep,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.width(10.dp))

            Column {
                Text(
                    text = "SIMULACIÓN VIRAL",
                    color = PurpleDeep,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 1.2.sp
                )
                Text(
                    text = if (sessionId != null) {
                        if (sessionStatus == "WAITING_FOR_PLAYERS") "Sala: $sessionCode (ID: $sessionId)"
                        else "Partida: $sessionCode (ID: $sessionId)"
                    } else "Simulador de Campus BLE",
                    color = TextSecondary,
                    fontSize = 11.sp
                )
            }
        }

        // Chip de estado WebSocket
        Surface(
            shape = RoundedCornerShape(12.dp),
            color = if (isConnected) StatusGreenBg else BeigeSurfaceAlt,
            border = BorderStroke(1.dp, if (isConnected) StatusGreenBorder else BorderBeige)
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(if (isConnected) StatusGreenDot else TextMuted)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = if (isConnected) "BROKER EN LÍNEA" else if (sessionId != null) "CONECTANDO WS..." else "WS EN ESPERA",
                    color = if (isConnected) StatusGreenText else TextSecondary,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
private fun ServerConnectionCard(
    serverHost: String,
    isServerReachable: Boolean?,
    latencyMs: Long?,
    onUpdateHost: (String) -> Unit,
    onTestConnection: () -> Unit
) {
    var hostInput by remember(serverHost) { mutableStateOf(serverHost) }
    var isExpanded by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = BeigeSurface),
        border = BorderStroke(1.dp, BorderBeige),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .clip(CircleShape)
                            .background(
                                when (isServerReachable) {
                                    true -> StatusGreenDot
                                    false -> StatusRedDot
                                    null -> StatusYellowDot
                                }
                            )
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = when (isServerReachable) {
                            true -> "BACKEND CONECTADO (${latencyMs ?: 0}ms)"
                            false -> "ERROR DE CONEXIÓN CON BACKEND"
                            null -> "VERIFICANDO SERVIDOR..."
                        },
                        color = when (isServerReachable) {
                            true -> StatusGreenText
                            false -> StatusRedText
                            null -> StatusYellowText
                        },
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.sp
                    )
                }

                Text(
                    text = if (isExpanded) "Ocultar" else "Configurar IP",
                    color = PurplePrimary,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.clickable { isExpanded = !isExpanded }
                )
            }

            Text(
                text = "URL Base: ${ServerConfigManager.getHttpBaseUrl()}",
                color = TextSecondary,
                fontSize = 10.sp,
                fontFamily = FontFamily.Monospace,
                modifier = Modifier.padding(top = 2.dp)
            )

            if (isExpanded) {
                Spacer(modifier = Modifier.height(10.dp))
                OutlinedTextField(
                    value = hostInput,
                    onValueChange = { hostInput = it },
                    label = { Text("IP / Host y Puerto (ej. 192.168.1.35:8080)") },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary,
                        focusedBorderColor = PurplePrimary,
                        unfocusedBorderColor = BorderLavender,
                        focusedLabelColor = PurplePrimary,
                        unfocusedLabelColor = TextSecondary
                    ),
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))

                // Presets rápidos
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    listOf("192.168.1.18:8080", "10.0.2.2:8080", "localhost:8080").forEach { preset ->
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = BeigeSurfaceAlt,
                            border = BorderStroke(1.dp, BorderBeige),
                            modifier = Modifier
                                .weight(1f)
                                .clickable {
                                    hostInput = preset
                                    onUpdateHost(preset)
                                }
                        ) {
                            Text(
                                text = preset.substringBefore(":8080"),
                                color = PurpleDeep,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.SemiBold,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.padding(vertical = 6.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    Button(
                        onClick = { onUpdateHost(hostInput) },
                        colors = ButtonDefaults.buttonColors(containerColor = PurplePrimary),
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 6.dp)
                    ) {
                        Text("Guardar y Probar", color = BeigeCream, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
private fun LobbyWaitingCard(
    sessionId: Long,
    sessionCode: String,
    totalPlayers: Long,
    minPlayersRequired: Int,
    canStart: Boolean,
    isHost: Boolean,
    isLoading: Boolean,
    onStartGame: () -> Unit,
    onLeaveClick: () -> Unit,
    onFinishClick: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = BeigeSurface),
        border = BorderStroke(1.dp, BorderBeige),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "SALA DE ESPERA (LOBBY)",
                        color = PurplePrimary,
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        letterSpacing = 1.sp
                    )
                    Text(
                        text = "Partida: $sessionCode (ID #$sessionId)",
                        color = PurpleDeep,
                        fontWeight = FontWeight.Black,
                        fontSize = 16.sp
                    )
                }

                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = if (canStart) StatusGreenBg else StatusYellowBg,
                    border = BorderStroke(1.dp, if (canStart) StatusGreenBorder else StatusYellowBorder)
                ) {
                    Text(
                        text = if (canStart) "¡LISTO PARA INICIAR!" else "ESPERANDO JUGADORES",
                        color = if (canStart) StatusGreenText else StatusYellowText,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Contador Principal de Jugadores Conectados
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = BeigeSurfaceAlt,
                border = BorderStroke(1.dp, BorderBeige),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "JUGADORES CONECTADOS",
                        color = TextSecondary,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(verticalAlignment = Alignment.Bottom) {
                        Text(
                            text = "$totalPlayers",
                            color = if (canStart) StatusGreenText else PurpleDeep,
                            fontSize = 44.sp,
                            fontWeight = FontWeight.Black
                        )
                        Text(
                            text = " / $minPlayersRequired MÍNIMO",
                            color = TextMuted,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(bottom = 8.dp, start = 4.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    val progress = (totalPlayers / minPlayersRequired.toFloat()).coerceIn(0f, 1f)
                    LinearProgressIndicator(
                        progress = { progress },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(8.dp)
                            .clip(RoundedCornerShape(4.dp)),
                        color = if (canStart) StatusGreenDot else PurplePrimary,
                        trackColor = BorderBeige
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Cuadro de estado / advertencia
            if (!canStart) {
                val missing = (minPlayersRequired - totalPlayers).coerceAtLeast(1)
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = StatusYellowBg,
                    border = BorderStroke(1.dp, StatusYellowBorder),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Groups,
                            contentDescription = null,
                            tint = StatusYellowDot,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "Falta(n) $missing jugador(es) conectado(s) para poder iniciar la partida. Comparte el ID #$sessionId con tus compañeros.",
                            color = StatusYellowText,
                            fontSize = 12.sp,
                            lineHeight = 16.sp
                        )
                    }
                }
            } else {
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = StatusGreenBg,
                    border = BorderStroke(1.dp, StatusGreenBorder),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Groups,
                            contentDescription = null,
                            tint = StatusGreenDot,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "¡Cupo mínimo alcanzado! Hay $totalPlayers jugadores conectados. El host ya puede iniciar la partida.",
                            color = StatusGreenText,
                            fontSize = 12.sp,
                            lineHeight = 16.sp
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Botón de Iniciar Partida (Solo el Host puede iniciar el sorteo cuando hay >= 3)
            if (isHost) {
                Button(
                    onClick = onStartGame,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(54.dp),
                    enabled = canStart && !isLoading,
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = PurplePrimary,
                        disabledContainerColor = BeigeSurfaceAlt
                    ),
                    elevation = ButtonDefaults.buttonElevation(defaultElevation = 2.dp)
                ) {
                    if (isLoading) {
                        CircularProgressIndicator(color = Color.White, modifier = Modifier.size(20.dp))
                    } else {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = if (canStart) "🎲 INICIAR Y SORTEAR VIRUS" else "ESPERANDO JUGADORES ($totalPlayers/$minPlayersRequired MÍNIMO)",
                                color = if (canStart) BeigeCream else TextMuted,
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp
                            )
                            if (canStart) {
                                Spacer(modifier = Modifier.width(8.dp))
                                Icon(
                                    imageVector = Icons.Default.PlayArrow,
                                    contentDescription = null,
                                    tint = BeigeCream,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    }
                }
            } else {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = if (canStart) StatusGreenBg else BeigeSurfaceAlt,
                    border = BorderStroke(1.dp, if (canStart) StatusGreenBorder else BorderBeige),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (canStart) {
                            CircularProgressIndicator(
                                color = StatusGreenDot,
                                strokeWidth = 2.dp,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = "🎉 ¡Cupo alcanzado ($totalPlayers jugadores)! Esperando que el Host inicie el sorteo del virus...",
                                color = StatusGreenText,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Default.Groups,
                                contentDescription = null,
                                tint = TextMuted,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = "⏳ Esperando a que se conecten más jugadores ($totalPlayers / $minPlayersRequired mínimo)...",
                                color = TextSecondary,
                                fontSize = 12.sp
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = StatusRedBg,
                    border = BorderStroke(1.dp, StatusRedBorder),
                    modifier = Modifier
                        .weight(1f)
                        .height(44.dp)
                        .clickable { onLeaveClick() }
                ) {
                    Row(
                        modifier = Modifier.fillMaxSize(),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ExitToApp,
                            contentDescription = null,
                            tint = StatusRedText,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("SALIR DE SALA", color = StatusRedText, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }

                if (isHost) {
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = Color(0xFFC62828),
                        modifier = Modifier
                            .weight(1f)
                            .height(44.dp)
                            .clickable { onFinishClick() }
                    ) {
                        Row(
                            modifier = Modifier.fillMaxSize(),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Stop,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("CANCELAR (HOST)", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun JoinSessionCard(
    isLoading: Boolean,
    onJoin: (Long) -> Unit,
    onCreate: (String, Int) -> Unit
) {
    var sessionCodeInput by remember { mutableStateOf("CAMPUS-" + (1000..9999).random()) }
    var sessionIdInput by remember { mutableStateOf("") }
    var p0Input by remember { mutableStateOf("1") }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = BeigeSurface),
        border = BorderStroke(1.dp, BorderBeige),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "INICIALIZAR O UNIRSE A SALA",
                color = PurpleDeep,
                fontWeight = FontWeight.Black,
                fontSize = 14.sp
            )
            Spacer(modifier = Modifier.height(12.dp))

            OutlinedTextField(
                value = sessionIdInput,
                onValueChange = { sessionIdInput = it },
                label = { Text("ID de Sala existente") },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = TextPrimary,
                    unfocusedTextColor = TextPrimary,
                    focusedBorderColor = PurplePrimary,
                    unfocusedBorderColor = BorderBeige,
                    focusedLabelColor = PurplePrimary,
                    unfocusedLabelColor = TextSecondary
                ),
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(8.dp))

            Button(
                onClick = { sessionIdInput.toLongOrNull()?.let { onJoin(it) } },
                modifier = Modifier.fillMaxWidth(),
                enabled = !isLoading,
                colors = ButtonDefaults.buttonColors(containerColor = PurplePrimary)
            ) {
                if (isLoading) {
                    CircularProgressIndicator(color = Color.White, modifier = Modifier.size(20.dp))
                } else {
                    Text("UNIRSE A SALA DE ESPERA", color = BeigeCream, fontWeight = FontWeight.Bold)
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
            Text(text = "O crear una nueva sala de simulación:", color = TextSecondary, fontSize = 12.sp)
            Spacer(modifier = Modifier.height(8.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = sessionCodeInput,
                    onValueChange = { sessionCodeInput = it },
                    label = { Text("Código") },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary,
                        focusedBorderColor = PurplePrimary,
                        unfocusedBorderColor = BorderBeige,
                        focusedLabelColor = PurplePrimary,
                        unfocusedLabelColor = TextSecondary
                    ),
                    modifier = Modifier.weight(2f)
                )
                OutlinedTextField(
                    value = p0Input,
                    onValueChange = { p0Input = it },
                    label = { Text("P(0)") },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary,
                        focusedBorderColor = PurplePrimary,
                        unfocusedBorderColor = BorderBeige,
                        focusedLabelColor = PurplePrimary,
                        unfocusedLabelColor = TextSecondary
                    ),
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Button(
                onClick = {
                    val p0 = p0Input.toIntOrNull() ?: 1
                    onCreate(sessionCodeInput, p0)
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = !isLoading,
                colors = ButtonDefaults.buttonColors(containerColor = BeigeGold)
            ) {
                Text("CREAR SALA (HOST)", fontWeight = FontWeight.Black, color = PurpleDeep)
            }
        }
    }
}

@Composable
private fun ErrorMessageCard(
    message: String,
    onDismiss: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = StatusRedBg),
        border = BorderStroke(1.dp, StatusRedBorder)
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.Warning,
                contentDescription = null,
                tint = StatusRedDot,
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.width(10.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Aviso del Sistema",
                    color = StatusRedText,
                    fontWeight = FontWeight.Bold,
                    fontSize = 11.sp
                )
                Text(
                    text = message,
                    color = TextPrimary,
                    fontSize = 12.sp,
                    lineHeight = 16.sp
                )
            }
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = "✕",
                color = StatusRedText,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier
                    .clickable { onDismiss() }
                    .padding(8.dp)
            )
        }
    }
}

@Composable
private fun PlayerStatusBanner(status: PlayerStatus) {
    val (backgroundColor, borderColor, title, description, icon) = when (status) {
        PlayerStatus.HEALTHY -> Quintuple(
            StatusGreenBg,
            StatusGreenBorder,
            "ESTADO: SANO (VULNERABLE)",
            "Tu receptor BLE está pasivo. Mantén más de 2 metros de distancia con personas infectadas.",
            Icons.Default.HealthAndSafety
        )
        PlayerStatus.PATIENT_ZERO -> Quintuple(
            StatusRedBg,
            StatusRedBorder,
            "ESTADO: PACIENTE CERO (VECTOR)",
            "Eres el emisor inicial del virus. Tu firma BLE contagia a nodos cercanos expuestos por >15s.",
            Icons.Default.Dangerous
        )
        PlayerStatus.INFECTED -> Quintuple(
            Color(0xFFFDE8E8),
            Color(0xFFEF9A9A),
            "ESTADO: INFECTADO (CONTAGIOSO)",
            "Has sido infectado por proximidad. Ahora actúas como vector de propagación activa en el campus.",
            Icons.Default.Coronavirus
        )
        PlayerStatus.IMMUNE -> Quintuple(
            PurpleSoftTint,
            BorderLavender,
            "ESTADO: INMUNE (ESCUDO ACTIVO)",
            "Inmunidad adquirida. El servidor rechazará cualquier reporte de infección sobre tu dispositivo.",
            Icons.Default.Shield
        )
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = backgroundColor),
        border = BorderStroke(1.5.dp, borderColor)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = when (status) {
                    PlayerStatus.HEALTHY -> StatusGreenDot
                    PlayerStatus.PATIENT_ZERO -> StatusRedDot
                    PlayerStatus.INFECTED -> Color(0xFFC62828)
                    PlayerStatus.IMMUNE -> PurplePrimary
                },
                modifier = Modifier.size(44.dp)
            )
            Spacer(modifier = Modifier.width(14.dp))
            Column {
                Text(
                    text = title,
                    color = when (status) {
                        PlayerStatus.HEALTHY -> StatusGreenText
                        PlayerStatus.PATIENT_ZERO -> StatusRedText
                        PlayerStatus.INFECTED -> Color(0xFFB71C1C)
                        PlayerStatus.IMMUNE -> PurpleDeep
                    },
                    fontWeight = FontWeight.Black,
                    fontSize = 14.sp
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = description,
                    color = TextSecondary,
                    fontSize = 12.sp,
                    lineHeight = 16.sp
                )
            }
        }
    }
}

@Composable
private fun MetricsGridSection(
    k: Double,
    r: Double,
    a: Double,
    infected: Long,
    healthy: Long,
    theoreticalPt: Double
) {
    Column {
        Text(
            text = "VARIABLES MATEMÁTICAS EN TIEMPO REAL (REDIS/SPRING)",
            color = PurpleDeep,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(8.dp))

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            MetricBox(
                title = "k (Capacidad)",
                value = "${k.toInt()}",
                subtitle = "Población activa (2 min)",
                accentColor = PurplePrimary,
                modifier = Modifier.weight(1f)
            )
            MetricBox(
                title = "r (Propagación)",
                value = String.format("%.3f", r),
                subtitle = "Densidad BLE dinámica",
                accentColor = BeigeDeep,
                modifier = Modifier.weight(1f)
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            MetricBox(
                title = "Infectados",
                value = "$infected",
                subtitle = "Sanos: $healthy",
                accentColor = StatusRedDot,
                modifier = Modifier.weight(1f)
            )
            MetricBox(
                title = "P(t) Teórico",
                value = String.format("%.2f", theoreticalPt),
                subtitle = "Constante A: ${String.format("%.2f", a)}",
                accentColor = PurpleDark,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun MetricBox(
    title: String,
    value: String,
    subtitle: String,
    accentColor: Color,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = BeigeSurface),
        border = BorderStroke(1.dp, BorderBeige),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(text = title, color = TextSecondary, fontSize = 11.sp)
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = value,
                color = accentColor,
                fontWeight = FontWeight.Black,
                fontSize = 20.sp
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(text = subtitle, color = TextMuted, fontSize = 10.sp)
        }
    }
}

@Composable
private fun LogisticChartCard(
    chartEntryModelProducer: ChartEntryModelProducer,
    hasData: Boolean
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = BeigeSurface),
        border = BorderStroke(1.dp, BorderBeige),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "CURVA DE CRECIMIENTO LOGÍSTICO (P(t))",
                color = PurpleDeep,
                fontSize = 13.sp,
                fontWeight = FontWeight.Black
            )
            Text(
                text = "P(t) = k / (A * e^(-rt) + 1) renderizado por Vico",
                color = TextSecondary,
                fontSize = 11.sp
            )

            Spacer(modifier = Modifier.height(16.dp))

            if (hasData) {
                Chart(
                    chart = lineChart(),
                    chartModelProducer = chartEntryModelProducer,
                    startAxis = rememberStartAxis(
                        title = "Infectados",
                        tickLength = 4.dp
                    ),
                    bottomAxis = rememberBottomAxis(
                        title = "Tiempo t (s)",
                        tickLength = 4.dp
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(220.dp)
                )
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(180.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Esperando primer BOARD_UPDATE del servidor...",
                        color = TextMuted,
                        fontSize = 12.sp
                    )
                }
            }
        }
    }
}

@Composable
private fun BleRadarIndicator() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = PurpleSoftTint),
        border = BorderStroke(1.dp, BorderLavender)
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.BluetoothSearching,
                contentDescription = null,
                tint = PurplePrimary,
                modifier = Modifier.size(28.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text(
                    text = "RADAR BLE ACTIVO (Foreground Service)",
                    color = PurpleDeep,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Transmitiendo baliza anónima y escaneando RSSI continuo para filtrar proximidad < 2 metros.",
                    color = TextSecondary,
                    fontSize = 11.sp
                )
            }
        }
    }
}

@Composable
private fun CrimsonGlitchOverlay(
    isActive: Boolean,
    onDismiss: () -> Unit
) {
    val alphaAnim = remember { Animatable(0.85f) }

    LaunchedEffect(isActive) {
        if (isActive) {
            alphaAnim.animateTo(
                targetValue = 0.4f,
                animationSpec = infiniteRepeatable(
                    animation = tween(120, easing = LinearEasing),
                    repeatMode = RepeatMode.Reverse
                )
            )
        }
    }

    AnimatedVisibility(
        visible = isActive,
        enter = fadeIn(tween(100)),
        exit = fadeOut(tween(300))
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.radialGradient(
                        colors = listOf(
                            Color(0xFFC2185B).copy(alpha = alphaAnim.value),
                            Color(0xFF4A0020).copy(alpha = 0.92f)
                        )
                    )
                )
                .clickable { onDismiss() }
                .padding(24.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(
                    imageVector = Icons.Default.Warning,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(80.dp)
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "¡CONTAGIO CONFIRMADO!",
                    color = Color.White,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Black,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Has estado expuesto a un portador del virus por más de 15 segundos a menos de 2 metros.\n\nTu ritmo cardíaco se acelera. Tu firma BLE ahora infecta a otros.",
                    color = Color(0xFFFFCDD2),
                    fontSize = 14.sp,
                    textAlign = TextAlign.Center,
                    lineHeight = 20.sp
                )
                Spacer(modifier = Modifier.height(24.dp))
                Button(
                    onClick = { onDismiss() },
                    colors = ButtonDefaults.buttonColors(containerColor = BeigeCream)
                ) {
                    Text("ENTENDIDO (VER TABLERO)", color = Color(0xFF880E4F), fontWeight = FontWeight.Black)
                }
            }
        }
    }
}

@Composable
private fun GameControlActionsCard(
    isHost: Boolean,
    onLeaveClick: () -> Unit,
    onFinishClick: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = BeigeSurface),
        border = BorderStroke(1.dp, BorderBeige)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Text(
                text = "GESTIÓN DE PARTIDA",
                color = TextSecondary,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp
            )
            Spacer(modifier = Modifier.height(10.dp))

            if (isHost) {
                Button(
                    onClick = onFinishClick,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp),
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFC62828))
                ) {
                    Icon(
                        imageVector = Icons.Default.Stop,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "FINALIZAR JUEGO (HOST)",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))
            }

            Surface(
                shape = RoundedCornerShape(10.dp),
                color = StatusRedBg,
                border = BorderStroke(1.dp, StatusRedBorder),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(44.dp)
                    .clickable { onLeaveClick() }
            ) {
                Row(
                    modifier = Modifier.fillMaxSize(),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ExitToApp,
                        contentDescription = null,
                        tint = StatusRedText,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "SALIR DE LA PARTIDA",
                        color = StatusRedText,
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp
                    )
                }
            }
        }
    }
}

@Composable
fun LeaveConfirmDialog(
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = BeigeSurface,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ExitToApp,
                    contentDescription = null,
                    tint = StatusRedDot,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "¿Salir de la Partida?",
                    color = TextPrimary,
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp
                )
            }
        },
        text = {
            Text(
                text = "Tu dispositivo se desconectará del servidor y saldrás de la sala de simulación.",
                color = TextSecondary,
                fontSize = 14.sp
            )
        },
        confirmButton = {
            Button(
                onClick = onConfirm,
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFC62828))
            ) {
                Text("SÍ, SALIR", fontWeight = FontWeight.Bold, color = Color.White)
            }
        },
        dismissButton = {
            Button(
                onClick = onDismiss,
                colors = ButtonDefaults.buttonColors(containerColor = BeigeSurfaceAlt)
            ) {
                Text("CANCELAR", color = PurpleDeep, fontWeight = FontWeight.SemiBold)
            }
        }
    )
}

@Composable
fun FinishConfirmDialog(
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = BeigeSurface,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Stop,
                    contentDescription = null,
                    tint = Color(0xFFC62828),
                    modifier = Modifier.size(26.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "¿Finalizar Juego (Host)?",
                    color = TextPrimary,
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp
                )
            }
        },
        text = {
            Text(
                text = "Esta acción terminará la simulación inmediatamente para TODOS los participantes y mostrará los resultados finales.",
                color = TextSecondary,
                fontSize = 14.sp
            )
        },
        confirmButton = {
            Button(
                onClick = onConfirm,
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFC62828))
            ) {
                Text("FINALIZAR JUEGO", fontWeight = FontWeight.Bold, color = Color.White)
            }
        },
        dismissButton = {
            Button(
                onClick = onDismiss,
                colors = ButtonDefaults.buttonColors(containerColor = BeigeSurfaceAlt)
            ) {
                Text("CANCELAR", color = PurpleDeep, fontWeight = FontWeight.SemiBold)
            }
        }
    )
}

@Composable
fun GameOverSummaryDialog(
    playerStatus: PlayerStatus,
    totalInfected: Long,
    totalHealthy: Long,
    totalPlayers: Long,
    onReturnHome: () -> Unit
) {
    val isInfected = playerStatus == PlayerStatus.INFECTED || playerStatus == PlayerStatus.PATIENT_ZERO
    val isGameStarted = totalInfected > 0 || (totalPlayers >= 3 && totalHealthy > 0)
    val titleText = if (isGameStarted) "🏁 PARTIDA FINALIZADA" else "🛑 SALA CANCELADA"
    val subtitleText = if (isGameStarted) "El host ha concluido la simulación" else "La sala fue cancelada antes de iniciar la partida"

    AlertDialog(
        onDismissRequest = onReturnHome,
        containerColor = BeigeSurface,
        title = {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.fillMaxWidth()
            ) {
                Surface(
                    shape = CircleShape,
                    color = if (isInfected) StatusRedBg else StatusGreenBg,
                    border = BorderStroke(1.5.dp, if (isInfected) StatusRedBorder else StatusGreenBorder),
                    modifier = Modifier.size(60.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = if (isInfected) Icons.Default.Coronavirus else Icons.Default.Shield,
                            contentDescription = null,
                            tint = if (isInfected) StatusRedDot else StatusGreenDot,
                            modifier = Modifier.size(36.dp)
                        )
                    }
                }
                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    text = titleText,
                    color = PurpleDeep,
                    fontWeight = FontWeight.Black,
                    fontSize = 18.sp,
                    letterSpacing = 1.sp
                )
                Text(
                    text = subtitleText,
                    color = TextSecondary,
                    fontSize = 12.sp
                )
            }
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                if (isGameStarted) {
                    // Resultado individual
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = if (isInfected) StatusRedBg else StatusGreenBg,
                        border = BorderStroke(1.dp, if (isInfected) StatusRedBorder else StatusGreenBorder),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = if (isInfected) "ESTADO FINAL: INFECTADO" else "ESTADO FINAL: SOBREVIVIENTE SANO",
                                color = if (isInfected) StatusRedText else StatusGreenText,
                                fontWeight = FontWeight.Black,
                                fontSize = 13.sp
                            )
                            Text(
                                text = if (isInfected) "Fuiste alcanzado por la epidemia en el campus." else "Lograste mantener la distancia y evitar el contagio.",
                                color = TextSecondary,
                                fontSize = 11.sp,
                                textAlign = TextAlign.Center
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Resumen Global
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = BeigeSurfaceAlt,
                            border = BorderStroke(1.dp, BorderBeige),
                            modifier = Modifier.weight(1f)
                        ) {
                            Column(
                                modifier = Modifier.padding(10.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text("Infectados", color = StatusRedText, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                Text("$totalInfected", color = StatusRedDot, fontSize = 22.sp, fontWeight = FontWeight.Black)
                            }
                        }

                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = BeigeSurfaceAlt,
                            border = BorderStroke(1.dp, BorderBeige),
                            modifier = Modifier.weight(1f)
                        ) {
                            Column(
                                modifier = Modifier.padding(10.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text("Sobrevivientes", color = StatusGreenText, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                Text("$totalHealthy", color = StatusGreenDot, fontSize = 22.sp, fontWeight = FontWeight.Black)
                            }
                        }
                    }
                } else {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = BeigeSurfaceAlt,
                        border = BorderStroke(1.dp, BorderBeige),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "La sala se cerró antes de realizar el sorteo del virus. No se registraron contagios en esta sesión.",
                            color = TextSecondary,
                            fontSize = 12.sp,
                            modifier = Modifier.padding(14.dp),
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onReturnHome,
                colors = ButtonDefaults.buttonColors(containerColor = PurplePrimary),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "VOLVER AL MENÚ PRINCIPAL",
                    fontWeight = FontWeight.Bold,
                    color = BeigeCream,
                    fontSize = 12.sp
                )
            }
        }
    )
}

private data class Quintuple<A, B, C, D, E>(
    val first: A,
    val second: B,
    val third: C,
    val fourth: D,
    val fifth: E
)
