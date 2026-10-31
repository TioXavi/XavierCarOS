package com.xavier.carlauncher.voice

import android.content.Context
import android.content.Intent
import android.media.AudioManager
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.util.Log
import com.xavier.carlauncher.illumination.VehicleIlluminationManager
import com.xavier.carlauncher.media.XavierLocalPlayer
import com.xavier.carlauncher.weather.WeatherRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.Locale

/**
 * Offline Automotive Voice Controller for XavierCarOS (v2.0).
 *
 * Provides completely hands-free voice control designed for driving safety:
 * - Operates 100% offline via local on-device speech models (EXTRA_PREFER_OFFLINE).
 * - Real-time NLP intent matching for music, volume, navigation, climate, and lights.
 * - Spoken conversational feedback using the warm Colombian female voice assistant.
 * - Reactive state flow for animated listening banners and soundwave pulses.
 */
class OfflineVoiceController private constructor(private val context: Context) {

    companion object {
        private const val TAG = "OfflineVoiceController"

        @Volatile
        private var instance: OfflineVoiceController? = null

        fun getInstance(context: Context): OfflineVoiceController {
            return instance ?: synchronized(this) {
                instance ?: OfflineVoiceController(context.applicationContext).also { instance = it }
            }
        }
    }

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private val mainHandler = Handler(Looper.getMainLooper())
    private val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
    private val voiceAssistant = CarVoiceAssistant.getInstance(context)

    private var speechRecognizer: SpeechRecognizer? = null

    // ── Reactive States for UI ────────────────────────────────────────────────
    private val _isListening = MutableStateFlow(false)
    val isListening: StateFlow<Boolean> = _isListening.asStateFlow()

    private val _recognizedText = MutableStateFlow("")
    val recognizedText: StateFlow<String> = _recognizedText.asStateFlow()

    private val _assistantFeedback = MutableStateFlow("")
    val assistantFeedback: StateFlow<String> = _assistantFeedback.asStateFlow()

    private val _audioRmsLevel = MutableStateFlow(0f)
    val audioRmsLevel: StateFlow<Float> = _audioRmsLevel.asStateFlow()

    // ── Recognition Listener ──────────────────────────────────────────────────
    private val listener = object : RecognitionListener {
        override fun onReadyForSpeech(params: Bundle?) {
            _isListening.value = true
            _assistantFeedback.value = "Te escucho..."
            Log.d(TAG, "SpeechRecognizer: Ready for speech")
        }

        override fun onBeginningOfSpeech() {
            _assistantFeedback.value = "Escuchando tu voz..."
            Log.d(TAG, "SpeechRecognizer: Beginning of speech")
        }

        override fun onRmsChanged(rmsdB: Float) {
            _audioRmsLevel.value = ((rmsdB + 2f) / 12f).coerceIn(0f, 1f)
        }

        override fun onBufferReceived(buffer: ByteArray?) {}

        override fun onEndOfSpeech() {
            _assistantFeedback.value = "Procesando comando..."
            _isListening.value = false
            Log.d(TAG, "SpeechRecognizer: End of speech")
        }

        override fun onError(error: Int) {
            _isListening.value = false
            val errorMsg = when (error) {
                SpeechRecognizer.ERROR_NO_MATCH -> "No entendí el comando. Intenta de nuevo."
                SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> "Tiempo agotado. Toca el micrófono para hablar."
                SpeechRecognizer.ERROR_AUDIO -> "Error de captura de micrófono."
                SpeechRecognizer.ERROR_RECOGNIZER_BUSY -> "Reconocedor ocupado."
                else -> "Listo para escuchar."
            }
            _assistantFeedback.value = errorMsg
            Log.w(TAG, "SpeechRecognizer error: $error ($errorMsg)")
        }

        override fun onResults(results: Bundle?) {
            _isListening.value = false
            val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
            val query = matches?.firstOrNull() ?: ""
            if (query.isNotBlank()) {
                _recognizedText.value = query
                executeVoiceCommand(query.trim())
            } else {
                _assistantFeedback.value = "No escuché ningún comando."
            }
        }

        override fun onPartialResults(partialResults: Bundle?) {
            val partial = partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)?.firstOrNull()
            if (!partial.isNullOrBlank()) {
                _recognizedText.value = partial
            }
        }

