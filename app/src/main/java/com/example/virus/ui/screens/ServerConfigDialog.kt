package com.example.virus.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.virus.ui.viewmodel.GameViewModel

@Composable
fun ServerConfigDialog(
    viewModel: GameViewModel,
    serverHost: String,
    isServerReachable: Boolean?,
    latencyMs: Long?,
    onDismiss: () -> Unit
) {
    var hostInput by remember(serverHost) { mutableStateOf(serverHost) }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Color(0xFF141C28),
        title = {
            Text(
                text = "CONEXIÓN CON EL BACKEND",
                color = Color.White,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp
            )
        },
        text = {
            Column {
                Text(
                    text = "Configura la dirección IP y puerto del servidor Spring Boot. Funciona con cualquier puerto, IP local, emuladores o URLs en la nube:",
                    color = Color(0xFFB0BEC5),
                    fontSize = 12.sp,
                    lineHeight = 16.sp
                )

                Spacer(modifier = Modifier.height(14.dp))

                OutlinedTextField(
                    value = hostInput,
                    onValueChange = { hostInput = it },
                    label = { Text("IP / Host y Puerto", color = Color(0xFF90A4AE)) },
                    placeholder = { Text("ej. 192.168.1.35:8080", color = Color(0xFF546E7A)) },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedBorderColor = Color(0xFF00E5FF),
                        unfocusedBorderColor = Color(0xFF37474F)
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(10.dp))

                Text(
                    text = "Presets rápidos:",
                    color = Color(0xFF78909C),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium
                )

                Spacer(modifier = Modifier.height(6.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    listOf("192.168.1.18:8080", "10.0.2.2:8080", "localhost:8080").forEach { preset ->
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = Color(0xFF1F2B3D),
                            modifier = Modifier
                                .weight(1f)
                                .clickable {
                                    hostInput = preset
                                    viewModel.updateServerHost(preset)
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

                Spacer(modifier = Modifier.height(14.dp))

                // Estado de conectividad en vivo
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = when (isServerReachable) {
                        true -> Color(0xFF132F20)
                        false -> Color(0xFF381216)
                        null -> Color(0xFF2B2513)
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = when (isServerReachable) {
                                true -> "🟢 Conectado con éxito (${latencyMs ?: 0} ms) • Supabase / Spring"
                                false -> "🔴 Sin conexión al servidor (Verifica IP/puerto)"
                                null -> "⏳ Probando conexión..."
                            },
                            color = when (isServerReachable) {
                                true -> Color(0xFF69F0AE)
                                false -> Color(0xFFFF5252)
                                null -> Color(0xFFFFD54F)
                            },
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    viewModel.updateServerHost(hostInput)
                },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00E5FF))
            ) {
                Text("PROBAR Y GUARDAR", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 11.sp)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("CERRAR", color = Color(0xFF90A4AE), fontSize = 11.sp)
            }
        }
    )
}
