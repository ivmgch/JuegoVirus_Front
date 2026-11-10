package com.example.virus.ui.screens

import androidx.compose.foundation.BorderStroke
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.virus.ui.theme.BeigeCream
import com.example.virus.ui.theme.BeigeSurface
import com.example.virus.ui.theme.BeigeSurfaceAlt
import com.example.virus.ui.theme.BorderBeige
import com.example.virus.ui.theme.BorderLavender
import com.example.virus.ui.theme.PurpleDeep
import com.example.virus.ui.theme.PurplePrimary
import com.example.virus.ui.theme.StatusGreenBg
import com.example.virus.ui.theme.StatusGreenBorder
import com.example.virus.ui.theme.StatusGreenText
import com.example.virus.ui.theme.StatusRedBg
import com.example.virus.ui.theme.StatusRedBorder
import com.example.virus.ui.theme.StatusRedText
import com.example.virus.ui.theme.StatusYellowBg
import com.example.virus.ui.theme.StatusYellowBorder
import com.example.virus.ui.theme.StatusYellowText
import com.example.virus.ui.theme.TextMuted
import com.example.virus.ui.theme.TextPrimary
import com.example.virus.ui.theme.TextSecondary
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
        containerColor = BeigeSurface,
        title = {
            Text(
                text = "CONEXIÓN CON EL BACKEND",
                color = PurpleDeep,
                fontSize = 15.sp,
                fontWeight = FontWeight.Black,
                letterSpacing = 1.sp
            )
        },
        text = {
            Column {
                Text(
                    text = "Configura la dirección IP y puerto del servidor Spring Boot. Funciona con cualquier puerto, IP local, emuladores o URLs en la nube:",
                    color = TextSecondary,
                    fontSize = 12.sp,
                    lineHeight = 16.sp
                )

                Spacer(modifier = Modifier.height(14.dp))

                OutlinedTextField(
                    value = hostInput,
                    onValueChange = { hostInput = it },
                    label = { Text("IP / Host y Puerto") },
                    placeholder = { Text("ej. 192.168.1.35:8080") },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary,
                        focusedBorderColor = PurplePrimary,
                        unfocusedBorderColor = BorderLavender,
                        focusedLabelColor = PurplePrimary,
                        unfocusedLabelColor = TextSecondary,
                        focusedPlaceholderColor = TextMuted,
                        unfocusedPlaceholderColor = TextMuted
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(10.dp))

                Text(
                    text = "Presets rápidos:",
                    color = TextSecondary,
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
                            color = BeigeSurfaceAlt,
                            border = BorderStroke(1.dp, BorderBeige),
                            modifier = Modifier
                                .weight(1f)
                                .clickable {
                                    hostInput = preset
                                    viewModel.updateServerHost(preset)
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

                Spacer(modifier = Modifier.height(14.dp))

                // Estado de conectividad en vivo
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = when (isServerReachable) {
                        true -> StatusGreenBg
                        false -> StatusRedBg
                        null -> StatusYellowBg
                    },
                    border = BorderStroke(
                        1.dp,
                        when (isServerReachable) {
                            true -> StatusGreenBorder
                            false -> StatusRedBorder
                            null -> StatusYellowBorder
                        }
                    ),
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
                                true -> StatusGreenText
                                false -> StatusRedText
                                null -> StatusYellowText
                            },
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold
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
                colors = ButtonDefaults.buttonColors(containerColor = PurplePrimary)
            ) {
                Text("PROBAR Y GUARDAR", color = BeigeCream, fontWeight = FontWeight.Bold, fontSize = 11.sp)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("CERRAR", color = TextSecondary, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
            }
        }
    )
}