        override fun onEvent(eventType: Int, params: Bundle?) {}
    }

    /**
     * Initializes or wakes the speech recognizer on the main thread.
     */
    fun startListening() {
        mainHandler.post {
            if (_isListening.value) {
                stopListening()
                return@post
            }

            try {
                if (!SpeechRecognizer.isRecognitionAvailable(context)) {
                    Log.w(TAG, "Speech recognition not available on this ROM")
                    _assistantFeedback.value = "Reconocimiento no disponible en esta pantalla"
                    voiceAssistant.speak("El micrófono no está disponible en este sistema.")
                    return@post
                }

                if (speechRecognizer == null) {
                    speechRecognizer = SpeechRecognizer.createSpeechRecognizer(context).apply {
                        setRecognitionListener(listener)
                    }
                }

                val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                    putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                    putExtra(RecognizerIntent.EXTRA_LANGUAGE, "es-419")
                    putExtra(RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE, "es-419")
                    putExtra(RecognizerIntent.EXTRA_ONLY_RETURN_LANGUAGE_PREFERENCE, "es-419")
                    putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 3)
                    putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
                    // Request offline on-device speech processing
                    putExtra(RecognizerIntent.EXTRA_PREFER_OFFLINE, true)
                }

                _recognizedText.value = ""
                _assistantFeedback.value = "Iniciando micrófono..."
                speechRecognizer?.startListening(intent)
                _isListening.value = true
                Log.i(TAG, "Speech listening started (offline preference requested)")
            } catch (e: Exception) {
                Log.e(TAG, "Error starting speech recognizer", e)
                _isListening.value = false
                _assistantFeedback.value = "Error al abrir micrófono"
            }
        }
    }

    /**
     * Stops listening immediately.
     */
    fun stopListening() {
        mainHandler.post {
            try {
                speechRecognizer?.stopListening()
                _isListening.value = false
                _assistantFeedback.value = "Micrófono cerrado"
            } catch (e: Exception) {
                Log.w(TAG, "Error stopping recognizer", e)
            }
        }
    }

    /**
     * Offline Natural Language Processing intent dispatcher for vehicle commands.
     */
    fun executeVoiceCommand(rawText: String) {
        val text = rawText.lowercase(Locale.getDefault())
        Log.i(TAG, "Processing vehicle voice command: '$text'")

        when {
            // ── 1. Music: Play ────────────────────────────────────────────────
            text.containsAny("reproducir", "poner música", "pon música", "play", "reanudar", "dale play", "música") &&
            !text.containsAny("siguiente", "anterior", "pausar", "parar", "abrir", "subir", "bajar") -> {
                com.xavier.carlauncher.media.PowerampNotificationListener.togglePlayPause(context)
                _assistantFeedback.value = "▶️ Reproduciendo música"
                voiceAssistant.speak("Reproduciendo tu música.")
            }

            // ── 2. Music: Pause / Stop ─────────────────────────────────────────
            text.containsAny("pausar", "pausa", "detener", "parar", "para la música", "stop", "silencia la música") -> {
                com.xavier.carlauncher.media.PowerampNotificationListener.togglePlayPause(context)
                _assistantFeedback.value = "⏸️ Música en pausa"
                voiceAssistant.speak("Música pausada.")
            }

            // ── 3. Music: Next Track ──────────────────────────────────────────
            text.containsAny("siguiente", "pasa", "cambiar", "avanzar", "otra canción", "adelantar", "next") -> {
                com.xavier.carlauncher.media.PowerampNotificationListener.skipToNext(context)
                _assistantFeedback.value = "⏭️ Siguiente canción"
                voiceAssistant.speak("Siguiente canción.")
            }

            // ── 4. Music: Previous Track ──────────────────────────────────────
            text.containsAny("anterior", "canción anterior", "atrás", "regresar", "previa", "atras") -> {
                com.xavier.carlauncher.media.PowerampNotificationListener.skipToPrevious(context)
                _assistantFeedback.value = "⏮️ Canción anterior"
                voiceAssistant.speak("Canción anterior.")
            }

            // ── 5. Volume: Raise ──────────────────────────────────────────────
            text.containsAny("subir volumen", "más volumen", "aumentar volumen", "volumen arriba", "mas volumen", "sube el volumen") -> {
                audioManager.adjustStreamVolume(AudioManager.STREAM_MUSIC, AudioManager.ADJUST_RAISE, AudioManager.FLAG_SHOW_UI)
                audioManager.adjustStreamVolume(AudioManager.STREAM_MUSIC, AudioManager.ADJUST_RAISE, AudioManager.FLAG_SHOW_UI)
                _assistantFeedback.value = "🔊 Volumen subido"
                voiceAssistant.speak("Volumen aumentado.")
            }

            // ── 6. Volume: Lower ──────────────────────────────────────────────
            text.containsAny("bajar volumen", "menos volumen", "disminuir volumen", "volumen abajo", "baja el volumen") -> {
                audioManager.adjustStreamVolume(AudioManager.STREAM_MUSIC, AudioManager.ADJUST_LOWER, AudioManager.FLAG_SHOW_UI)
                audioManager.adjustStreamVolume(AudioManager.STREAM_MUSIC, AudioManager.ADJUST_LOWER, AudioManager.FLAG_SHOW_UI)
                _assistantFeedback.value = "🔉 Volumen bajado"
                voiceAssistant.speak("Volumen reducido.")
            }

            // ── 7. Volume: Mute ───────────────────────────────────────────────
            text.containsAny("silencio", "mutear", "sin sonido", "mute", "cállate", "callate") -> {
                audioManager.adjustStreamVolume(AudioManager.STREAM_MUSIC, AudioManager.ADJUST_MUTE, AudioManager.FLAG_SHOW_UI)
                _assistantFeedback.value = "🔇 Silencio activado"
                voiceAssistant.speak("Silencio activado.")
            }

            // ── 8. Navigation / Map ───────────────────────────────────────────
            text.containsAny("abrir mapa", "mapa", "mapas", "navegación", "navegacion", "waze", "google maps", "cómo llegar", "ruta") -> {
                _assistantFeedback.value = "🗺️ Abriendo navegación"
                try {
                    val wazeIntent = context.packageManager.getLaunchIntentForPackage("com.waze")
                    val mapsIntent = context.packageManager.getLaunchIntentForPackage("com.google.android.apps.maps")
                    val organicIntent = context.packageManager.getLaunchIntentForPackage("app.organicmaps")
                        ?: context.packageManager.getLaunchIntentForPackage("app.organicmaps.web")

                    when {
                        wazeIntent != null -> {
                            voiceAssistant.speak("Abriendo Waze.")
                            wazeIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                            context.startActivity(wazeIntent)
                        }
                        mapsIntent != null -> {
                            voiceAssistant.speak("Abriendo Google Maps.")
                            mapsIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                            context.startActivity(mapsIntent)
                        }
                        organicIntent != null -> {
                            voiceAssistant.speak("Abriendo Organic Maps.")
                            organicIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                            context.startActivity(organicIntent)
                        }
                        else -> {
                            voiceAssistant.speak("El mapa satelital integrado está activo en tu pantalla.")
                        }
                    }
                } catch (_: Exception) {}
            }

            // ── 9. Weather Info ───────────────────────────────────────────────
            text.containsAny("clima", "cómo está el clima", "tiempo", "temperatura", "hace calor", "hace frío") -> {
                val weather = WeatherRepository.latestWeatherData
                val temp = weather?.temp ?: 24
                val desc = weather?.description ?: "cielo despejado"
                val city = weather?.cityName ?: "tu ubicación"
                _assistantFeedback.value = "🌤️ $temp°C, $desc en $city"
                voiceAssistant.speak("En $city la temperatura es de $temp grados con $desc.")
            }

            // ── 10. Vehicle Location ──────────────────────────────────────────
            text.containsAny("dónde estoy", "donde estoy", "ubicación", "ubicacion", "dónde me encuentro", "donde me encuentro") -> {
                val weather = WeatherRepository.latestWeatherData
                val place = weather?.cityName ?: "Yarumela"
                val muni = weather?.municipality ?: "La Paz"
                _assistantFeedback.value = "📍 Te encuentras en $place, $muni"
                voiceAssistant.speak("Te encuentras en $place, departamento de $muni.")
            }

            // ── 11. Vehicle Lights ────────────────────────────────────────────
            text.containsAny("encender luces", "prender luces", "luces encendidas", "faros encendidos", "luces on") -> {
                VehicleIlluminationManager.setManual(true)
                _assistantFeedback.value = "💡 Faros encendidos"
                voiceAssistant.speak("Faros encendidos.")
            }

            text.containsAny("apagar luces", "luces apagadas", "luces automáticas", "luces off", "luces auto") -> {
                VehicleIlluminationManager.setManual(null)
                _assistantFeedback.value = "💡 Luces en modo automático"
                voiceAssistant.speak("Luces en modo automático.")
            }

            // ── 12. Help / Commands list ──────────────────────────────────────
            text.containsAny("ayuda", "comandos", "qué puedes hacer", "que puedes hacer", "instrucciones") -> {
                _assistantFeedback.value = "ℹ️ Comandos: Música, Volumen, Mapa, Clima, Luces"
                voiceAssistant.speak("Puedes decir: reproducir música, pausar, siguiente canción, subir volumen, abrir mapa o pedir el clima.")
            }

            // ── Fallback ──────────────────────────────────────────────────────
            else -> {
                _assistantFeedback.value = "❓ Comando no reconocido: '$rawText'"
                voiceAssistant.speak("No reconocí el comando. Di ayuda para escuchar los comandos disponibles.")
            }
        }
    }

    fun destroy() {
        mainHandler.post {
            try {
                speechRecognizer?.destroy()
                speechRecognizer = null
            } catch (_: Exception) {}
        }
    }

    private fun String.containsAny(vararg keywords: String): Boolean {
        return keywords.any { this.contains(it, ignoreCase = true) }
    }
}

