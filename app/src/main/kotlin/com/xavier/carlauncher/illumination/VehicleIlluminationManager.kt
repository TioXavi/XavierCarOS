package com.xavier.carlauncher.illumination

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.res.Configuration
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Build
import android.util.Log
import com.xavier.carlauncher.weather.WeatherRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.util.Calendar

/**
 * Senior Automotive Illumination & Darkness Manager for XavierCarOS.
 *
 * Detects whether vehicle lights should be rendered ON based on 3 complementary sensors:
 * 1. Physical Ambient Light Sensor (`Sensor.TYPE_LIGHT` on chip LTR388ALS):
 *    Detects real-time physical lux level (tunnels, garage, evening dusk < 25 lux).
 * 2. Hardware Headlight/Illumination Wire (FYT ILLUM):
 *    Intercepts vehicle headlights / parking lights trigger broadcasts (com.syu.illum.on, HEADLIGHT_ON).
 * 3. Solar Astronomical Night Calculator:
 *    Calculates whether current time is post-sunset or pre-sunrise at the vehicle's real coordinates.
 *
 * When ANY of these conditions indicate darkness or headlights ON, [isLightsOn] emits `true`,
 * seamlessly transitioning the car graphic to the headlights-illuminated render.
 */
object VehicleIlluminationManager {

    private const val TAG = "VehicleIllumination"

    // Lux thresholds with hysteresis to prevent flickering under streetlights
    private const val LUX_DARK_THRESHOLD = 25f
    private const val LUX_BRIGHT_THRESHOLD = 38f

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private var monitorJob: Job? = null

    private var appContext: Context? = null
    private var sensorManager: SensorManager? = null
    private var lightSensor: Sensor? = null

    private val _isLightsOn = MutableStateFlow(false)
    val isLightsOn: StateFlow<Boolean> = _isLightsOn.asStateFlow()

    private val _isDark = MutableStateFlow(false)
    val isDark: StateFlow<Boolean> = _isDark.asStateFlow()

    private val _isHeadlightsOn = MutableStateFlow(false)
    val isHeadlightsOn: StateFlow<Boolean> = _isHeadlightsOn.asStateFlow()

    private val _currentLux = MutableStateFlow(0f)
    val currentLux: StateFlow<Float> = _currentLux.asStateFlow()

    private val _manualOverride = MutableStateFlow<Boolean?>(null)
    val manualOverride: StateFlow<Boolean?> = _manualOverride.asStateFlow()

    private val _statusDescription = MutableStateFlow("Iniciando detección...")
    val statusDescription: StateFlow<String> = _statusDescription.asStateFlow()

    // ── 1. Hardware Light Sensor Listener ────────────────────────────────────
    private val lightSensorListener = object : SensorEventListener {
        override fun onSensorChanged(event: SensorEvent?) {
            if (event?.sensor?.type == Sensor.TYPE_LIGHT) {
                val lux = event.values.getOrNull(0) ?: return
                _currentLux.value = lux

                val currentDark = _isDark.value
                val newDark = when {
                    lux <= LUX_DARK_THRESHOLD -> true
                    lux >= LUX_BRIGHT_THRESHOLD -> false
                    else -> currentDark
                }

                if (newDark != currentDark) {
                    _isDark.value = newDark
                    Log.i(TAG, "Hardware light sensor transitioned: lux=$lux -> isDark=$newDark")
                    reevaluateState()
                }
            }
        }

        override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
    }

