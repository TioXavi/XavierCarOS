# 🚗 XavierCarOS — Android Car Launcher

> **Toyota Corolla Azul (HAR-6843) · Head Unit Edition**  
> Kotlin + Jetpack Compose · Ultra-low latency · OEM-grade UI

<div align="center">

[![Ko-fi](https://img.shields.io/badge/Ko--fi-Support%20Xavy%20Flores-FF5E5B?style=for-the-badge&logo=ko-fi&logoColor=white)](https://ko-fi.com/xavyflores)
[![Guide](https://img.shields.io/badge/Manual-Guía%20de%20Instalación%20y%20Optimización-blue?style=for-the-badge)](./INSTALLATION_AND_OPTIMIZATION_GUIDE.md)

**¿Te gusta el proyecto? [Invita un café a Xavy Flores](https://ko-fi.com/xavyflores)** ☕
</div>

---

## 📖 Guía Completa de Instalación y Optimización 100% Garantizada
👉 **Consulta [INSTALLATION_AND_OPTIMIZATION_GUIDE.md](./INSTALLATION_AND_OPTIMIZATION_GUIDE.md)** para la auditoría paso a paso, diagnóstico interactivo por preguntas y optimización a bajo nivel de cualquier pantalla Android automotriz.

---

## ⚡ Quick Start (5 minutos)

### 1. Compilar el APK

```bash
cd XavierCarOS2

# Asegúrate de tener JAVA_HOME y ANDROID_HOME configurados
./gradlew assembleDebug
```

El APK queda en: `app/build/outputs/apk/debug/app-debug.apk`

### 2. Conectar a la Head Unit por ADB Wi-Fi

```bash
# Conectar (usa la IP de tu pantalla)
adb connect 192.168.1.55:5555

# Verificar
adb devices
```

### 3. Instalar y configurar

```bash
# Windows
.\scripts\optimize_headunit.ps1 192.168.1.55

# Linux / Mac
chmod +x scripts/optimize_headunit.sh
./scripts/optimize_headunit.sh 192.168.1.55
```

---

## ☀️ Clima Inteligente (Zero Config — Listo para Usar)

El módulo de clima de XavierCarOS viene **100% preconfigurado de fábrica**:
- **API Key Pública Integrada**: Incluye una clave de OpenWeatherMap lista para usar desde el primer segundo.
- **Respaldo Automático Open-Meteo (Sin Clave)**: Si la clave principal se satura o agota, el sistema conmuta automáticamente a Open-Meteo (100% abierto y público a nivel mundial).
- **Caché 100% Offline**: Si no hay conexión Wi-Fi/datos, carga al instante el último reporte guardado en memoria DataStore sin tirones ni bloqueos (0ms de retraso).
- **No requiere registro ni configuración manual** por parte del usuario final.

---

## 🎵 Reproductor Multimedia Nativo (`XavierLocalPlayer`)

XavierCarOS **incluye su propio reproductor de música integrado de alto rendimiento**, desarrollado en Kotlin con MediaPlayer de Android:

- ✅ **Sin dependencias externas**: No necesitas instalar Poweramp, Spotify ni ninguna otra app para escuchar música.
- ✅ **Escaneo automático de USB**: Detecta pendrives y memorias USB conectadas al vehículo de forma automática.
- ✅ **Soporte Hi-Res Audio**: Reproduce archivos FLAC (24-bit / 16-bit), MP3 y WAV con decodificación directa.
- ✅ **Arranque automático (Ignition Auto-Play)**: Comienza a reproducir tu música automáticamente al dar contacto al auto (ACC ON).
- ✅ **Letras sincronizadas en vivo (`.lrc`)**: Muestra las letras en formato karaoke con iluminación activa verso a verso.
- ✅ **Traducción de letras en vivo**: Traduce las canciones en tiempo real del inglés al español con un solo toque.
- ✅ **Explorador por categorías**: Navegación por Lista completa, Artistas, Carpetas y Favoritos.
- 💡 *Soporte opcional para reproductores externos*: Si algún usuario avanzado prefiere usar reproductores de terceros como Poweramp, el launcher también puede capturar sus metadatos vía Notification Listener.

---

## 🚗 Descubrimiento de CANBus (Puertas)

```bash
# Ejecutar en Windows
.\scripts\canbus_discovery.sh

# Luego abre cada puerta física del auto
# El log mostrará el Intent exacto → copiar la línea y pasarla al CanbusDoorReceiver.kt
```

---

## 📦 Estructura del Proyecto

```
app/src/main/kotlin/com/xavier/carlauncher/
├── MainActivity.kt                    ← Entry point + Steering Wheel Controls
├── SpeedometerLocationManager.kt      ← GPS mph + Kalman filter
├── media/
│   ├── XavierLocalPlayer.kt           ← Reproductor nativo USB/FLAC/MP3 + auto-play
│   ├── LyricsRepository.kt            ← Parser de letras .lrc sincronizadas
│   └── LyricsTranslationManager.kt    ← Traducción en vivo de letras (EN -> ES)
├── bluetooth/
│   └── BluetoothStateReceiver.kt      ← A2DP + HFP Bluetooth state
├── canbus/
│   └── CanbusDoorReceiver.kt          ← Multi-platform door events
├── weather/
│   └── WeatherRepository.kt           ← Clima dual OWM + Open-Meteo + DataStore cache
├── services/
│   └── AccOffService.kt               ← ACC OFF power management
└── ui/
    ├── theme/Theme.kt
    ├── screens/
    │   ├── DashboardScreen.kt         ← Main layout (3-column grid)
    │   └── ExpandedMusicScreen.kt     ← Estudio de música y letras karaoke a pantalla completa
    └── components/
        ├── StatusBar.kt
        ├── NavigationDock.kt
        ├── VehicleCard.kt             ← Corolla + door overlay animations
        ├── SpeedometerCard.kt         ← Canvas arc speedometer
        ├── MusicPlayerCard.kt         ← Tarjeta de música nativa + letras sincronizadas
        ├── NavigationMapCard.kt       ← OSMDroid dark map
        ├── PhoneCard.kt
        └── BluetoothCard.kt
```

---

## 🔧 Permisos ADB Post-Instalación

```bash
PKG=com.xavier.carlauncher
IP=192.168.1.55

adb connect $IP:5555

# Ubicación para velocímetro y mapa
adb shell pm grant $PKG android.permission.ACCESS_FINE_LOCATION
adb shell pm grant $PKG android.permission.ACCESS_COARSE_LOCATION

# Bluetooth
adb shell pm grant $PKG android.permission.BLUETOOTH_CONNECT

# Notification Listener para Poweramp
adb shell cmd notification allow_listener $PKG/$PKG.media.PowerampNotificationListener

# Establecer como HOME launcher
adb shell cmd package set-home-activity $PKG/.MainActivity

# Whitelist en Doze (no matar en suspensión)
adb shell cmd deviceidle whitelist +$PKG
adb shell cmd deviceidle whitelist +com.maxmpz.audioplayer
adb shell cmd deviceidle whitelist +com.waze
```

---

## 🎵 Formato de Letras Sincronizadas (.lrc)

Para ver las letras en vivo en el reproductor nativo o en pantalla completa, basta con colocar el archivo `.lrc` junto al archivo de música en tu memoria USB o disco con el mismo nombre:  
`Musica/TuCancion.mp3` ➔ `Musica/TuCancion.lrc`

---

## 📡 CANBus — Plataformas Soportadas

| Plataforma | Intent | Extras |
|---|---|---|
| SYU / FYT | `com.syu.ms.canbus.DOOR_STATE` | `door_fl`, `door_fr`, `door_rl`, `door_rr` (Int) |
| SYU v2 | `com.syu.ms.canbus.door` | Mismos extras |
| Microntek / MTC | `com.microntek.canbusdisplay` | `door_status` (byte array, bits 0-4) |
| Topway | `com.tw.canbus` | `fl`, `fr`, `rl`, `rr` (Boolean) |
| MTC v2 | `microntek.intent.action.CAR_DOOR` | Mismos que MTC |

---

## 🗺️ Roadmap

| Versión | Feature |
|---|---|
| ✅ v1.0 | Launcher completo, GPS, Poweramp, Clima, CANBus, BT |
| 🔜 v1.1 | OBD2 ELM327 — velocímetro real desde ECU, RPM, temp motor |
| 🔜 v1.2 | Modo noche automático por sunset/sunrise GPS |
| 🔜 v1.3 | ACC OFF deep sleep — no consume batería sin motor |
| 🔜 v2.0 | Control por voz local (Vosk offline TTS) |

---

## ☕ Apoya el Proyecto

Si disfrutas de XavierCarOS en tu vehículo y quieres apoyar el desarrollo de nuevas funciones y optimizaciones automotrices:

[![Support Xavy Flores](https://img.shields.io/badge/Ko--fi-Support%20Xavy%20Flores-FF5E5B?style=for-the-badge&logo=ko-fi&logoColor=white)](https://ko-fi.com/xavyflores)

**[👉 Invítame un café en Ko-fi (Support Xavy Flores) 👈](https://ko-fi.com/xavyflores)**

---

*XavierCarOS — Hecho con ❤️ para el Corolla Azul HAR-6843*

