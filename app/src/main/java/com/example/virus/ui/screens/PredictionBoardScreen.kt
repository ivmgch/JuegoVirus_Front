package com.example.virus.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
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
import androidx.compose.material.icons.filled.Coronavirus
import androidx.compose.material.icons.filled.Dangerous
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.HealthAndSafety
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
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

    Box(modifier = modifier.fillMaxSize().background(Color(0xFF0F141C))) {
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
                    isLoading = uiState.isLoading,
                    onStartGame = { viewModel.startGameSession(uiState.sessionId!!) }
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
            }
        }

        // 3. Efecto Glitch Rojo Carmesí al ser Infectado
        CrimsonGlitchOverlay(
            isActive = uiState.isCrimsonGlitchActive,
            onDismiss = { viewModel.dismissCrimsonGlitch() }
        )
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
                color = Color(0xFF1F2B3D),
                modifier = Modifier
                    .size(36.dp)
                    .clickable { onBackToWelcome() }
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Volver",
                        tint = Color.White,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.width(10.dp))

            Column {
                Text(
                    text = "SIMULACIÓN VIRAL",
                    color = Color.White,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.2.sp
                )
                Text(
                    text = if (sessionId != null) {
                        if (sessionStatus == "WAITING_FOR_PLAYERS") "Sala: $sessionCode (ID: $sessionId)"
                        else "Partida: $sessionCode (ID: $sessionId)"
                    } else "Simulador de Campus BLE",
                    color = Color(0xFF90A4AE),
                    fontSize = 11.sp
                )
            }
        }

        // Chip de estado WebSocket
        Surface(
            shape = RoundedCornerShape(12.dp),
            color = if (isConnected) Color(0xFF1B5E20) else Color(0xFF263238)
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(if (isConnected) Color(0xFF69F0AE) else Color(0xFF78909C))
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = if (isConnected) "BROKER EN LÍNEA" else if (sessionId != null) "CONECTANDO WS..." else "WS EN ESPERA",
                    color = Color.White,
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
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = when (isServerReachable) {
                true -> Color(0xFF10281D)
                false -> Color(0xFF381216)
                null -> Color(0xFF262312)
            }
        )
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
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
                                    true -> Color(0xFF00E676)
                                    false -> Color(0xFFFF5252)
                                    null -> Color(0xFFFFD600)
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
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.sp
                    )
                }

                Text(
                    text = if (isExpanded) "Ocultar" else "Configurar IP",
                    color = Color(0xFF00B0FF),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.clickable { isExpanded = !isExpanded }
                )
            }

            Text(
                text = "URL Base: ${ServerConfigManager.getHttpBaseUrl()}",
                color = Color(0xFF90A4AE),
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
                            color = Color(0xFF263238),
                            modifier = Modifier
                                .weight(1f)
                                .clickable {
                                    hostInput = preset
                                    onUpdateHost(preset)
                                }
                        ) {
                            Text(
                                text = preset.substringBefore(":8080"),
                                color = Color(0xFF80D8FF),
                                fontSize = 10.sp,
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
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00B0FF)),
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 6.dp)
                    ) {
                        Text("Guardar y Probar", fontSize = 11.sp, fontWeight = FontWeight.Bold)
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
    isLoading: Boolean,
    onStartGame: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF141D2B))
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
                        color = Color(0xFF00E5FF),
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        letterSpacing = 1.sp
                    )
                    Text(
                        text = "Partida: $sessionCode (ID #$sessionId)",
                        color = Color.White,
                        fontWeight = FontWeight.Black,
                        fontSize = 16.sp
                    )
                }

                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = if (canStart) Color(0xFF1B5E20) else Color(0xFF3E2723)
                ) {
                    Text(
                        text = if (canStart) "¡LISTO PARA INICIAR!" else "ESPERANDO JUGADORES",
                        color = if (canStart) Color(0xFF69F0AE) else Color(0xFFFFB74D),
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
                color = Color(0xFF1B2636),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "JUGADORES CONECTADOS",
                        color = Color(0xFF90A4AE),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(verticalAlignment = Alignment.Bottom) {
                        Text(
                            text = "$totalPlayers",
                            color = if (canStart) Color(0xFF00E676) else Color(0xFFFFD54F),
                            fontSize = 44.sp,
                            fontWeight = FontWeight.Black
                        )
                        Text(
                            text = " / $minPlayersRequired MÍNIMO",
                            color = Color(0xFF78909C),
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
                        color = if (canStart) Color(0xFF00E676) else Color(0xFFFFB74D),
                        trackColor = Color(0xFF263238)
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Cuadro de estado / advertencia
            if (!canStart) {
                val missing = (minPlayersRequired - totalPlayers).coerceAtLeast(1)
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = Color(0xFF381F12),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Groups,
                            contentDescription = null,
                            tint = Color(0xFFFFB74D),
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "Falta(n) $missing jugador(es) conectado(s) para poder iniciar la partida. Comparte el ID #$sessionId con tus compañeros.",
                            color = Color(0xFFFFE082),
                            fontSize = 12.sp,
                            lineHeight = 16.sp
                        )
                    }
                }
            } else {
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = Color(0xFF132F20),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Groups,
                            contentDescription = null,
                            tint = Color(0xFF00E676),
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "¡Cupo mínimo alcanzado! Hay $totalPlayers jugadores conectados. El host ya puede iniciar la partida.",
                            color = Color(0xFFA5D6A7),
                            fontSize = 12.sp,
                            lineHeight = 16.sp
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Botón de Iniciar Partida
            Button(
                onClick = onStartGame,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                enabled = canStart && !isLoading,
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF00E676),
                    disabledContainerColor = Color(0xFF263238)
                )
            ) {
                if (isLoading) {
                    CircularProgressIndicator(color = Color.White, modifier = Modifier.size(20.dp))
                } else {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = if (canStart) "INICIAR PARTIDA DE SIMULACIÓN" else "ESPERANDO MÍNIMO 3 JUGADORES",
                            color = if (canStart) Color.Black else Color(0xFF78909C),
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp
                        )
                        if (canStart) {
                            Spacer(modifier = Modifier.width(8.dp))
                            Icon(
                                imageVector = Icons.Default.PlayArrow,
                                contentDescription = null,
                                tint = Color.Black,
                                modifier = Modifier.size(20.dp)
                            )
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
    var sessionCodeInput by remember { mutableStateOf("CAMPUS-CENTRAL") }
    var sessionIdInput by remember { mutableStateOf("1") }
    var p0Input by remember { mutableStateOf("1") }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF182232))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "INICIALIZAR O UNIRSE A SALA",
                color = Color.White,
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp
            )
            Spacer(modifier = Modifier.height(12.dp))

            OutlinedTextField(
                value = sessionIdInput,
                onValueChange = { sessionIdInput = it },
                label = { Text("ID de Sala existente") },
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(8.dp))

            Button(
                onClick = { sessionIdInput.toLongOrNull()?.let { onJoin(it) } },
                modifier = Modifier.fillMaxWidth(),
                enabled = !isLoading,
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00B0FF))
            ) {
                if (isLoading) {
                    CircularProgressIndicator(color = Color.White, modifier = Modifier.size(20.dp))
                } else {
                    Text("UNIRSE A SALA DE ESPERA", fontWeight = FontWeight.Bold)
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
            Text(text = "O crear una nueva sala de simulación:", color = Color(0xFF90A4AE), fontSize = 12.sp)
            Spacer(modifier = Modifier.height(8.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = sessionCodeInput,
                    onValueChange = { sessionCodeInput = it },
                    label = { Text("Código") },
                    modifier = Modifier.weight(2f)
                )
                OutlinedTextField(
                    value = p0Input,
                    onValueChange = { p0Input = it },
                    label = { Text("P(0)") },
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
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00E676))
            ) {
                Text("CREAR SALA (HOST)", fontWeight = FontWeight.Bold, color = Color.Black)
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
        colors = CardDefaults.cardColors(containerColor = Color(0xFF4A1015))
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.Warning,
                contentDescription = null,
                tint = Color(0xFFFF5252),
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.width(10.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Aviso del Sistema",
                    color = Color(0xFFFF8A80),
                    fontWeight = FontWeight.Bold,
                    fontSize = 11.sp
                )
                Text(
                    text = message,
                    color = Color.White,
                    fontSize = 12.sp,
                    lineHeight = 16.sp
                )
            }
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = "✕",
                color = Color(0xFFB0BEC5),
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
            Color(0xFF132F20),
            Color(0xFF00E676),
            "ESTADO: SANO (VULNERABLE)",
            "Tu receptor BLE está pasivo. Mantén más de 2 metros de distancia con personas infectadas.",
            Icons.Default.HealthAndSafety
        )
        PlayerStatus.PATIENT_ZERO -> Quintuple(
            Color(0xFF3E1218),
            Color(0xFFFF1744),
            "ESTADO: PACIENTE CERO (VECTOR)",
            "Eres el emisor inicial del virus. Tu firma BLE contagia a nodos cercanos expuestos por >15s.",
            Icons.Default.Dangerous
        )
        PlayerStatus.INFECTED -> Quintuple(
            Color(0xFF4A0E17),
            Color(0xFFFF5252),
            "ESTADO: INFECTADO (CONTAGIOSO)",
            "Has sido infectado por proximidad. Ahora actúas como vector de propagación activa en el campus.",
            Icons.Default.Coronavirus
        )
        PlayerStatus.IMMUNE -> Quintuple(
            Color(0xFF0D253F),
            Color(0xFF2979FF),
            "ESTADO: INMUNE (ESCUDO ACTIVO)",
            "Inmunidad adquirida. El servidor rechazará cualquier reporte de infección sobre tu dispositivo.",
            Icons.Default.Shield
        )
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = backgroundColor)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = borderColor,
                modifier = Modifier.size(44.dp)
            )
            Spacer(modifier = Modifier.width(14.dp))
            Column {
                Text(
                    text = title,
                    color = borderColor,
                    fontWeight = FontWeight.Black,
                    fontSize = 14.sp
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = description,
                    color = Color(0xFFECEFF1),
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
            color = Color(0xFFB0BEC5),
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(8.dp))

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            MetricBox(
                title = "k (Capacidad)",
                value = "${k.toInt()}",
                subtitle = "Población activa (2 min)",
                accentColor = Color(0xFF00E5FF),
                modifier = Modifier.weight(1f)
            )
            MetricBox(
                title = "r (Propagación)",
                value = String.format("%.3f", r),
                subtitle = "Densidad BLE dinámica",
                accentColor = Color(0xFFFFB74D),
                modifier = Modifier.weight(1f)
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            MetricBox(
                title = "Infectados",
                value = "$infected",
                subtitle = "Sanos: $healthy",
                accentColor = Color(0xFFFF5252),
                modifier = Modifier.weight(1f)
            )
            MetricBox(
                title = "P(t) Teórico",
                value = String.format("%.2f", theoreticalPt),
                subtitle = "Constante A: ${String.format("%.2f", a)}",
                accentColor = Color(0xFFB388FF),
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
        colors = CardDefaults.cardColors(containerColor = Color(0xFF182232))
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(text = title, color = Color(0xFF90A4AE), fontSize = 11.sp)
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = value,
                color = accentColor,
                fontWeight = FontWeight.Black,
                fontSize = 20.sp
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(text = subtitle, color = Color(0xFF78909C), fontSize = 10.sp)
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
        colors = CardDefaults.cardColors(containerColor = Color(0xFF182232))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "CURVA DE CRECIMIENTO LOGÍSTICO (P(t))",
                color = Color.White,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "P(t) = k / (A * e^(-rt) + 1) renderizado por Vico",
                color = Color(0xFF90A4AE),
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
                        color = Color(0xFF78909C),
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
        colors = CardDefaults.cardColors(containerColor = Color(0xFF131D2A))
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.BluetoothSearching,
                contentDescription = null,
                tint = Color(0xFF00E5FF),
                modifier = Modifier.size(28.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text(
                    text = "RADAR BLE ACTIVO (Foreground Service)",
                    color = Color.White,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Transmitiendo baliza anónima y escaneando RSSI continuo para filtrar proximidad < 2 metros.",
                    color = Color(0xFF90A4AE),
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
                        colors = listOf(Color(0xFFD50000).copy(alpha = alphaAnim.value), Color(0xFF210206).copy(alpha = 0.95f))
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
                    colors = ButtonDefaults.buttonColors(containerColor = Color.White)
                ) {
                    Text("ENTENDIDO (VER TABLERO)", color = Color.Red, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

private data class Quintuple<A, B, C, D, E>(
    val first: A,
    val second: B,
    val third: C,
    val fourth: D,
    val fifth: E
)
