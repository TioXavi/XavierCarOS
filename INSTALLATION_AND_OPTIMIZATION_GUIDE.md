# 🚗 Guía Definitiva de Instalación y Optimización de Pantallas Android con XavierCarOS
### *Diseñada para cualquier persona (incluso sin conocimientos de programación ni informática)*

<div align="center">

[![Ko-fi](https://img.shields.io/badge/Ko--fi-Support%20Xavy%20Flores-FF5E5B?style=for-the-badge&logo=ko-fi&logoColor=white)](https://ko-fi.com/xavyflores)
[![Windows](https://img.shields.io/badge/Instaladores-.bat%20Doble%20Clic-0078D6?style=for-the-badge&logo=windows&logoColor=white)](https://github.com/TioXavi/XavierCarOS)
[![FPS](https://img.shields.io/badge/Rendimiento-60%20FPS%20GPU%20SkiaGL-success?style=for-the-badge)](https://github.com/TioXavi/XavierCarOS)

### ☕ ¿Te gusta el proyecto?
> **XavierCarOS** fue desarrollado con dedicación para transformar cualquier pantalla Android de carro lenta y saturada en una experiencia de lujo, rápida, fluida y con diseño Cyberpunk OEM.
> 
> Si esta guía e instaladores le han dado una nueva vida a tu vehículo, ¡te invito a donar un café para seguir mejorando el proyecto y creando nuevas funciones!  
> 👉 **[Support Xavy Flores en Ko-fi (Invítame un café)](https://ko-fi.com/xavyflores)** ☕

</div>

---

## 🌟 Qué vas a lograr

Con esta guía y los instaladores automáticos (`.bat`) podrás cambiar la pantalla lenta y aburrida de tu radio Android por **XavierCarOS**, dejándola mucho más rápida y fluida (a 60 cuadros por segundo), **sin necesidad de programar ni escribir comandos en terminales**.

### ✨ Características que tendrás en tu vehículo:
1. **Velocidad Extrema (60 FPS):** Optimizaciones profundas de fábrica que eliminan los tirones y el molesto mensaje de *"La aplicación no responde"* (cero ANRs).
2. **Velocímetro GPS con Brújula Dinámica:** La pantalla detecta tu velocidad real por satélite y orienta el mapa automáticamente hacia el frente de la carretera por donde conduces.
3. **Reproductor de Música Nativo con Auto-Play:** Conecta tu memoria USB y la música arrancará sola automáticamente cada vez que enciendas el carro (ACC ON).
4. **Letras Karaoke en Vivo (`.lrc`) y Traducción:** Pantalla de música ampliada con letras sincronizadas que se iluminan al ritmo de la canción y traducción instantánea de inglés a español con un solo toque.
5. **Clima Inteligente (Zero-Config):** Actualización automática cada 10 minutos o 100% sin internet (modo offline inmediato). No tienes que registrarte ni crear cuentas para el clima.
6. **Saludo Personalizado:** Mantén presionado *"Hola, Xavier"* durante 1 segundo para escribir tu propio nombre o apodo.
7. **100% Seguro y Reversible:** Nada de lo que hacemos daña la radio. Con un solo doble clic en `3_REVERTIR_TODO.bat` tu pantalla vuelve exactamente como venía de fábrica.
8. **Lo que NO se toca y sigue funcionando al 100%:** Tu cámara de reversa original, los controles del timón (volante), la radio FM y las llamadas Bluetooth siguen funcionando perfectamente.

---

## 🛠️ Antes de empezar: Qué necesitas

| Elemento | Para qué sirve | Notas para novatos |
|---|---|---|
| **Laptop o PC con Windows 10 u 11** | Para ejecutar los instaladores de un solo clic. | Cualquier laptop sirve. |
| **La pantalla Android de tu carro** | El equipo donde se instalará XavierCarOS. | Puede ser de 7, 9 o 10 pulgadas (cualquier marca china o genérica). |
| **Red WiFi Compartida (Hotspot del Celular)** | Para que tu laptop y la pantalla puedan comunicarse. | **Recomendación de oro:** Activa la *"Zona Wi-Fi / Compartir Internet"* de tu celular y conecta ahí tanto la laptop como la pantalla. |
| **Repositorio descargado en tu PC** | Contiene los archivos `.bat` y la aplicación lista. | Solo descargas el archivo ZIP desde [GitHub](https://github.com/TioXavi/XavierCarOS) y lo descomprimes en una carpeta. |

---

## 🛡️ Reglas de Oro y Seguridad

1. **Haz la instalación con el vehículo detenido** (en tu casa, garaje o parqueo).
2. **Nunca formatees el firmware de la radio**: No hace falta. XavierCarOS se instala como una aplicación principal sin tocar las partes críticas del carro.
3. **Todo se hace con doble clic**: No tienes que instalar programas raros ni saber comandos de consola; la carpeta ya trae instaladores `.bat` numerados (1, 2 y 3).

---

## 🔌 PASO 1: Conectar tu PC con la Pantalla

### 1.1 Conecta ambos al mismo WiFi
1. Enciende la zona Wi-Fi (punto de acceso) de tu celular.
2. Conecta tu laptop a ese Wi-Fi.
3. En la pantalla del carro, ve a **Ajustes > WiFi** y conéctate también al mismo Wi-Fi de tu celular.
4. Toca sobre el nombre de tu Wi-Fi en la pantalla para ver la dirección IP (suele ser algo como `192.168.43.XX` o `192.168.1.XX`). Anota ese número.

### 1.2 Ejecutar el Asistente de Conexión
1. En tu laptop, entra a la carpeta de XavierCarOS.
2. Haz doble clic sobre el archivo:  
   👉 **`1_CONECTAR_PANTALLA.bat`**
3. El programa revisará si tienes las herramientas de Google; si no las tienes, **las descargará solo automáticamente**.
4. Escribe la dirección IP de tu pantalla que anotaste antes y presiona `ENTER`.

---

### ❓ ¿Qué pasa según lo que veas en la pantalla de la laptop?

* ✅ **Si dice "¡CONEXIÓN EXITOSA!":**  
  ¡Listo! Puedes cerrar esa ventana y pasar directo al **PASO 2**.

* ⚠️ **Si dice "¡PANTALLA NO AUTORIZADA TODAVÍA!":**  
  Mira la pantalla de tu carro en ese momento: habrá salido un mensaje preguntando *"¿Permitir depuración USB?"*. Marca la casilla **"Permitir siempre desde esta computadora"** y pulsa **Aceptar**. Luego vuelve a hacer doble clic en `1_CONECTAR_PANTALLA.bat`.

* ❌ **Si dice "NO SE PUDO CONECTAR":**  
  Significa que el fabricante de tu pantalla dejó el puerto de conexión (puerto 5555) apagado de fábrica. Sigue la guía de solución a continuación.

---

### 🔓 ¿Cómo abrir el puerto de conexión si tu pantalla viene bloqueada?
*(Prueba estas alternativas en orden, de la más fácil a la más avanzada)*:

#### Alternativa A: Menú Secreto de Fábrica (El 90% de las pantallas lo resuelven aquí)
1. En tu pantalla, abre la app de **Ajustes** (o Ajustes del Coche).
2. Busca la opción **Ajustes de Fábrica** (Factory Settings).
3. Te pedirá una contraseña numérica. Prueba las más comunes según el fabricante:
   - **`3368`** (Pantallas FYT / Topway comunes)
   - **`8888`** o **`1617`** (Pantallas TS10 / Topway)
   - **`126`** (Pantallas Microntek / MTC)
   - **`000000`** o **`123456`** o **`888888`** (Pantallas genéricas Allwinner / Android)
4. Una vez dentro del menú de fábrica, busca una casilla llamada **"USB Debugging"**, **"ADB"** o **"Protocolo USB: Host / Device"** y márcala como **Activada**. Reinicia la radio.

#### Alternativa B: Conexión rápida por Cable USB
1. Consigue un cable USB normal (macho a macho) o el cable que uses para conectar tu teléfono a la radio.
2. Conéctalo desde tu laptop a uno de los puertos USB traseros de la pantalla (el que se usa habitualmente para CarPlay o USB 1).
3. En tu laptop, abre `1_CONECTAR_PANTALLA.bat` y el sistema abrirá el puerto Wi-Fi al instante. Luego puedes desconectar el cable y seguir sin cables.

#### Alternativa C: Instalar la app "WiFi ADB" o Terminal
1. En la pantalla del carro, abre el navegador web o pon en una memoria USB la app gratuita **WiFi ADB** (disponible en APKPure o Play Store).
2. Ábrela y pulsa el botón verde de **"Activar ADB"**. La app te mostrará en letras grandes la IP y el puerto activo (`5555`).

#### Alternativa D: Si la pantalla requiere Root
La gran mayoría de pantallas Android chinas **ya vienen con permisos de administrador (Root) activados de fábrica**. Si la tuya lo pide:
- Puedes instalar en la radio una app de Terminal (como *Termux* o *Terminal Emulator*) y escribir:
  ```bash
  su
  setprop service.adb.tcp.port 5555
  stop adbd
  start adbd
  ```
- Si la radio estuviera completamente bloqueada por el vendedor, herramientas seguras como **KingoRoot APK** (instalado directamente desde un pendrive) pueden conceder root temporal en 1 minuto sin necesidad de flashear firmware peligroso.

---

## 🚀 PASO 2: Instalar y Optimizar a 60 FPS

Una vez confirmada la conexión en el Paso 1:

1. En tu laptop, haz doble clic sobre:  
   👉 **`2_INSTALAR_XAVIERCAROS.bat`**
2. Siéntate y mira la pantalla: el instalador hará todo por ti en 30 segundos:
   - ⚡ **Acelera los gráficos:** Fuerza a la pantalla a renderizar con la tarjeta gráfica GPU (`SkiaGL`) para que no se tranque.
   - 🏎️ **Animaciones a velocidad deportiva:** Reduce las animaciones a `0.5x` para que al tocar cualquier botón la respuesta sea instantánea.
   - 🛡️ **Blindaje de Batería:** Protege a XavierCarOS para que el sistema de la radio no lo cierre cuando vas conduciendo.
   - 📲 **Instala la aplicación y concede todos los permisos:** Ubicación para el velocímetro, lectura de memorias USB para música y micrófono para el asistente de voz (sin que tengas que aceptar ventanas molestas a mano).
   - 🏠 **Fija XavierCarOS como pantalla principal:** Cada vez que enciendas tu carro o toques el botón de "Inicio / Home", XavierCarOS aparecerá de inmediato.

---

## 🎨 PASO 3: Personalizar y Disfrutar en tu Vehículo

¡Ya tienes XavierCarOS corriendo en tu auto! Aquí tienes trucos rápidos para personalizarlo:

1. **Cambiar tu Nombre de Bienvenida:**  
   En la pantalla principal, mantén presionado el texto *"Hola, Xavier"* durante 1 segundo. Se abrirá una ventana elegante donde podrás escribir tu propio nombre (ejemplo: *"Hola, Carlos"*) y pulsar Guardar.
2. **Tu Música en USB:**  
   Guarda tus canciones (en formato FLAC, MP3 o WAV) en cualquier memoria USB y conéctala al carro. La música empezará a reproducirse automáticamente al encender el auto.
3. **Letras Karaoke (`.lrc`):**  
   Si quieres ver la letra sincronizada en tiempo real, pon el archivo `.lrc` con el mismo nombre de la canción en la misma carpeta (ejemplo: `Musica/MiCancion.mp3` y `Musica/MiCancion.lrc`).
4. **Traducción de Letras:**  
   En el estudio de música (deslizando a la derecha o tocando el icono de música en la barra izquierda), toca el botón de traducción para ver la letra en español al instante.
5. **Brújula y Mapa que se orienta hacia el frente:**  
   Al comenzar a manejar (a más de 1 mph o km/h), el mapa rotará automáticamente apuntando hacia donde avanza tu carro, con un círculo cibernético de alta visibilidad.

---

## 🔄 PASO 4: Cómo volver a como venía de fábrica (100% Reversible)

Si por cualquier motivo vendes el auto, prestas el vehículo o quieres volver al launcher original que traía la radio:

1. En tu laptop, haz doble clic sobre:  
   👉 **`3_REVERTIR_TODO.bat`**
2. El sistema restaurará las animaciones originales a 1.0x, liberará la pantalla de inicio y te preguntará si deseas desinstalar XavierCarOS.
3. Tu radio volverá a su estado original exactamente como el primer día, sin dejar ningún rastro.

---

## 📋 Problemas que pueden suceder y cómo se resuelven
*(Basado en situaciones reales de pruebas en carretera y pantallas automotrices)*

| Problema | Por qué ocurre | Cómo se resuelve |
|---|---|---|
| **Aparecía *"XavierCarOS no responde" (ANR)*** | Ocurría en versiones antiguas cuando la radio no tenía internet y el sistema se quedaba esperando respuestas de clima o traducción. | **100% Solucionado.** El sistema ahora cuenta con memoria caché de 0 milisegundos y modo offline inteligente que nunca bloquea la pantalla. |
| **La flecha del mapa no apuntaba hacia donde iba el auto** | Las pantallas chinas tienen magnetómetros muy sensibles al metal del tablero del auto, lo que confunde las brújulas convencionales. | **100% Solucionado.** Reemplazamos la flecha por un círculo indicador cibernético y el mapa ahora rota dinámicamente usando el rumbo real de avance satelital (Course-Up). |
| **La música tardaba en sonar al encender el carro** | La radio tarda cerca de 1.5 segundos en detectar y montar los puertos USB tras dar el contacto. | **100% Solucionado.** `XavierLocalPlayer` incluye un guardián de arranque inteligente con reintento automático a los 2.0 segundos que inicia la reproducción sola. |
| **La IP de la pantalla cambia sola** | El router de la casa o el teléfono asigna una IP distinta por DHCP cada vez que se reconecta. | Usa la zona Wi-Fi de tu celular para que la IP se mantenga fija, o consulta la IP actual en Ajustes > WiFi antes de conectar. |
| **Las opciones de desarrollador piden contraseña** | El fabricante chino bloquea el menú para que nadie lo desconfigure. | No fuerces contraseñas al azar. Usa los códigos universales de fábrica (`3368`, `8888`, `1617`, `126`) o conecta un cable USB temporal. |
| **El launcher original vuelve a aparecer al reiniciar** | Algunas marcas (como TopWay o FYT) tienen servicios de vigilancia que restauran su propia app. | El comando de `2_INSTALAR_XAVIERCAROS.bat` utiliza `cmd package set-home-activity`, asegurando que XavierCarOS sea la pantalla predeterminada permanente. |

---

## 📚 Glosario amigable para no expertos

* **Launcher:** Es la pantalla de inicio de Android (donde están los iconos, la hora, el velocímetro y la música). XavierCarOS reemplaza esa pantalla para que se vea moderna y deportiva.
* **ADB (Android Debug Bridge):** Es la herramienta oficial y segura que permite a tu computadora comunicarse con la pantalla del carro por Wi-Fi o cable para instalar mejoras.
* **Firmware:** El sistema operativo base que trae la radio de fábrica. Nosotros **no modificamos el firmware**, por eso la instalación es 100% segura.
* **ACC ON / ACC OFF:** Las señales del carro cuando giras la llave de encendido (ACC ON = encendido, ACC OFF = apagado). XavierCarOS usa esto para pausar y reanudar la música solo.
* **ANR ("La aplicación no responde"):** Mensaje de bloqueo que muestra Android cuando una app mal optimizada se congela. XavierCarOS está optimizado a bajo nivel para tener **cero ANRs**.
* **Archivo .bat:** Un archivo simple de Windows que realiza tareas de forma automática al hacerle doble clic, para que tú no tengas que escribir nada.
* **Hotspot / Punto de Acceso:** La función de tu teléfono celular que permite compartir tus datos móviles por Wi-Fi con otros dispositivos.

---

<div align="center">

### ❤️ ¿Te ha sido útil esta guía?
Apoya a **Xavier Flores** para seguir creando software automotriz de código abierto y alto rendimiento:

[![Ko-fi Banner](https://img.shields.io/badge/Donar%20un%20Café%20en%20Ko--fi-FF5E5B?style=for-the-badge&logo=ko-fi&logoColor=white)](https://ko-fi.com/xavyflores)

**[👉 https://ko-fi.com/xavyflores 👈](https://ko-fi.com/xavyflores)**

</div>
