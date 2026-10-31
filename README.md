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

## 🚀 OPCIÓN 1 (Recomendada): Instalación 100% Asistida por IA con Google Antigravity o Claude

La forma más fácil y avanzada de instalar XavierCarOS. No necesitas tocar código ni saber nada de informática:

1. Conecta tu laptop y la pantalla de tu carro a la **misma red Wi-Fi** (lo más fácil es usar la Zona Wi-Fi / Hotspot de tu celular).
2. Mira la IP de tu pantalla en **Ajustes > Wi-Fi** (ejemplo: `192.168.1.12`).
3. Abre **Google Antigravity** o **Claude** en tu computadora, dale la ruta de esta carpeta o este repositorio y copia y pega este mensaje:

```text
Quiero instalar y optimizar XavierCarOS en la pantalla Android de mi auto usando el repositorio oficial:
https://github.com/TioXavi/XavierCarOS

Tengo mi laptop con Windows y mi pantalla conectadas al mismo WiFi.
Quiero que hagas todo el trabajo técnico por mí:
1. Conéctate a la pantalla por ADB en el puerto 5555. Mi IP de pantalla es: [PEGA_TU_IP_AQUÍ].
2. Lee las instrucciones de INSTALLATION_AND_OPTIMIZATION_GUIDE.md del repositorio.
3. Audita la pantalla, aplica las optimizaciones a 60 FPS (GPU SkiaGL, animaciones 0.5x, Doze whitelist) e instala XavierCarOS como pantalla principal.
4. Si la pantalla tiene el puerto cerrado o pide contraseña de fábrica, guíame con los métodos del repositorio.
¡Comencemos!
```

El asistente de IA se encargará de todo el proceso de conexión, auditoría y despliegue por ti.

---

## 📦 OPCIÓN 2 (Alternativa): Instalación con Archivos de Doble Clic (.bat)

Si prefieres no usar una IA y hacerlo tú mismo con doble clic desde Windows:

1. **Paso 1 — Conectar tu pantalla**: Haz doble clic en 👉 **`1_CONECTAR_PANTALLA.bat`**, escribe la IP de tu pantalla y presiona `ENTER`.
2. **Paso 2 — Instalar y Optimizar a 60 FPS**: Haz doble clic en 👉 **`2_INSTALAR_XAVIERCAROS.bat`**. Aplicará las aceleraciones por GPU, instalará XavierCarOS y lo activará como launcher principal.
3. **Paso 3 — ¿Volver al estado de fábrica?**: Haz doble clic en 👉 **`3_REVERTIR_TODO.bat`**. 100% reversible y seguro.

> 📖 **Para la guía paso a paso completa, solución a pantallas bloqueadas y menú secreto de fábrica:**  
> Consulta la **[Guía Definitiva de Instalación y Optimización](./INSTALLATION_AND_OPTIMIZATION_GUIDE.md)**.

---

## 🎨 6 Temas de Lujo Automotriz (Selector Táctil Rápido)

XavierCarOS incorpora un motor de temas dinámico de grado OEM. Mantén presionado durante 1 segundo el **icono circular del auto** en la tarjeta de vehículo para elegir entre:
- ⚡ **Cyberpunk Original (Cyan Neón):** El contraste deportivo clásico con acentos azul cielo y cyan eléctrico.
- 🖤 **Negro OLED:** Negros puros absolutos (`#000000`) para pantallas OLED/QLED con ahorro de energía y contraste ámbar de carreras.
- ⚙️ **Gris Titanio:** Look industrial sobrio de cabina de carreras en grafito cepillado.
- 🤍 **Blanco Platino:** Modo diurno de alta visibilidad para días de sol radiante con texto de alto contraste.
- 🌸 **Rosa Magenta:** Estilo moderno y audaz con acentos magenta vibrantes.
- 🔮 **Morado Neón:** Estilo deep violet futurista con resplandor púrpura.

---

