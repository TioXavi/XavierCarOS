package com.xavier.carlauncher.voice

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioManager
import android.os.Build
import android.os.Bundle
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.speech.tts.Voice
import android.util.Log
import com.xavier.carlauncher.canbus.DoorState
import com.xavier.carlauncher.weather.WeatherRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.Locale

/**
 * Intelligent Automotive Voice Assistant for XavierCarOS.
 * Provides a warm Colombian female voice synthesized via Android's Text-To-Speech engine.
 * Announces ignition greeting ("Bienvenido Xavier") and real-time door notifications.
 */
class CarVoiceAssistant(private val context: Context) {

    companion object {
        private const val TAG = "CarVoiceAssistant"
        private const val UTTERANCE_ID = "xavier_car_voice"

        @Volatile
        private var instance: CarVoiceAssistant? = null

        /**
         * Persistent ignition flag.
         * Ensures the welcome greeting and reminder are spoken strictly ONCE per vehicle
         * ignition/session, and NEVER when pressing the HOME button or switching apps.
         */
        @Volatile
        var hasGreetedThisIgnition: Boolean = false

        fun getInstance(context: Context): CarVoiceAssistant {
            return instance ?: synchronized(this) {
                instance ?: CarVoiceAssistant(context.applicationContext).also { instance = it }
            }
        }
    }

    private var tts: TextToSpeech? = null
    private var isInitialized = false
    private var pendingGreeting = false

    // Debounce tracking
    private var lastSpokenText: String = ""
    private var lastSpokenTimestamp: Long = 0L
    private var lastWakeGreetingTimestamp: Long = 0L

    // Coroutine scope for delayed reminders (1m30s seatbelt + weather)
    private val assistantScope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    private var beltJob: Job? = null

    init {
        initTts(context)
    }

    private fun initTts(context: Context) {
        tts = TextToSpeech(context, { status ->
            if (status == TextToSpeech.SUCCESS) {
                configureVoice()
                isInitialized = true
                Log.i(TAG, "Google TTS initialized successfully with Colombian female voice tuning")
                if (pendingGreeting) {
                    greetXavier()
                    pendingGreeting = false
                }
            } else {
                Log.e(TAG, "Google TTS initialization failed with status: $status, attempting default engine fallback")
                tts = TextToSpeech(context) { fallbackStatus ->
                    if (fallbackStatus == TextToSpeech.SUCCESS) {
                        configureVoice()
                        isInitialized = true
                        Log.i(TAG, "Fallback TTS initialized successfully")
                        if (pendingGreeting) {
                            greetXavier()
                            pendingGreeting = false
                        }
                    }
                }
            }
        }, "com.google.android.tts")
    }

    private fun configureVoice() {
        val engine = tts ?: return

        // 1. Set language to Spanish (Colombia)
        val localeColombia = Locale("es", "CO")
        val langResult = engine.setLanguage(localeColombia)
        if (langResult == TextToSpeech.LANG_MISSING_DATA || langResult == TextToSpeech.LANG_NOT_SUPPORTED) {
            Log.w(TAG, "es_CO missing data or not supported, falling back to general Spanish")
            engine.setLanguage(Locale("es", "ES"))
        }

        // 2. Tune Pitch and Cadence for a pleasant, natural Colombian female tone
        engine.setPitch(1.18f)        // Slightly higher pitch for feminine tone
        engine.setSpeechRate(0.96f)     // Natural, fluid conversational tempo

        // 3. Audio attributes for automotive media stream (plays through main car speakers)
        val audioAttributes = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_MEDIA)
            .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
            .build()
        engine.setAudioAttributes(audioAttributes)

        // 4. Try selecting a female voice if available
        try {
            val voices = engine.voices
            if (!voices.isNullOrEmpty()) {
                val femaleVoice = voices.firstOrNull { v ->
                    val name = v.name.lowercase()
                    val isSpanish = v.locale.language.equals("es", ignoreCase = true)
                    val isColombia = v.locale.country.equals("CO", ignoreCase = true)
                    val isFemale = name.contains("female") || name.contains("fem") || name.contains("woman") || name.contains("zira") || name.contains("network")
                    isSpanish && (isColombia || isFemale)
                } ?: voices.firstOrNull { it.locale.language.equals("es", ignoreCase = true) }

                if (femaleVoice != null) {
                    engine.voice = femaleVoice
                    Log.i(TAG, "Selected voice: ${femaleVoice.name} (${femaleVoice.locale})")
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "Error selecting specific voice", e)
        }
    }

