package com.example.virus.data.websocket

import android.util.Log
import com.example.virus.data.api.RetrofitClient
import com.example.virus.model.dto.BoardUpdateDto
import com.example.virus.model.dto.ExposureReportPayload
import com.example.virus.model.dto.HeartbeatPayload
import com.example.virus.model.dto.StateChangeDto
import com.google.gson.Gson
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import okhttp3.Request
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener

class ViralWebSocketClient(
    private val gson: Gson = Gson(),
    private val scope: CoroutineScope = CoroutineScope(Dispatchers.IO)
) {
    private val tag = "ViralWebSocket"
    private var webSocket: WebSocket? = null

    private val _isConnected = MutableStateFlow(false)
    val isConnected: StateFlow<Boolean> = _isConnected.asStateFlow()

    private val _boardUpdates = MutableSharedFlow<BoardUpdateDto>(replay = 1)
    val boardUpdates: SharedFlow<BoardUpdateDto> = _boardUpdates.asSharedFlow()

    private val _stateChanges = MutableSharedFlow<StateChangeDto>(replay = 1)
    val stateChanges: SharedFlow<StateChangeDto> = _stateChanges.asSharedFlow()

    private var activeSessionId: Long? = null
    private var activePlayerUuid: String? = null

    fun connect(sessionId: Long, playerUuid: String) {
        this.activeSessionId = sessionId
        this.activePlayerUuid = playerUuid

        val url = com.example.virus.data.api.ServerConfigManager.getWebSocketUrl()
        Log.i(tag, "Iniciando conexión WebSocket a $url...")

        val request = Request.Builder().url(url).build()
        webSocket = RetrofitClient.okHttpClient.newWebSocket(request, object : WebSocketListener() {
            override fun onOpen(webSocket: WebSocket, response: Response) {
                Log.i(tag, "Conexión TCP abierta. Negociando STOMP...")
                // 1. Enviar frame CONNECT
                val connectFrame = StompMessage(
                    command = "CONNECT",
                    headers = mapOf(
                        "accept-version" to "1.1,1.2",
                        "heart-beat" to "10000,10000"
                    )
                )
                webSocket.send(connectFrame.toFrame())
            }

            override fun onMessage(webSocket: WebSocket, text: String) {
                val stomp = StompMessage.parse(text) ?: return

                when (stomp.command) {
                    "CONNECTED" -> {
                        Log.i(tag, "STOMP conectado exitosamente con el Broker Spring Boot.")
                        _isConnected.value = true
                        subscribeToChannels(sessionId)
                    }
                    "MESSAGE" -> {
                        val destination = stomp.headers["destination"].orEmpty()
                        handleIncomingMessage(destination, stomp.payload)
                    }
                    "ERROR" -> {
                        Log.e(tag, "Error recibido del broker STOMP: ${stomp.payload}")
                    }
                }
            }

            override fun onClosing(webSocket: WebSocket, code: Int, reason: String) {
                _isConnected.value = false
                Log.w(tag, "Cerrando WebSocket: $code / $reason")
            }

            override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                _isConnected.value = false
                Log.e(tag, "Fallo de conexión WebSocket: ${t.message}", t)
            }
        })
    }

    private fun subscribeToChannels(sessionId: Long) {
        // Suscripción Broadcast: Tablero general de la partida
        val boardSub = StompMessage(
            command = "SUBSCRIBE",
            headers = mapOf(
                "id" to "sub-board-$sessionId",
                "destination" to "/topic/board/$sessionId"
            )
        )
        webSocket?.send(boardSub.toFrame())

        // Suscripción Unicast: Notificaciones dirigidas al usuario (infección)
        val stateSub = StompMessage(
            command = "SUBSCRIBE",
            headers = mapOf(
                "id" to "sub-state-$sessionId",
                "destination" to "/user/queue/state"
            )
        )
        webSocket?.send(stateSub.toFrame())

        Log.i(tag, "Suscripciones activadas: /topic/board/$sessionId y /user/queue/state")
    }

    private fun handleIncomingMessage(destination: String, payload: String) {
        scope.launch {
            try {
                if (destination.contains("/topic/board/")) {
                    val boardUpdate = gson.fromJson(payload, BoardUpdateDto::class.java)
                    _boardUpdates.emit(boardUpdate)
                } else if (destination.contains("queue/state")) {
                    val stateChange = gson.fromJson(payload, StateChangeDto::class.java)
                    _stateChanges.emit(stateChange)
                }
            } catch (e: Exception) {
                Log.e(tag, "Error al parsear payload desde $destination: ${e.message}")
            }
        }
    }

    fun sendHeartbeat(sessionId: Long, playerUuid: String, bleNearbyCount: Int) {
        val payload = HeartbeatPayload(sessionId, playerUuid, bleNearbyCount)
        val json = gson.toJson(payload)
        val frame = StompMessage(
            command = "SEND",
            headers = mapOf(
                "destination" to "/app/telemetry/heartbeat",
                "content-type" to "application/json"
            ),
            payload = json
        )
        webSocket?.send(frame.toFrame())
    }

    fun sendExposureReport(report: ExposureReportPayload) {
        val json = gson.toJson(report)
        val frame = StompMessage(
            command = "SEND",
            headers = mapOf(
                "destination" to "/app/exposure/report",
                "content-type" to "application/json"
            ),
            payload = json
        )
        webSocket?.send(frame.toFrame())
        Log.i(tag, "EXPOSURE_REPORT enviado al backend para beacon ${report.infectedBeaconUuid}")
    }

    fun disconnect() {
        webSocket?.close(1000, "Desconexión normal de cliente")
        webSocket = null
        _isConnected.value = false
    }
}
