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

## 🔑 Configuración Inicial Obligatoria

### API Key de Clima (OpenWeatherMap)
Edita `app/src/main/kotlin/com/xavier/carlauncher/weather/WeatherRepository.kt`:
```kotlin
const val API_KEY = "TU_API_KEY_AQUI"  // ← Reemplazar
```
Obtén tu clave gratuita en: https://openweathermap.org/api

### Assets del Corolla (imágenes)
Agrega estos PNGs a `app/src/main/res/drawable/`:
- `corolla_base.png` — auto con puertas cerradas (transparente)
- `corolla_door_fl.png` — puerta conductor abierta
- `corolla_door_fr.png` — puerta copiloto abierta
- `corolla_door_rl.png` — puerta trasera izquierda abierta
- `corolla_door_rr.png` — puerta trasera derecha abierta
- `corolla_trunk.png` — maletero abierto

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
│   └── PowerampMediaController.kt     ← Poweramp + LRC lyrics
├── bluetooth/
│   └── BluetoothStateReceiver.kt      ← A2DP + HFP Bluetooth state
├── canbus/
│   └── CanbusDoorReceiver.kt          ← Multi-platform door events
├── weather/
│   └── WeatherRepository.kt           ← OpenWeatherMap + DataStore cache
├── services/
│   └── AccOffService.kt               ← ACC OFF power management
└── ui/
    ├── theme/Theme.kt
    ├── screens/DashboardScreen.kt     ← Main layout (3-column grid)
    └── components/
        ├── StatusBar.kt
        ├── NavigationDock.kt
        ├── VehicleCard.kt             ← Corolla + door overlay animations
        ├── SpeedometerCard.kt         ← Canvas arc speedometer
        ├── MusicPlayerCard.kt         ← Poweramp + lyrics crossfade
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

## 🎵 Configurar Poweramp

En Poweramp → Ajustes → Audio:
- ✅ **Enviar eventos de audio** → Activado
- ✅ **API de estado en notificaciones** → Activado
- ✅ **Album art en notificaciones** → Activado

Para letras `.lrc`: coloca el archivo con el mismo nombre que el MP3 junto al audio.  
Ejemplo: `Musica/IM Sorry.mp3` → `Musica/IM Sorry.lrc`

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