    /**
     * Speaks the given phrase using the Colombian female voice.
     */
    fun speak(text: String, queueMode: Int = TextToSpeech.QUEUE_FLUSH) {
        val now = System.currentTimeMillis()
        if (text == lastSpokenText && (now - lastSpokenTimestamp) < 2500L) {
            return // Debounce duplicate phrase within 2.5 seconds
        }

        lastSpokenText = text
        lastSpokenTimestamp = now

        if (!isInitialized) {
            Log.d(TAG, "TTS not ready yet, queuing: $text")
            return
        }

        try {
            val params = Bundle()
            params.putString(TextToSpeech.Engine.KEY_PARAM_UTTERANCE_ID, UTTERANCE_ID)
            params.putInt(TextToSpeech.Engine.KEY_PARAM_STREAM, AudioManager.STREAM_MUSIC)
            tts?.speak(text, queueMode, params, UTTERANCE_ID)
            Log.d(TAG, "Speaking: $text")
        } catch (e: Exception) {
            Log.e(TAG, "Error during speech synthesis", e)
        }
    }

    /**
     * Vehicle Ignition / Cold Boot / Wake handler:
     * Greets Xavier strictly ONCE per vehicle power cycle.
     * Guaranteed to NEVER repeat when pressing the HOME button or switching between apps.
     */
    fun onVehicleStartupOrWake(weatherDescription: String? = null, weatherTemp: Int? = null) {
        if (hasGreetedThisIgnition) {
            Log.d(TAG, "Ignition greeting already delivered for this drive. Skipping (No repeat on Home press).")
            return
        }
        hasGreetedThisIgnition = true
        Log.i(TAG, "Vehicle startup / ignition detected: greeting Xavier once!")

        greetXavier()

        // Cancel previous pending reminder if any, and schedule 90s reminder
        beltJob?.cancel()
        beltJob = assistantScope.launch {
            delay(90_000L) // 1 minute and 30 seconds
            val latestWeather = WeatherRepository.latestWeatherData
            val desc = weatherDescription ?: latestWeather?.description ?: ""
            val temp = weatherTemp ?: latestWeather?.temp ?: 0
            announceBeltAndWeather(desc, temp)
        }
    }

    /**
     * Called when the vehicle turns off or enters deep sleep (ACC OFF).
     * Resets the flag so the next time the car is powered on, the welcome greeting plays again.
     */
    fun onVehiclePowerOff() {
        Log.i(TAG, "Vehicle power off (ACC OFF / deep sleep): resetting greeting flag for next ignition")
        hasGreetedThisIgnition = false
        beltJob?.cancel()
    }

    fun onScreenWakeOrResume(weatherDescription: String? = null, weatherTemp: Int? = null) {
        onVehicleStartupOrWake(weatherDescription, weatherTemp)
    }

    /**
     * Ignition / Startup welcome greeting with dynamic time of day.
     * Pronounces phonetically as "Zavier" with Colombian female TTS voice.
     */
    fun greetXavier() {
        if (!isInitialized) {
            pendingGreeting = true
            return
        }

        val hour = java.util.Calendar.getInstance().get(java.util.Calendar.HOUR_OF_DAY)
        val timeGreeting = when (hour) {
            in 5..11 -> "Buenos días"
            in 12..18 -> "Buenas tardes"
            else -> "Buenas noches"
        }

        val prefs = context.getSharedPreferences("xavier_user_prefs", Context.MODE_PRIVATE)
        val customName = prefs.getString("user_custom_name", "Xavier") ?: "Xavier"
        val spokenName = if (customName.equals("Xavier", ignoreCase = true)) "Zavier" else customName

        val message = "Hola, $spokenName. $timeGreeting, ¡Bienvenido, $spokenName! Estamos listos para conducir."
        speak(message)
    }

    /**
     * Cordial announcement after 1m 30s:
     * "Recuerda usar el cinturón de seguridad" followed cordially by the weather forecast for today.
     */
    fun announceBeltAndWeather(description: String = "", temp: Int = 0) {
        val weatherClause = if (description.isNotBlank() && temp != 0) {
            "Para el día de hoy se pronostica $description, con una temperatura de $temp grados centígrados."
        } else {
            "Te deseamos un excelente viaje."
        }

        val message = "Recuerda usar el cinturón de seguridad. $weatherClause"
        speak(message)
    }

    /**
     * Announces opening of individual vehicle doors in real-time.
     */
    fun onDoorStateTransition(previous: DoorState, current: DoorState) {
        val messages = mutableListOf<String>()

        if (!previous.frontLeft && current.frontLeft) {
            messages.add("Puerta del conductor abierta")
        }
        if (!previous.frontRight && current.frontRight) {
            messages.add("Puerta del copiloto abierta")
        }
        if (!previous.rearLeft && current.rearLeft) {
            messages.add("Puerta trasera izquierda abierta")
        }
        if (!previous.rearRight && current.rearRight) {
            messages.add("Puerta trasera derecha abierta")
        }
        if (!previous.hood && current.hood) {
            messages.add("Capó del motor abierto")
        }
        if (!previous.trunk && current.trunk) {
            messages.add("Maletero abierto")
        }

        if (messages.isNotEmpty()) {
            val combined = messages.joinToString(". ")
            speak(combined)
        }
    }

    fun shutdown() {
        beltJob?.cancel()
        try {
            tts?.stop()
            tts?.shutdown()
            tts = null
            isInitialized = false
        } catch (e: Exception) {
            Log.w(TAG, "Error shutting down TTS", e)
        }
    }
}

