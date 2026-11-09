# Front-End Móvil: Simulación Viral por Cercanía 📱🦠

Aplicación móvil nativa en **Android (Jetpack Compose & Kotlin)** para el **Juego de Simulación Viral por Cercanía en Campus Universitario**.

La aplicación actúa como un **cliente reactivo de telemetría y renderizado en tiempo real**, utilizando el hardware **Bluetooth Low Energy (BLE)** del dispositivo para rastrear la proximidad física entre estudiantes (< 2 metros por más de 15 segundos) y visualizando la curva de propagación del modelo logístico:

$$P(t) = \frac{k}{A \cdot e^{-rt} + 1}$$

calculada por el backend centralizado en Spring Boot.

---

## 🚀 Características Principales

* **Tablero de Predicción en Vivo (Vico Charts):** Traza el plano cartesiano continuo de la función de crecimiento logístico $P(t)$ en tiempo real a partir de los eventos `BOARD_UPDATE` recibidos cada 5 segundos.
* **Hardware BLE Dual Simultáneo:**
  * **Peripheral (Emisor):** Transmite balizas anónimas (`AdvertiseData`) con el identificador único ofuscado del jugador.
  * **Central (Escáner):** Escaneo pasivo continuo en segundo plano filtrando balizas mediante intensidad de señal (**RSSI $\ge$ -75 dBm** $\approx$ menos de 2 metros).
* **Regla de Infección por Exposición Prolongada:** Si el dispositivo detecta la presencia continua de un jugador infectado por más de 15 segundos, despacha automáticamente un `EXPOSURE_REPORT` hacia el servidor vía WebSockets.
* **Feedback Visual y Háptico de Contagio:** Ante la confirmación de infección por el backend (evento `STATE_CHANGE`), la interfaz activa una superposición pulsante en **rojo carmesí** y el motor háptico vibra con un patrón rápido simulando **taquicardia**.
* **Foreground Service de Alta Confiabilidad:** Servicio en segundo plano con notificación persistente para garantizar que el escaneo BLE no sea suspendido por el sistema operativo Android.
* **Diagnóstico de Conectividad Integrado:** Panel en pantalla que verifica la salud del backend (`GET /api/v1/sessions/health`), permitiendo configurar la IP/puerto dinámicamente con un solo toque (Presets: LAN `192.168.1.18:8080`, Emulador `10.0.2.2:8080` o `localhost:8080`).

---

## 🏛️ Arquitectura de la Aplicación (MVVM)

```
┌────────────────────────────────────────────────────────────────────────┐
│                        CAPA DE PRESENTACIÓN (UI)                      │
│                                                                        │
│    ┌──────────────────────────┐      ┌──────────────────────────────┐  │
│    │  PredictionBoardScreen   │      │    CrimsonGlitchOverlay      │  │
│    │  (Tablero / Vico Chart)  │      │  (Feedback Visual Contagio)  │  │
│    └─────────────┬────────────┘      └──────────────┬───────────────┘  │
│                  │                                  │                  │
│                  ▼                                  ▼                  │
│         ┌─────────────────────────────────────────────────────┐        │
│         │                    GameViewModel                    │        │
│         │     StateFlow<GameUiState> + HapticFeedbackManager   │        │
│         └──────────────────────────┬──────────────────────────┘        │
└────────────────────────────────────┼───────────────────────────────────┘
                                     │
┌────────────────────────────────────┼───────────────────────────────────┐
│                        CAPA DE DOMINIO Y DATOS                         │
│                                    ▼                                   │
│                       ┌─────────────────────────┐                      │
│                       │     GameRepository      │                      │
│                       └────────────┬────────────┘                      │
│                                    │                                   │
│             ┌──────────────────────┴──────────────────────┐            │
│             ▼                                             ▼            │
│  ┌───────────────────────┐                    ┌──────────────────────┐ │
│  │   RetrofitClient      │                    │ ViralWebSocketClient │ │
│  │   (API REST - HTTP)   │                    │ (STOMP sobre OkHttp) │ │
│  └───────────────────────┘                    └──────────────────────┘ │
└────────────────────────────────────────────────────────────────────────┘
                                     │
┌────────────────────────────────────┼───────────────────────────────────┐
│                      CAPA DE HARDWARE Y SERVICIOS                      │
│                                    ▼                                   │
│                       ┌─────────────────────────┐                      │
│                       │   BleForegroundService  │                      │
│                       └────────────┬────────────┘                      │
│                                    │                                   │
│             ┌──────────────────────┴──────────────────────┐            │
│             ▼                                             ▼            │
│  ┌───────────────────────┐                    ┌──────────────────────┐ │
│  │  BleScannerManager    │                    │ BleAdvertiserManager │ │
│  │  (Central: <2m, >15s) │                    │ (Peripheral: Beacon) │ │
│  └───────────────────────┘                    └──────────────────────┘ │
└────────────────────────────────────────────────────────────────────────┘
```

---

## 🛠️ Stack Tecnológico