    // ── 2. Headlight / Illumination Broadcast Receiver ───────────────────────
    private val illumBroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            val action = intent?.action ?: return
            Log.d(TAG, "Illumination broadcast received: $action")
            when (action) {
                "com.syu.illum.on",
                "android.intent.action.HEADLIGHT_ON" -> {
                    _isHeadlightsOn.value = true
                    reevaluateState()
                }
                "com.syu.illum.off",
                "android.intent.action.HEADLIGHT_OFF" -> {
                    _isHeadlightsOn.value = false
                    reevaluateState()
                }
                "com.fyt.system.illum" -> {
                    val state = intent.getBooleanExtra("state", false) ||
                                intent.getIntExtra("state", 0) == 1
                    _isHeadlightsOn.value = state
                    reevaluateState()
                }
            }
        }
    }

    /**
     * Initializes the illumination detection pipeline.
     */
    fun initialize(context: Context) {
        if (appContext != null) return
        val app = context.applicationContext
        appContext = app

        // 1. Setup hardware light sensor
        sensorManager = app.getSystemService(Context.SENSOR_SERVICE) as? SensorManager
        lightSensor = sensorManager?.getDefaultSensor(Sensor.TYPE_LIGHT)

        if (lightSensor != null) {
            sensorManager?.registerListener(
                lightSensorListener,
                lightSensor,
                SensorManager.SENSOR_DELAY_NORMAL
            )
            Log.i(TAG, "Hardware ambient light sensor registered: ${lightSensor?.name}")
        } else {
            Log.w(TAG, "No hardware light sensor found on this head unit; using solar calculation")
        }

        // 2. Setup illumination broadcast receiver
        try {
            val filter = IntentFilter().apply {
                addAction("com.syu.illum.on")
                addAction("com.syu.illum.off")
                addAction("com.fyt.system.illum")
                addAction("android.intent.action.HEADLIGHT_ON")
                addAction("android.intent.action.HEADLIGHT_OFF")
            }
            app.registerReceiver(illumBroadcastReceiver, filter)
        } catch (e: Exception) {
            Log.w(TAG, "Cannot register illumBroadcastReceiver", e)
        }

        // 3. Initial evaluation
        reevaluateState()

        // 4. Start periodic solar evaluation loop (every 15 seconds)
        startMonitoringLoop()
    }

    private fun startMonitoringLoop() {
        monitorJob?.cancel()
        monitorJob = scope.launch(Dispatchers.Default) {
            while (isActive) {
                delay(15_000L)
                reevaluateState()
            }
        }
    }

    /**
     * Re-evaluates whether lights should be ON based on:
     * - Manual override (if set)
     * - Headlights wire signal (ILLUM)
     * - Physical ambient light sensor (lux < 25)
     * - Solar night / sunset time
     * - Android system night mode
     */
    fun reevaluateState() {
        val override = _manualOverride.value
        if (override != null) {
            _isLightsOn.value = override
            _statusDescription.value = if (override) "Luces forzadas (Manual)" else "Luces apagadas (Manual)"
            return
        }

        val headlights = _isHeadlightsOn.value
        val sensorDark = _isDark.value
        val solarNight = isSolarNight()
        val sysNight = isSystemNightMode()

        val shouldBeOn = headlights || sensorDark || solarNight || sysNight
        _isLightsOn.value = shouldBeOn

        val reason = when {
            headlights -> "Faros de vehículo encendidos"
            sensorDark -> "Sensor de luz: Oscuridad (${_currentLux.value.toInt()} lux)"
            solarNight -> "Horario nocturno / Puesta de sol"
            sysNight -> "Modo Noche de sistema"
            else -> "Luz diurna (Faros apagados)"
        }
        _statusDescription.value = reason
        Log.d(TAG, "Illumination evaluated: shouldBeOn=$shouldBeOn ($reason)")
    }

    /**
     * Toggles manual override: Auto -> Forced ON -> Forced OFF -> Auto.
     */
    fun toggleManual(): Boolean? {
        val next = when (_manualOverride.value) {
            null -> true
            true -> false
            false -> null
        }
        _manualOverride.value = next
        reevaluateState()
        return next
    }

    fun setManual(value: Boolean?) {
        _manualOverride.value = value
        reevaluateState()
    }

    /**
     * Determines whether it is currently night based on:
     * - OpenWeatherMap sunrise/sunset timestamps if available
     * - Fallback to vehicle local solar hours (17:45 - 05:45 in Honduras)
     */
    private fun isSolarNight(): Boolean {
        val weather = WeatherRepository.latestWeatherData
        if (weather != null && !weather.iconCode.isNullOrEmpty()) {
            if (weather.iconCode.endsWith("n", ignoreCase = true)) {
                return true
            }
            if (weather.iconCode.endsWith("d", ignoreCase = true)) {
                return false
            }
        }

        // Fallback: Local hour in Honduras (CST, UTC-6)
        val cal = Calendar.getInstance()
        val hour = cal.get(Calendar.HOUR_OF_DAY)
        val minute = cal.get(Calendar.MINUTE)
        val timeInMin = hour * 60 + minute

        // Night in Honduras is approximately from 17:45 (5:45 PM) to 05:45 (5:45 AM)
        val sunsetMin = 17 * 60 + 45
        val sunriseMin = 5 * 60 + 45

        return timeInMin >= sunsetMin || timeInMin < sunriseMin
    }

    private fun isSystemNightMode(): Boolean {
        val ctx = appContext ?: return false
        val currentNightMode = ctx.resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK
        return currentNightMode == Configuration.UI_MODE_NIGHT_YES
    }
}

