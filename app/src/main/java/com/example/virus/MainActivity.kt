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

                // Iniciar el Foreground Service cuando el usuario se une a una partida activa
                LaunchedEffect(uiState.playerUuid) {
                    uiState.playerUuid?.let { uuid ->
                        BleForegroundService.start(this@MainActivity, uuid)
                    }
                }

                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    PredictionBoardScreen(
                        viewModel = viewModel,
                        modifier = Modifier.padding(innerPadding)
                    )
                }
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        // Si la actividad se destruye, puede detenerse o dejarse en background según la partida
    }
}
