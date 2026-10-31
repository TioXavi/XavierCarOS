# 🚗 Guía Definitiva de Instalación y Optimización de Pantallas Android con XavierCarOS
### *Diseñada para cualquier persona (incluso sin conocimientos de programación ni informática)*

<div align="center">

[![Ko-fi](https://img.shields.io/badge/Ko--fi-Support%20Xavy%20Flores-FF5E5B?style=for-the-badge&logo=ko-fi&logoColor=white)](https://ko-fi.com/xavyflores)
[![AI Powered](https://img.shields.io/badge/Instalación-Asistida%20por%20IA%20(Antigravity%20%7C%20Claude)-9cf?style=for-the-badge&logo=google&logoColor=white)](https://github.com/TioXavi/XavierCarOS)
[![Windows](https://img.shields.io/badge/Opción%202-.bat%20Doble%20Clic-0078D6?style=for-the-badge&logo=windows&logoColor=white)](https://github.com/TioXavi/XavierCarOS)
[![FPS](https://img.shields.io/badge/Rendimiento-60%20FPS%20GPU%20SkiaGL-success?style=for-the-badge)](https://github.com/TioXavi/XavierCarOS)

### ☕ ¿Te gusta el proyecto?
> **XavierCarOS** fue desarrollado con pasión para transformar cualquier pantalla Android de carro lenta y saturada en una experiencia fluida a 60 FPS con diseño Cyberpunk OEM.
> 
> Si esta guía e instaladores le han dado una nueva vida a tu vehículo, ¡te invito a donar un café para seguir mejorando el proyecto y creando nuevas funciones!  
> 👉 **[Support Xavy Flores en Ko-fi (Invítame un café)](https://ko-fi.com/xavyflores)** ☕

</div>

---

## 🌟 Qué vas a lograr

Con esta guía puedes transformar la pantalla de inicio de tu radio Android de carro por **XavierCarOS**, dejándola mucho más rápida y fluida (a 60 cuadros por segundo), **sin necesidad de programar tú mismo**.

Tienes **dos formas de hacerlo**:
1. **🚀 OPCIÓN 1 (Recomendada): Dejar que un asistente de IA (Google Antigravity o Claude) haga todo el trabajo técnico por ti.** Solo le das el enlace de este repositorio y la IA se conecta a tu radio, audita la pantalla, optimiza el sistema a 60 FPS, instala la app y concede todos los permisos.
2. **📦 OPCIÓN 2 (Alternativa sin IA): Usar los instaladores `.bat` de doble clic.** Si prefieres hacerlo tú mismo en tu PC con Windows sin usar una IA.

---

### ✨ Características que tendrás en tu vehículo:
1. **Velocidad Extrema (60 FPS):** Optimizaciones profundas de GPU y sistema que eliminan los tirones y el molesto mensaje de *"La aplicación no responde"* (cero ANRs).
2. **Velocímetro GPS con Brújula Dinámica:** Detecta tu velocidad real por satélite y orienta el mapa dinámicamente apuntando siempre hacia el frente de la carretera por donde conduces.
3. **Reproductor Nativo con Auto-Play:** Conecta tu memoria USB y la música arrancará sola automáticamente cada vez que enciendas el carro (ACC ON), sin necesidad de Poweramp ni Spotify.
4. **Letras Karaoke en Vivo (`.lrc`) y Traducción:** Pantalla de música ampliada con letras sincronizadas que se iluminan al ritmo de la canción y traducción instantánea de inglés a español con un toque.
5. **Clima Inteligente (Zero-Config):** Actualización automática cada 10 minutos con respaldo dual (OpenWeatherMap + Open-Meteo) o modo 100% offline sin internet. No requiere crear cuentas ni ingresar claves.
6. **Saludo Personalizado:** Mantén presionado *"Hola, Xavier"* durante 1 segundo para escribir tu propio nombre o apodo.
7. **100% Seguro y Reversible:** Nada de lo que hacemos daña la radio. Siempre puedes volver al launcher de fábrica cuando quieras.
8. **Lo que NO se toca y sigue funcionando al 100%:** Tu cámara de reversa original, los controles del timón (volante), la radio FM original y las llamadas Bluetooth siguen funcionando perfectamente.

---

## 🛠️ Antes de empezar: Qué necesitas

| Elemento | Para qué sirve | Notas para novatos |
|---|---|---|
| **Laptop o PC con Windows 10 u 11** | Para ejecutar el asistente de IA o los instaladores. | Cualquier computadora portátil sirve. |
| **La pantalla Android de tu carro** | El equipo donde se instalará XavierCarOS. | Puede ser de 7, 9 o 10 pulgadas (FYT, TopWay, Microntek, TS10 o genérica). |
| **Red WiFi Compartida (Hotspot del Celular)** | Para que tu laptop y la pantalla puedan comunicarse. | **Recomendación de oro:** Activa la *"Zona Wi-Fi / Compartir Internet"* de tu celular y conecta ahí tanto la laptop como la pantalla. |
| **Asistente de IA (Antigravity o Claude)** | Hace todo el trabajo técnico de instalación por ti. | Solo necesitas copiar el enlace del repositorio y pegar el prompt maestro. |

---

# 🚀 OPCIÓN 1 (RECOMENDADA): Instalación 100% Asistida por IA con Google Antigravity o Claude

Esta es la forma más moderna, fácil y segura. No tienes que tocar código ni aprender comandos de consola. Un asistente de inteligencia artificial como **Google Antigravity** o **Claude** se encargará de todo el trabajo técnico.

### Pasos para el usuario:
1. Conecta tu laptop y la pantalla de tu carro al **Hotspot WiFi de tu teléfono celular**.
2. En la pantalla del carro, ve a **Ajustes > WiFi**, toca sobre la red y anota la dirección IP (por ejemplo: `192.168.43.15` o `192.168.1.12`).
3. Abre **Google Antigravity** o **Claude** en tu computadora.
4. Copia y pega el siguiente **Prompt Maestro** en el chat del asistente:

```text
Quiero instalar y optimizar XavierCarOS en la pantalla Android de mi auto usando el repositorio oficial:
https://github.com/TioXavi/XavierCarOS

Tengo mi laptop con Windows y mi pantalla Android conectadas a la misma red WiFi.
Quiero que tú hagas todo el trabajo técnico por mí directamente en mi equipo:
1. Conéctate a la pantalla por ADB en el puerto 5555. Mi IP de pantalla es: [PEGA_TU_IP_AQUÍ, ej: 192.168.1.12].
2. Lee las instrucciones de INSTALLATION_AND_OPTIMIZATION_GUIDE.md del repositorio.
3. Haz una auditoría de hardware (SoC, RAM real, versión de Android, resolución y plataforma FYT/TopWay/MTC).
4. Aplica las optimizaciones profundas de ultra fluidez (forzar GPU SkiaGL, animaciones a 0.5x, Doze whitelist para evitar que se cierre).
5. Compila o instala el APK de XavierCarOS, concédele todos los permisos requeridos (GPS, USB, Notificaciones) y fíjala como launcher de inicio principal.
6. Si la pantalla tiene el puerto cerrado o pide contraseña de fábrica, guíame paso a paso con los métodos del repositorio.

Reglas:
- Haz todo paso a paso y explícame en lenguaje sencillo.
- No toques partes críticas (cámara de reversa, timón ni radio FM).
- Todo debe ser seguro y reversible.
¡Comencemos!
```

5. ¡Listo! El agente de IA descargará el proyecto, verificará la conexión con la radio, aplicará las aceleraciones por GPU, instalará el launcher y te avisará cuando esté listo en la pantalla de tu carro.

---

## 🤖 Guía Técnica de Operaciones para el Agente de IA (Antigravity / Claude)

> *Esta sección está escrita para que el Asistente de IA (Antigravity o Claude) sepa con exactitud qué comandos ejecutar y cómo proceder en cada fase del despliegue en la máquina del usuario.*

### Fase A: Conexión y Verificación de Red
1. Comprueba si `adb` está disponible. Si no está en el PATH, descárgalo o usa `platform-tools\adb.exe`.
2. Ejecuta:
   ```powershell
   adb connect <IP_PANTALLA>:5555
   adb devices -l
   ```
3. Si el estado es `unauthorized`, pídele al usuario que mire la pantalla de su vehículo y acepte el diálogo: *"¿Permitir depuración USB? > Permitir siempre"*.
4. Si la conexión es rechazada (puerto 5555 cerrado), guíalo a través de la sección de [Desbloqueo de Pantallas](#-cómo-abrir-el-puerto-de-conexión-si-tu-pantalla-viene-bloqueada).

### Fase B: Auditoría de Hardware de la Pantalla
Ejecuta la radiografía del dispositivo para detectar su arquitectura y plataforma:
```powershell
adb -s <IP>:5555 shell getprop ro.board.platform
adb -s <IP>:5555 shell wm size
adb -s <IP>:5555 shell wm density
adb -s <IP>:5555 shell "pm list packages | grep -E 'syu|tw\.|microntek|ts\.'"
```
*Si la pantalla es TopWay o Microntek, el sistema utiliza automáticamente `AutomotiveIntentResolver.kt` para vincular el marcador telefónico y los ajustes del auto.*

### Fase C: Modificaciones y Optimizaciones Profundas (60 FPS)
Aplica las optimizaciones de aceleración por GPU y gestión de energía:
```powershell
# Forzar GPU y motor SkiaGL para Compose
adb -s <IP>:5555 shell "setprop debug.sf.hw 1"
adb -s <IP>:5555 shell "setprop debug.egl.hw 1"
adb -s <IP>:5555 shell "setprop debug.hwui.renderer skiagl"
adb -s <IP>:5555 shell "settings put global force_gpu_rendering 1"

# Velocidad de respuesta táctil deportiva (0.5x)
adb -s <IP>:5555 shell "settings put global window_animation_scale 0.5"
adb -s <IP>:5555 shell "settings put global transition_animation_scale 0.5"
adb -s <IP>:5555 shell "settings put global animator_duration_scale 0.5"

# Blindaje Doze (Evitar que Android cierre el launcher o duerma el GPS)
adb -s <IP>:5555 shell "dumpsys deviceidle whitelist +com.xavier.carlauncher.debug"
adb -s <IP>:5555 shell "cmd appops set com.xavier.carlauncher.debug RUN_IN_BACKGROUND allow"
```

### Fase D: Instalación e Inyección Silenciosa de Permisos
1. Si no existe el APK compilado, compílalo con `./gradlew assembleDebug`.
2. Instala el APK:
   ```powershell
   adb -s <IP>:5555 install -r -d app/build/outputs/apk/debug/app-debug.apk
   ```
3. Otorga todos los permisos del sistema por consola para que el usuario no tenga diálogos emergentes:
   ```powershell
   adb -s <IP>:5555 shell "pm grant com.xavier.carlauncher.debug android.permission.ACCESS_FINE_LOCATION"
   adb -s <IP>:5555 shell "pm grant com.xavier.carlauncher.debug android.permission.ACCESS_COARSE_LOCATION"
   adb -s <IP>:5555 shell "pm grant com.xavier.carlauncher.debug android.permission.READ_EXTERNAL_STORAGE"
   adb -s <IP>:5555 shell "pm grant com.xavier.carlauncher.debug android.permission.WRITE_EXTERNAL_STORAGE"
   adb -s <IP>:5555 shell "appops set com.xavier.carlauncher.debug SYSTEM_ALERT_WINDOW allow"
   adb -s <IP>:5555 shell "cmd notification allow_listener com.xavier.carlauncher.debug/com.xavier.carlauncher.media.PowerampNotificationListener"
   ```
4. Fija XavierCarOS como Launcher predeterminado y lánzalo:
   ```powershell
   adb -s <IP>:5555 shell "cmd package set-home-activity com.xavier.carlauncher.debug/com.xavier.carlauncher.MainActivity"
   adb -s <IP>:5555 shell "am start -n com.xavier.carlauncher.debug/com.xavier.carlauncher.MainActivity"
   ```

---

# 📦 OPCIÓN 2 (ALTERNATIVA): Instalación Rápida con Archivos de Doble Clic (`.bat`)

Si prefieres no usar un asistente de IA y realizar la instalación tú mismo directamente desde Windows:

1. Descarga el repositorio desde GitHub y descomprímelo en tu computadora.
2. Asegúrate de conectar tu laptop y la pantalla al mismo Wi-Fi (o al Hotspot de tu celular).
3. Haz doble clic en:  
   👉 **`1_CONECTAR_PANTALLA.bat`**  
   *Escribe la IP de tu pantalla y presiona ENTER. El instalador descargará las herramientas necesarias si no las tienes.*
4. Cuando confirme la conexión exitosa, haz doble clic en:  
   👉 **`2_INSTALAR_XAVIERCAROS.bat`**  
   *Aplicará las optimizaciones de 60 FPS, instalará la aplicación y la dejará activa en tu vehículo.*
5. Si algún día deseas volver a como venía de fábrica, haz doble clic en:  
   👉 **`3_REVERTIR_TODO.bat`**

---

## 🔓 ¿Cómo abrir el puerto de conexión si tu pantalla viene bloqueada?
*(Si el instalador o la IA te indican que la pantalla no responde en el puerto 5555, prueba estas alternativas en orden)*:

### Método A: Menú Secreto de Fábrica (El 90% de las pantallas lo resuelven aquí)
1. En tu pantalla, abre la app de **Ajustes** (o Ajustes del Coche).
2. Busca la opción **Ajustes de Fábrica** (Factory Settings).
3. Te pedirá una contraseña numérica. Prueba las más comunes según la marca:
   - **`3368`** (Pantallas FYT / UIS7862 / UIS8141E / Topway comunes)
   - **`8888`** o **`1617`** (Pantallas TS10 / TS18 / Topway)
   - **`126`** (Pantallas Microntek / MTC / Rockchip)
   - **`000000`** o **`123456`** o **`888888`** (Pantallas genéricas Allwinner / Android)
4. Dentro del menú de fábrica, busca la casilla llamada **"USB Debugging"**, **"ADB"** o **"Protocolo USB: Host / Device"** y márcala como **Activada**. Reinicia la radio.

### Método B: Conexión rápida por Cable USB
1. Consigue un cable USB normal (macho a macho) o el cable USB de datos de tu teléfono.
2. Conéctalo desde tu laptop a uno de los puertos USB traseros de la pantalla (el puerto USB 1 o el que usas para CarPlay/pendrives).
3. En la PC, ejecuta `adb tcpip 5555`. Una vez hecho esto, el puerto quedará abierto y podrás desconectar el cable para seguir por Wi-Fi.

### Método C: Instalar la app "WiFi ADB" o Terminal
1. En la pantalla del carro, abre el navegador o pon en una memoria USB la app gratuita **WiFi ADB** (disponible en APKPure o Play Store).
2. Ábrela y presiona el botón de **"Activar ADB"**. La app te mostrará en grande la IP y el puerto activo (`5555`).

### Método D: Si la pantalla requiere Root
La gran mayoría de pantallas chinas ya vienen rooteadas de fábrica. Si la tuya lo requiere:
- Instala una app de Terminal en la pantalla (*Termux* o *Terminal Emulator*) y escribe:
  ```bash
  su
  setprop service.adb.tcp.port 5555
  stop adbd
  start adbd
  ```
- Si la radio está completamente bloqueada, herramientas seguras como **KingoRoot APK** (instalada directamente desde un pendrive) pueden conceder acceso temporal sin tocar particiones delicadas, garantizando que **jamás se pierda la cámara de reversa ni los controles del timón**.

---

## 🎨 Personalización en tu Vehículo

Una vez instalado XavierCarOS:
1. **Cambiar tu Nombre de Bienvenida:** Mantén presionado el texto *"Hola, Xavier"* durante 1 segundo para escribir tu propio nombre (ej: *"Hola, Carlos"*).
2. **Tu Música en USB:** Conecta cualquier pendrive con canciones (FLAC, MP3 o WAV) y la música arrancará automáticamente al encender el auto.
3. **Letras Karaoke (`.lrc`):** Coloca el archivo `.lrc` junto a la canción con el mismo nombre (ej: `Cancion.mp3` y `Cancion.lrc`).
4. **Traducción en Vivo:** Toca el botón de traducción en el reproductor para traducir del inglés al español al instante.

---

## 📋 Problemas que pueden suceder y cómo se resuelven

| Problema | Por qué ocurre | Cómo se resuelve |
|---|---|---|
| **Aparecía *"XavierCarOS no responde" (ANR)*** | Ocurría en versiones antiguas cuando la radio no tenía internet y se quedaba esperando datos de clima o traducción. | **100% Solucionado.** El sistema cuenta con memoria caché de 0 ms y modo offline inteligente que nunca bloquea la pantalla. |
| **La flecha del mapa no apuntaba hacia donde iba el auto** | Las pantallas chinas tienen magnetómetros muy sensibles al metal del tablero, confundiendo las brújulas convencionales. | **100% Solucionado.** Reemplazamos la flecha por un círculo cibernético de alta visibilidad y el mapa rota dinámicamente con el rumbo satelital real (*Course-Up*). |
| **La música tardaba en sonar al encender el carro** | La radio tarda cerca de 1.5 segundos en montar los puertos USB tras dar contacto. | **100% Solucionado.** `XavierLocalPlayer` incluye un guardián de arranque inteligente con reintento automático a los 2.0s que inicia la música solo. |
| **La IP de la pantalla cambia sola** | El router asigna una IP distinta por DHCP cada vez que se reconecta. | Usa la zona Wi-Fi de tu celular para que la IP se mantenga fija, o consulta la IP en Ajustes > WiFi antes de conectar. |

---

## 📚 Glosario amigable para no expertos

* **Asistente de IA (Antigravity / Claude):** Programa inteligente en tu PC capaz de leer este repositorio y ejecutar todo el proceso de instalación y optimización por ti.
* **Launcher:** Es la pantalla de inicio de Android (donde están los iconos, la hora, el velocímetro y la música). XavierCarOS reemplaza esa pantalla.
* **ADB (Android Debug Bridge):** Herramienta oficial y segura de Google que permite a la computadora comunicarse con la pantalla del carro.
* **ACC ON / ACC OFF:** Señal del carro cuando giras la llave de encendido (ACC ON = motor/contacto encendido, ACC OFF = apagado).
* **ANR ("La aplicación no responde"):** Bloqueo molesto de Android cuando una app está mal optimizada. XavierCarOS está optimizado para tener **cero ANRs**.

---

<div align="center">

### ❤️ ¿Te ha sido útil esta guía?
Apoya a **Xavier Flores** para seguir creando software automotriz de código abierto y alto rendimiento:

[![Ko-fi Banner](https://img.shields.io/badge/Donar%20un%20Café%20en%20Ko--fi-FF5E5B?style=for-the-badge&logo=ko-fi&logoColor=white)](https://ko-fi.com/xavyflores)

**[👉 https://ko-fi.com/xavyflores 👈](https://ko-fi.com/xavyflores)**

</div>
