package com.example.virus.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.material.icons.automirrored.filled.BluetoothSearching
import androidx.compose.material.icons.filled.Coronavirus
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Vibration
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.virus.R
import com.example.virus.ui.viewmodel.AppScreen
import com.example.virus.ui.viewmodel.GameViewModel

@Composable
fun WelcomeScreen(
    viewModel: GameViewModel,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF0F141C))
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(20.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // 1. Barra Superior con Estado del Servidor y Configuración
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = when (uiState.isServerReachable) {
                        true -> Color(0xFF132F20)
                        false -> Color(0xFF3E1218)
                        null -> Color(0xFF262413)
                    },
                    modifier = Modifier.clickable { viewModel.toggleServerConfigDialog(true) }
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(
                                    when (uiState.isServerReachable) {
                                        true -> Color(0xFF00E676)
                                        false -> Color(0xFFFF5252)
                                        null -> Color(0xFFFFD54F)
                                    }
                                )
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = when (uiState.isServerReachable) {
                                true -> "SERVIDOR OK (${uiState.latencyMs ?: 0}ms)"
                                false -> "SIN CONEXIÓN"
                                null -> "VERIFICANDO..."
                            },
                            color = Color.White,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = Color(0xFF1A2332),
                    modifier = Modifier.clickable { viewModel.toggleServerConfigDialog(true) }
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = "Configurar",
                            tint = Color(0xFF00E5FF),
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "IP / PUERTO",
                            color = Color(0xFF80D8FF),
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // 2. Icono Hero Weezing con Aura de Radar
            Box(
                modifier = Modifier
                    .size(120.dp)
                    .clip(CircleShape)
                    .background(
                        Brush.radialGradient(
                            listOf(Color(0xFF1F2F45), Color(0xFF0F141C))
                        )
                    )
                    .border(2.dp, Color(0x5500E5FF), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Image(
                    painter = painterResource(id = R.drawable.ic_launcher_foreground),
                    contentDescription = "Logo Virus",
                    modifier = Modifier.size(105.dp)
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = "SIMULACIÓN VIRAL",
                color = Color.White,
                fontSize = 22.sp,
                fontWeight = FontWeight.Black,
                letterSpacing = 2.sp,
                textAlign = TextAlign.Center
            )

            Text(
                text = "Campus Universitario • Supervivencia BLE",
                color = Color(0xFF00E5FF),
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                letterSpacing = 1.sp,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(20.dp))

            // 3. Tarjeta: ¿Qué es el Juego?
            InfoCard(
                icon = Icons.Default.Coronavirus,
                accentColor = Color(0xFFFF5252),
                title = "¿QUÉ ES LA SIMULACIÓN?",
                description = "Un juego de supervivencia multijugador en tiempo real. Utiliza la conectividad física de los teléfonos móviles para simular la propagación de una epidemia virtual en el campus.\n\nEl servidor actúa como fuente de la verdad resolviendo continuamente la ecuación diferencial logística de Verhulst: P(t) = k / (A·e^(-rt) + 1)."
            )

            Spacer(modifier = Modifier.height(12.dp))

            // 4. Tarjeta: Mecánica de Contagio BLE
            InfoCard(
                icon = Icons.AutoMirrored.Filled.BluetoothSearching,
                accentColor = Color(0xFF00E5FF),
                title = "¿CÓMO FUNCIONA EL CONTAGIO?",
                description = "• Proximidad Real: El hardware Bluetooth Low Energy (BLE) detecta jugadores cercanos midiendo la intensidad RSSI a menos de 2 metros.\n• Exposición Prolongada: Estar expuesto por más de 15 segundos continuos a un portador infectado transmite el virus.\n• Telemetría Dinámica: Cada 30s tu dispositivo emite Heartbeats al servidor para actualizar la población activa (k) y la tasa de contagio (r)."
            )

            Spacer(modifier = Modifier.height(12.dp))

            // 5. Tarjeta: Feedback Háptico y Visual
            InfoCard(
                icon = Icons.Default.Vibration,
                accentColor = Color(0xFFFF1744),
                title = "FEEDBACK SENSORIAL",
                description = "En caso de contagio, tu teléfono vibrará con un patrón acelerado simulando taquicardia cardíaca y la pantalla se teñirá de rojo carmesí. Tu baliza BLE comenzará a transmitir como vector activo."
            )

            Spacer(modifier = Modifier.height(12.dp))

            // 6. Tarjeta: Regla de Mínimo 3 Jugadores
            InfoCard(
                icon = Icons.Default.Groups,
                accentColor = Color(0xFF00E676),
                title = "REGLA DE SALA (LOBBY)",
                description = "Para iniciar la simulación se requiere un MÍNIMO DE 3 PERSONAS conectadas en la misma sesión.\n\nAl comenzar, el servidor designará de manera aleatoria a los Pacientes Cero (vectores iniciales) y al resto como sanos vulnerables."
            )

            Spacer(modifier = Modifier.height(24.dp))

            // 7. Botón Principal: Comenzar
            Button(
                onClick = { viewModel.navigateTo(AppScreen.LOBBY) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00E5FF))
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "ENTRAR A LA SALA DE JUEGO",
                        color = Color(0xFF0F141C),
                        fontWeight = FontWeight.Black,
                        fontSize = 14.sp,
                        letterSpacing = 1.sp
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Icon(
                        imageVector = Icons.Default.PlayArrow,
                        contentDescription = null,
                        tint = Color(0xFF0F141C),
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
private fun InfoCard(
    icon: ImageVector,
    accentColor: Color,
    title: String,
    description: String
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF151D2A))
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.Top
        ) {
            Box(
                modifier = Modifier
                    .size(38.dp)
                    .clip(CircleShape)
                    .background(accentColor.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = accentColor,
                    modifier = Modifier.size(22.dp)
                )
            }
            Spacer(modifier = Modifier.width(14.dp))
            Column {
                Text(
                    text = title,
                    color = accentColor,
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp,
                    letterSpacing = 0.8.sp
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = description,
                    color = Color(0xFFCFD8DC),
                    fontSize = 12.sp,
                    lineHeight = 18.sp
                )
            }
        }
    }
}
