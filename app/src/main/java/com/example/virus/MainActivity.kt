package com.example.virus

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.virus.service.ble.BleForegroundService
import com.example.virus.ui.screens.PredictionBoardScreen
import com.example.virus.ui.theme.BeigeBackground
import com.example.virus.ui.theme.VirusTheme
import com.example.virus.ui.viewmodel.GameViewModel

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val app = application as ViralApplication

        setContent {
            VirusTheme {
                val viewModel: GameViewModel = viewModel {
                    GameViewModel(app.gameRepository, app.hapticFeedbackManager)
                }

                val uiState by viewModel.uiState.collectAsState()

                // Gestor de permisos para Bluetooth, Localización y Notificaciones
                val permissionLauncher = rememberLauncherForActivityResult(
                    contract = ActivityResultContracts.RequestMultiplePermissions()
                ) { _ ->
                    // Permisos evaluados
                }

                LaunchedEffect(Unit) {
                    val permissionsToRequest = mutableListOf<String>()

                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                        permissionsToRequest.add(Manifest.permission.BLUETOOTH_SCAN)
                        permissionsToRequest.add(Manifest.permission.BLUETOOTH_ADVERTISE)
                        permissionsToRequest.add(Manifest.permission.BLUETOOTH_CONNECT)
                    } else {
                        permissionsToRequest.add(Manifest.permission.ACCESS_FINE_LOCATION)
                    }

                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        permissionsToRequest.add(Manifest.permission.POST_NOTIFICATIONS)
                    }

                    val notGranted = permissionsToRequest.filter {
                        ContextCompat.checkSelfPermission(this@MainActivity, it) != PackageManager.PERMISSION_GRANTED
                    }

                    if (notGranted.isNotEmpty()) {
                        permissionLauncher.launch(notGranted.toTypedArray())
                    }
                }

                // Control del ciclo de vida del Foreground Service BLE:
                // Solo permanece activo mientras el usuario esté en sala/juego activo (no en bienvenida ni partida finalizada)
                LaunchedEffect(uiState.playerUuid, uiState.currentScreen, uiState.sessionStatus) {
                    val isSessionActive = uiState.playerUuid != null &&
                            uiState.currentScreen != com.example.virus.ui.viewmodel.AppScreen.WELCOME &&
                            uiState.sessionStatus != "COMPLETED"

                    if (isSessionActive) {
                        BleForegroundService.start(this@MainActivity, uiState.playerUuid!!)
                    } else {
                        BleForegroundService.stop(this@MainActivity)
                    }
                }

                Scaffold(
                    modifier = Modifier.fillMaxSize(),
                    containerColor = BeigeBackground
                ) { innerPadding ->
                    when (uiState.currentScreen) {
                        com.example.virus.ui.viewmodel.AppScreen.WELCOME -> {
                            com.example.virus.ui.screens.WelcomeScreen(
                                viewModel = viewModel,
                                modifier = Modifier.padding(innerPadding)
                            )
                        }
                        com.example.virus.ui.viewmodel.AppScreen.LOBBY,
                        com.example.virus.ui.viewmodel.AppScreen.GAME_DASHBOARD -> {
                            PredictionBoardScreen(
                                viewModel = viewModel,
                                modifier = Modifier.padding(innerPadding)
                            )
                        }
                    }

                    if (uiState.showServerConfigDialog) {
                        com.example.virus.ui.screens.ServerConfigDialog(
                            viewModel = viewModel,
                            serverHost = uiState.serverHost,
                            isServerReachable = uiState.isServerReachable,
                            latencyMs = uiState.latencyMs,
                            onDismiss = { viewModel.toggleServerConfigDialog(false) }
                        )
                    }
                }
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        // Garantizar que la notificación y el servicio BLE se cierren completamente al salir de la app
        BleForegroundService.stop(this)
    }
}