## 🗺️ Mapa Satelital Integrado (100% Autónomo) y Redirección Inteligente

### ❓ ¿El mapa funciona si no tengo instalado Waze, Google Maps ni Organic Maps?
**¡SÍ, 100%!**
- El mapa central de XavierCarOS **no depende de ninguna aplicación externa ni de los servicios de Google Play**.
- Incorpora un **motor cartográfico nativo (Osmdroid + OpenStreetMap)** con aceleración por GPU.
- Muestra en vivo las calles, el vehículo en movimiento y la orientación real por satélite en tiempo real (*Course-Up*).
- Admite mapas locales descargados para funcionar de forma **100% offline sin datos ni conexión a internet**.

### 🚀 Redirección Automática si una App de Navegación no está Instalada
El dock de navegación incluye accesos directos a **Maps**, **Waze**, **Organic Maps**, **Casa** y **Trabajo**.
- Si el conductor presiona cualquiera de estos botones y la aplicación **no está instalada** en la radio del auto:
  1. El asistente de voz del vehículo anuncia: *"Waze / Google Maps / Organic Maps no está instalado en la pantalla. Abriendo instalador."*
  2. Muestra un aviso en pantalla y **abre automáticamente la tienda de aplicaciones o el navegador web con el enlace de descarga oficial**.
  3. Si presionas "Casa" o "Trabajo" y no tienes Waze pero sí tienes Google Maps, el sistema conmutará automáticamente a Google Maps para iniciar la ruta sin interrupciones.

