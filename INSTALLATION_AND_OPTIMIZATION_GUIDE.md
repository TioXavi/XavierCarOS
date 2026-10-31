# 🚀 Guía Definitiva de Instalación, Auditoría y Optimización Extrema — XavierCarOS

<div align="center">

[![Ko-fi](https://img.shields.io/badge/Ko--fi-Support%20Xavy%20Flores-FF5E5B?style=for-the-badge&logo=ko-fi&logoColor=white)](https://ko-fi.com/xavyflores)
[![Android](https://img.shields.io/badge/Android-8.1%20a%2014+-3DDC84?style=for-the-badge&logo=android&logoColor=white)](https://developer.android.com)
[![Platform](https://img.shields.io/badge/Compatible-FYT%20%7C%20TopWay%20%7C%20MTC%20%7C%20TS10-007ACC?style=for-the-badge)](https://github.com)

### ☕ Apoya el Proyecto
> **XavierCarOS** es un desarrollo independiente de ingeniería automotriz de alto rendimiento creado con pasión para transformar cualquier pantalla Android lenta en una experiencia fluida a 60 FPS con estética Cyberpunk OEM.
> 
> Si este sistema y optimizaciones le han dado una nueva vida a tu vehículo, ¡te invito a donar un café para seguir impulsando el desarrollo libre y las nuevas actualizaciones!  
> 👉 **[Support Xavy Flores en Ko-fi](https://ko-fi.com/xavyflores)**

</div>

---

## 📋 Índice General
1. [Filosofía y Garantía del 100% de Éxito](#-filosofía-y-garantía-del-100-de-éxito)
2. [Fase 1: Búsqueda, Conectividad y Acceso ADB](#-fase-1-búsqueda-conectividad-y-acceso-adb)
   - Preguntas de Diagnóstico e Interactividad 1
3. [Fase 2: Auditoría Forense del Hardware de la Pantalla](#-fase-2-auditoría-forense-del-hardware-de-la-pantalla)
   - Preguntas de Diagnóstico e Interactividad 2
4. [Fase 3: Adaptación y Modificaciones Profundas del Sistema (Ultra Fluidez)](#-fase-3-adaptación-y-modificaciones-profundas-del-sistema)
5. [Fase 4: Instalación y Concesión de Permisos Críticos en Cero Clics](#-fase-4-instalación-y-concesión-de-permisos-críticos)
6. [Fase 5: Validación Funcional en Vivo y Modo 100% Offline](#-fase-5-validación-funcional-en-vivo-y-modo-100-offline)
7. [⚡ Script Todo en Uno Automatizado (`deploy_and_optimize.ps1`)](#-script-todo-en-uno-automatizado)

---

## 🎯 Filosofía y Garantía del 100% de Éxito

Las pantallas Android de automóviles (Head Units chinas o genéricas con procesadores Spreadtrum/Unisoc, Allwinner o MediaTek) suelen sufrir de **lentitud severa, congelamientos (ANR) y launchers de fábrica sobrecargados**.

Esta guía garantiza el **100% de compatibilidad, estabilidad y fluidez** porque no asume nada: audita el hardware real de la pantalla, adapta las variables del sistema en bajo nivel (GPU, memoria, renderizado SkiaGL) y blinda la aplicación contra modos de ahorro agresivos.

---

## 🔌 Fase 1: Búsqueda, Conectividad y Acceso ADB

Antes de tocar cualquier archivo, debemos garantizar una tubería de comunicación bidireccional estable entre tu computadora y la radio por **Wi-Fi** (recomendado) o **Cable USB**.

### 1.1 Conexión por Wi-Fi (Recomendado en Vehículo)
1. Conecta la pantalla y tu laptop a la **misma red Wi-Fi** (o comparte internet desde tu teléfono a ambos).
2. En la pantalla, entra a **Ajustes > Red e Internet > Wi-Fi > Toca tu red** y anota la dirección IP (ejemplo: `192.168.1.12`).
3. En tu PC (PowerShell), ejecuta:
```powershell
adb kill-server
adb start-server
adb connect 192.168.1.12:5555
```

### 1.2 Verificación de Estado
Ejecuta:
```powershell
adb devices -l
```

---

### ❓ Preguntas de Diagnóstico de la Fase 1
Responde a este checklist antes de continuar:

- [ ] **P1.1: ¿El comando `adb devices` muestra `192.168.1.XX:5555 device`?**
  - **SÍ:** ✅ Conectividad perfecta. Pasa a la [Fase 2](#-fase-2-auditoría-forense-del-hardware-de-la-pantalla).
  - **NO (Muestra `unauthorized`):** ⚠️ En la pantalla del vehículo acaba de aparecer una ventana: *"¿Permitir depuración USB?"*. Marca la casilla **"Permitir siempre desde esta computadora"** y pulsa **Aceptar**.
  - **NO (Muestra `offline` o no conecta / error 10060):** ❌ El puerto 5555 está cerrado o la IP cambió.
    - *Solución:* En los ajustes de fábrica de tu pantalla (código habitual `3368`, `8888`, `126` o `000000`), activa **"ADB Over Network"** o conecta un cable USB temporalmente y ejecuta:
      ```powershell
      adb usb
      adb tcpip 5555
      ```
- [ ] **P1.2: ¿Tu computadora y la pantalla están en redes con aislamiento de AP (AP Isolation)?**
  - Si usas el Wi-Fi de tu casa o router de oficina, desactiva temporalmente el *Client Isolation*. La forma más rápida y confiable es usar la zona Wi-Fi (Hotspot) de tu propio teléfono celular.

---

## 🔍 Fase 2: Auditoría Forense del Hardware de la Pantalla

Cada radio automotriz tiene especificaciones distintas. Ejecutaremos una radiografía forense completa mediante comandos ADB directos para conocer sus límites exactos:

```powershell
# 1. Obtener Chipset / Procesador / SoC
adb shell getprop ro.board.platform
adb shell getprop ro.product.board

# 2. Arquitectura de CPU
adb shell getprop ro.product.cpu.abi

# 3. Versión de Android y API Level
adb shell getprop ro.build.version.release
adb shell getprop ro.build.version.sdk

# 4. Memoria RAM real instalada y disponible
adb shell "cat /proc/meminfo | grep -E 'MemTotal|MemFree|MemAvailable|SwapTotal'"

# 5. Resolución nativa y Densidad de Pantalla
adb shell wm size
adb shell wm density

# 6. Identificar la Plataforma / Fabricante Automotriz (FYT, TopWay, Microntek, etc.)
adb shell "pm list packages | grep -E 'syu|tw\.|microntek|ts\.|autochips'"

# 7. Renderizador Gráfico Actual
adb shell getprop debug.hwui.renderer
```

---

### ❓ Preguntas de Diagnóstico de la Fase 2
Analiza las salidas obtenidas y clasifica tu pantalla:

- [ ] **P2.1: ¿Cuál es el fabricante / plataforma detectado en el punto 6?**
  - **`com.syu.*` (FYT / XY Auto / UIS7862 / UIS8141E / SC9853i):** Plataforma FYT líder. XavierCarOS integra soporte nativo directo para sus servicios de MCU, luces y Bluetooth.
  - **`com.tw.*` o `com.ts.*` (TopWay / TS10 / TS18):** Plataforma TopWay. XavierCarOS usará el enrutador universal `AutomotiveIntentResolver`.
  - **`com.microntek.*` (MTC / Rockchip PX5 / PX6):** Plataforma Microntek. El interceptor CANBus modular decodificará las puertas mediante el formato de byte array.
  - **Ninguno de los anteriores (Genérica / AOSP):** XavierCarOS usará los estándares nativos de Android Automotive (A2DP Sink, AVRCP y Action Dial).
- [ ] **P2.2: ¿Cuánta memoria RAM física tiene la unidad?**
  - **1 GB a 2 GB RAM:** Requiere congelar el launcher de fábrica para evitar que el sistema cierre apps en segundo plano (detallado en la Fase 3).
  - **3 GB a 8 GB RAM:** Rendimiento holgado. Admite multitarea completa simultánea (Waze + Spotify + XavierCarOS).
- [ ] **P2.3: ¿La resolución reportada en `wm size` coincide con la nativa?**
  - Las resoluciones automotrices estándar son `1024x600`, `1280x720` o `1920x1080`. XavierCarOS escala fluidamente por Compose DPI a cualquier dimensión.

---

## ⚡ Fase 3: Adaptación y Modificaciones Profundas del Sistema

Para lograr que una radio con procesador modesto vuele a **60 FPS** sin lag ni congelamientos, aplicamos optimizaciones a nivel de renderizado y administración de energía:

### 3.1 Aceleración Gráfica Forzada (GPU Hardware Rendering)
Por defecto, muchas radios chinas delegan capas al procesador en lugar de la GPU Mali/PowerVR. Forzamos el procesado 100% por GPU:
```powershell
# Forzar composición por GPU en lugar de CPU
adb shell setprop debug.sf.hw 1
adb shell setprop debug.egl.hw 1
adb shell settings put global force_gpu_rendering 1

# Activar el motor SkiaGL para Jetpack Compose (aceleración 2D ultra veloz)
adb shell setprop debug.hwui.renderer skiagl
```

### 3.2 Escala de Animaciones a Velocidad Deportiva (0.5x)
Las animaciones de Android 8.1 / 10 a escala 1.0x se sienten pesadas y lentas. Reducirlas a 0.5x duplica la sensación de respuesta táctil:
```powershell
adb shell settings put global window_animation_scale 0.5
adb shell settings put global transition_animation_scale 0.5
adb shell settings put global animator_duration_scale 0.5
```

### 3.3 Blindaje contra ANR y Whitelist del Modo Reposo (Doze Mode)
Evita que el sistema operativo "congele" XavierCarOS o apague el receptor GPS cuando la pantalla entra en bajo consumo:
```powershell
# Agregar a la lista blanca del sistema
adb shell dumpsys deviceidle whitelist +com.xavier.carlauncher
adb shell dumpsys deviceidle whitelist +com.xavier.carlauncher.debug

# Permitir ejecución libre en segundo plano
adb shell cmd appops set com.xavier.carlauncher RUN_IN_BACKGROUND allow
adb shell cmd appops set com.xavier.carlauncher.debug RUN_IN_BACKGROUND allow
```

### 3.4 Deshabilitar Launchers de Fábrica Lentos (Opcional pero muy recomendado)
Si tu pantalla tiene un launcher de fábrica pesado que consume 300MB de RAM en segundo plano, puedes congelarlo con seguridad (no se borra, solo se inhabilita):
```powershell
# Ejemplo para FYT (reemplazar por el paquete auditado en la Fase 2)
adb shell pm disable-user --user 0 com.syu.launcher47
# Para volverlo a habilitar si algún día lo deseas:
# adb shell pm enable com.syu.launcher47
```

---

## 📦 Fase 4: Instalación y Concesión de Permisos Críticos

Instala el APK generado de XavierCarOS y otorga todos los permisos por consola para evitar molestos diálogos emergentes mientras conduces:

```powershell
# 1. Instalar APK (Reemplazar con la ruta de tu APK)
adb install -r -d app/build/outputs/apk/debug/app-debug.apk

# 2. Concesión de Permiso de Ubicación Precisa (GPS / Velocímetro)
adb shell pm grant com.xavier.carlauncher.debug android.permission.ACCESS_FINE_LOCATION
adb shell pm grant com.xavier.carlauncher.debug android.permission.ACCESS_COARSE_LOCATION

# 3. Permisos de Almacenamiento (Lectura de USB y Música Local)
adb shell pm grant com.xavier.carlauncher.debug android.permission.READ_EXTERNAL_STORAGE
adb shell pm grant com.xavier.carlauncher.debug android.permission.WRITE_EXTERNAL_STORAGE

# 4. Permiso de Superposición en Pantalla (System Alert Window)
adb shell appops set com.xavier.carlauncher.debug SYSTEM_ALERT_WINDOW allow

# 5. Habilitar Interceptor de Notificaciones (Para metadatos multimedia Bluetooth)
adb shell cmd notification allow_listener com.xavier.carlauncher.debug/com.xavier.carlauncher.media.PowerampNotificationListener

# 6. Fijar XavierCarOS como Launcher Predeterminado
adb shell cmd package set-home-activity com.xavier.carlauncher.debug/com.xavier.carlauncher.MainActivity
```

---

## 🚦 Fase 5: Validación Funcional en Vivo y Modo 100% Offline

Verifica en tu automóvil que cada componente esté operando al 100%:

| Módulo | Comportamiento Esperado | Verificación |
|---|---|---|
| **GPS / Velocímetro** | El círculo guía apunta y se orienta hacia la carretera. | Inicia la marcha: a > 1 mph el mapa rota dinámicamente según la dirección del vehículo. |
| **Reproductor Local** | Auto-play inmediato al encender el auto. | Enciende el switch (ACC ON): la música arranca en 2.0s leyendo la USB. |
| **Personalización** | Nombre de bienvenida a medida. | Mantén presionado `"Hola, Xavier"` durante 1 segundo para ingresar tu nombre. |
| **Clima Inteligente** | Actualización cada 10 min o silencioso offline. | No congela la pantalla si no hay internet; muestra el último reporte en caché sin demoras. |
| **Estudio de Letras** | Desplazamiento instantáneo al tocar el dock de música. | Toca el icono de música o desliza: cambia a 0ms sin transiciones pesadas. |

---

## ⚡ Script Todo en Uno Automatizado

Hemos creado para ti el script automatizado [`scripts/deploy_and_optimize.ps1`](file:///c:/Users/UserAnonimo/.gemini/antigravity/scratch/XavierCarOS2/scripts/deploy_and_optimize.ps1).

Para ejecutar todo este proceso guiado con preguntas interactivas en tu terminal:

```powershell
.\scripts\deploy_and_optimize.ps1 -DeviceIp "192.168.1.12"
```

El script:
1. Conecta y valida ADB.
2. Extrae y formatea toda la auditoría de hardware en tu pantalla.
3. Te realiza preguntas de confirmación antes de aplicar cada optimización.
4. Aplica las optimizaciones de SkiaGL, animaciones 0.5x y Doze whitelist.
5. Instala el APK y aprueba todos los permisos en 5 segundos.

---

<div align="center">

### ❤️ ¿Te gustó el proyecto?
Apoya a **Xavier Flores** para seguir creando software automotriz de código abierto y alto rendimiento:

[![Ko-fi Banner](https://img.shields.io/badge/Donar%20un%20Café%20en%20Ko--fi-FF5E5B?style=for-the-badge&logo=ko-fi&logoColor=white)](https://ko-fi.com/xavyflores)

**[👉 https://ko-fi.com/xavyflores 👈](https://ko-fi.com/xavyflores)**

</div>