* **Lenguaje:** Kotlin 2.0+ (Coroutines, `StateFlow`, `SharedFlow`).
* **UI Toolkit:** Jetpack Compose (Material Design 3).
* **Gráficos Matemáticos:** [Vico](https://github.com/patrykandpatrick/vico) (Librería especializada en gráficas para Compose).
* **Red y Comunicaciones:**
  * **Retrofit 2** + **Gson:** Para la API RESTful sincrónica.
  * **OkHttp 4:** Para la conexión bidireccional WebSocket y parser del protocolo STOMP.
* **Hardware & BLE:** Android Bluetooth Low Energy (`BluetoothLeScanner`, `BluetoothLeAdvertiser`).
* **Notificaciones Push:** Firebase Cloud Messaging (FCM).
* **Servicios de Sistema:** Android `Vibrator` / `VibratorManager` para retroalimentación háptica.
* **Build System:** Gradle con Kotlin DSL (`build.gradle.kts`), Android Gradle Plugin 8.7+ / 9.0+, `compileSdk 37`, Java 21.

---

## 📡 Integración con el Backend

La aplicación se comunica con el servidor central [JuegoVirus_Bck](https://github.com/ivmgch/JuegoVirus_Bck.git):

### 1. Inicialización (REST)
* `GET /api/v1/sessions/health`: Verificación de salud y conectividad.
* `POST /api/v1/sessions`: Creación de partida con parámetros $P(0)$.
* `POST /api/v1/sessions/{id}/join`: Registro de la MAC/UUID del dispositivo y recepción del rol (`HEALTHY` o `PATIENT_ZERO`).

### 2. Tiempo Real (WebSockets STOMP)
* **Envío de Heartbeat (cada 30 s):** A `/app/telemetry/heartbeat` para mantener viva la presencia en $k$ y reportar el conteo de nodos cercanos.
* **Reporte de Contagio:** A `/app/exposure/report` ante contacto continuado.
* **Recepción del Tablero (cada 5 s):** Suscripción a `/topic/board/{sessionId}` para actualizar $k$, $r$, $A$, infectados y trazar la curva en Vico.
* **Recepción de Infección (Unicast):** Suscripción a `/user/queue/state` para activar la taquicardia háptica y el tinte carmesí.

---

## 📋 Permisos de Hardware Requeridos

En `AndroidManifest.xml` se gestionan los permisos para Android 12 (API 31) en adelante:
* `BLUETOOTH_SCAN`: Escaneo pasivo de balizas cercanas.
* `BLUETOOTH_ADVERTISE`: Emisión de la baliza del jugador.
* `BLUETOOTH_CONNECT`: Acceso a la interfaz de Bluetooth del sistema.
* `ACCESS_FINE_LOCATION` / `ACCESS_COARSE_LOCATION`: Requerido por el sistema para operaciones BLE.
* `FOREGROUND_SERVICE` y `FOREGROUND_SERVICE_CONNECTED_DEVICE`: Ejecución continua del servicio de proximidad.
* `VIBRATE`: Retroalimentación háptica de taquicardia.
* `POST_NOTIFICATIONS`: Alertas del sistema y notificación del Foreground Service.

---

## 📱 Guía de Instalación y Ejecución

### Prerrequisitos:
* **Android Studio** (versión Ladybug / Otter o superior).
* **JDK 21** configurado en Gradle.
* **Dispositivo Android** con Android 8.0+ (API 26+) y soporte para BLE (o Android Emulator con API 34+).

### 1. Clonar el Repositorio
```bash
git clone https://github.com/ivmgch/JuegoVirus_Front.git
cd JuegoVirus_Front
```

### 2. Conexión de Red con el Backend
Asegúrate de que el backend ([JuegoVirus_Bck](https://github.com/ivmgch/JuegoVirus_Bck.git)) esté corriendo en el puerto `8080`.

* **En Emulador Android:**
  Ejecuta en tu terminal para habilitar el reenvío de puertos:
  ```bash
  adb reverse tcp:8080 tcp:8080
  ```
  La app conectará automáticamente a través de la IP local de tu PC (`192.168.1.18:8080`) o `localhost:8080`.

* **En Dispositivo Físico:**
  Conecta el teléfono a la misma red Wi-Fi que el PC anfitrión e introduce la dirección IP en el botón *"Configurar IP"* de la pantalla de bienvenida.

### 3. Compilar e Instalar
Desde Android Studio: Presiona **Run 'app'** (`Shift + F10`).

O desde la terminal con Gradle:
```bash
./gradlew installDebug
```

---

## 🎮 Flujo de Juego en la Pantalla

1. **Estado Inicial:** La barra superior comprueba la conexión REST con el backend.
2. **Crear o Unirse:**
   * **Host:** Toca *"CREAR Y EMPEZAR (HOST)"* para inicializar la partida (`CAMPUS-CENTRAL`, $P(0)=2$).
   * **Jugador:** Ingresa el ID de la partida y presiona *"UNIRSE A PARTIDA"*.
3. **Tablero Activo:**
   * El chip superior cambia a `🟢 BROKER EN LÍNEA`.
   * Se inicia el servicio de rastreo BLE en segundo plano.
   * La gráfica de Vico renderiza en tiempo real la función logística $P(t)$.
4. **Evento de Contagio:**
   * Si permaneces cerca de un vector portador por más de 15 segundos, la pantalla se tiñe de **rojo carmesí**, el teléfono vibra imitando palpitaciones cardíacas aceleradas y tu estado cambia a **INFECTADO (CONTAGIOSO)**.

---

## 🔗 Repositorio del Backend
El código fuente del servidor Spring Boot está disponible en:
👉 [JuegoVirus_Bck (GitHub)](https://github.com/ivmgch/JuegoVirus_Bck.git)

---

## 📄 Autores y Licencia
Desarrollado para la investigación y simulación de modelos epidemiológicos aplicados en campus universitarios utilizando tecnología móvil y proximidad BLE.
* **Autor:** ivmgch (Universidad de Cundinamarca)