### 📥 Enlaces Oficiales de Navegadores Sugeridos:
- ⭐ **Organic Maps (100% Recomendado para radios chinas sin Play Store):** [Sitio Oficial y Descarga](https://organicmaps.app) · [Releases APK en GitHub](https://github.com/organicmaps/organicmaps/releases) · [F-Droid](https://f-droid.org/packages/app.organicmaps/)
- 🚗 **Waze (Tráfico en vivo y policía):** [Google Play](https://play.google.com/store/apps/details?id=com.waze) · [APKPure APK](https://apkpure.com/waze-gps-maps-traffic-alerts-live-navigation/com.waze)
- 📍 **Google Maps:** [Google Play](https://play.google.com/store/apps/details?id=com.google.android.apps.maps)

---

## ☀️ Clima Inteligente (Zero Config — Listo para Usar)

El módulo de clima de XavierCarOS viene **100% preconfigurado de fábrica**:
- **API Key Pública Integrada**: Incluye una clave de OpenWeatherMap lista para usar desde el primer segundo.
- **Respaldo Automático Open-Meteo (Sin Clave)**: Si la clave principal se satura o agota, el sistema conmuta automáticamente a Open-Meteo (100% abierto y público a nivel mundial).
- **Caché 100% Offline**: Si no hay conexión Wi-Fi/datos, carga al instante el último reporte guardado en memoria DataStore sin tirones ni bloqueos (0ms de retraso).
- **No requiere registro ni configuración manual** por parte del usuario final.

---

## 🎵 Reproductor Universal Híbrido: Local (USB/FLAC) + Streaming Online (Spotify, YouTube Music, etc.)

XavierCarOS integra un motor multimedia universal de grado automotriz que unifica la música local y las principales aplicaciones de streaming online:

### 🌐 1. Compatibilidad 100% con Reproductores Online
- ✅ **Soporte Universal**: Compatible con **Spotify**, **YouTube Music**, **Apple Music**, **Deezer**, **Tidal**, **Amazon Music**, **SoundCloud**, **VLC**, **Poweramp** y audio por Bluetooth.
- ✅ **Conmutación Inteligente**: Si estás escuchando música de un USB y abres Spotify o YouTube Music en la pantalla o en tu celular, XavierCarOS detecta la sesión activa, **pausa automáticamente la música local** para que no se superpongan los audios y muestra el badge del reproductor en uso (ej: 🟢 *Spotify*, 🔴 *YouTube Music*).
- ✅ **Control Total Bidireccional**: Play, Pausa, Siguiente, Anterior y Seeker interactivo que controlan directamente la app de streaming activa.
- ✅ **Controles al Volante (Timón) y Voz**: Los botones físicos de tu timón y los comandos de voz de la asistente local controlan fluidamente Spotify y YouTube Music.

### 🎤 2. Letras Sincronizadas en Vivo (Karaoke) con Descarga Automática
- ✅ **Búsqueda y Descarga Automática de Letras**: Si estás escuchando una canción en Spotify, YouTube Music o USB y no tienes el archivo `.lrc`, el sistema consulta en segundo plano la base de datos libre y abierta **LRCLIB**, descarga las letras con marcas de tiempo milimétricas `[mm:ss.xx]` y las almacena automáticamente en la memoria del vehículo (`/sdcard/Music/Lyrics/`).
- ✅ **Iluminación Dinámica a 250ms**: Desplazamiento y resaltado en tiempo real al ritmo de la voz del cantante con refresco 4 veces por segundo (0ms de retraso).
- ✅ **Salto Táctil por Versos (Tap-to-Seek)**: Toca cualquier estrofa o línea de la letra en pantalla para adelantar o retroceder la canción en Spotify / YouTube Music exactamente a ese segundo.
- ✅ **Traducción Bilingüe Instantánea**: Botón de traducción para visualizar en vivo las letras en español de cualquier canción en inglés.

### 📁 3. Reproductor Local Nativo de Alto Rendimiento (`XavierLocalPlayer`)
- ✅ **Sin dependencias externas**: No necesitas ninguna app de terceros para reproducir tus pendrives USB.
- ✅ **Audio Hi-Res sin pérdidas**: Reproducción nativa bit-perfect de FLAC (24-bit / 16-bit), WAV y MP3 a 320 kbps.
- ✅ **Arranque automático al encender el auto (Ignition Auto-Play)**: Tu música reanuda sola en 1.5s al girar la llave (ACC ON).
- ✅ **Explorador por 4 Vistas**: Lista completa, Artistas, Carpetas y Favoritos.

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

## 🗺️ Roadmap de Versiones

| Versión | Feature | Estado |
|---|---|---|
| **v1.0** | Launcher completo, GPS satelital, Reproductor nativo USB, Clima dual, CANBus, BT | ✅ Completado |
| **v1.2** | Modo noche automático por sunset/sunrise GPS (Algoritmo NOAA en tiempo real) | ✅ Completado |
| **v1.3** | ACC OFF deep sleep — desconexión de hardware GPS y CPU powersave (cero consumo de batería) | ✅ Completado |
| **v2.0** | Control por voz local manos libres (Offline Voice Assistant + TTS colombiano) | ✅ Completado |
| **v2.2** | Selector dinámico de 6 temas de lujo (Azul Cyber, Negro OLED, Gris Titanio, Blanco Platino, Rosa Cyberpunk, Morado Nebulosa) con activación por pulsación larga | ✅ Completado |
| **v2.5** | OBD2 ELM327 — velocímetro directo desde ECU, RPM, temperatura del motor y fallas | 🔜 Próximamente (Fase 2) |

---

## ☕ Apoya el Proyecto

Si disfrutas de XavierCarOS en tu vehículo y quieres apoyar el desarrollo de nuevas funciones y optimizaciones automotrices:

[![Support Xavy Flores](https://img.shields.io/badge/Ko--fi-Support%20Xavy%20Flores-FF5E5B?style=for-the-badge&logo=ko-fi&logoColor=white)](https://ko-fi.com/xavyflores)

**[👉 Invítame un café en Ko-fi (Support Xavy Flores) 👈](https://ko-fi.com/xavyflores)**

---

*XavierCarOS — Hecho con ❤️ para el Corolla Azul HAR-6843*

