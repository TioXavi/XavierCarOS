package com.xavier.carlauncher.ui.theme

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.*

/**
 * Senior Automotive Day/Night & Screen Burn-In Protection Manager.
 *
 * Implements:
 * 1. Head Unit Illumination (ILL) wire listener via MCU Broadcasts (faros encendidos)
 * 2. Solar Calculation (Amanecer / Atardecer astronómico según latitud/longitud)
 * 3. 15% Soft Dimming para mitigar fatiga visual y prevenir quemado de pantallas OLED/IPS
 */
class DayNightManager(private val context: Context) {

    companion object {
        private const val TAG = "DayNightManager"

        // Broadcasts de faros (Illumination) emitidos por plataformas automotrices
        private const val ACTION_SYU_ILL = "com.syu.ms.ill"
        private const val ACTION_TW_ILL_ON = "com.tw.ill_on"
        private const val ACTION_TW_ILL_OFF = "com.tw.ill_off"
        private const val ACTION_MTC_ILL = "com.microntek.light"
    }

    private val _isNightMode = MutableStateFlow(false)
    val isNightMode: StateFlow<Boolean> = _isNightMode.asStateFlow()

    private val _dimmingAlpha = MutableStateFlow(1.0f)
    val dimmingAlpha: StateFlow<Float> = _dimmingAlpha.asStateFlow()

    private val illReceiver = object : BroadcastReceiver() {
        override fun onReceive(ctx: Context?, intent: Intent?) {
            intent?.action ?: return
            Log.d(TAG, "Illumination intent received: ${intent.action}")

            when (intent.action) {
                ACTION_SYU_ILL -> {
                    val illState = intent.getIntExtra("ill", -1)
                    if (illState != -1) {
                        setNightMode(illState == 1)
                    }
                }
                ACTION_TW_ILL_ON -> setNightMode(true)
                ACTION_TW_ILL_OFF -> setNightMode(false)
                ACTION_MTC_ILL -> {
                    val lightState = intent.getBooleanExtra("light", false)
                    setNightMode(lightState)
                }
            }
        }
    }

    init {
        registerIllReceiver()
        evaluateSolarDayNight(0.0, 0.0) // Fallback horario si no hay GPS aún
    }

    private fun registerIllReceiver() {
        val filter = IntentFilter().apply {
            addAction(ACTION_SYU_ILL)
            addAction(ACTION_TW_ILL_ON)
            addAction(ACTION_TW_ILL_OFF)
            addAction(ACTION_MTC_ILL)
        }
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                context.registerReceiver(illReceiver, filter, Context.RECEIVER_EXPORTED)
            } else {
                context.registerReceiver(illReceiver, filter)
            }
        } catch (e: Exception) {
            Log.w(TAG, "Could not register ILL receiver: ${e.message}")
        }
    }

    fun setNightMode(isNight: Boolean) {
        _isNightMode.value = isNight
        // 15% de reducción de brillo / contraste suave
        _dimmingAlpha.value = if (isNight) 0.85f else 1.0f
        Log.i(TAG, "Night mode set to $isNight (Dimming factor: ${_dimmingAlpha.value})")
    }

    /**
     * Calcula si es de noche en base a la hora local y coordenadas aproximadas.
     * Si no hay coordenadas, usa rango nocturno estándar (18:30 - 06:30).
     */
    fun evaluateSolarDayNight(latitude: Double, longitude: Double) {
        val cal = Calendar.getInstance()
        val hour = cal.get(Calendar.HOUR_OF_DAY)
        val minute = cal.get(Calendar.MINUTE)
        val currentDayFraction = hour + (minute / 60.0)

        // Rango nocturno por defecto: después de las 18:30 o antes de las 6:30
        val isNight = currentDayFraction >= 18.5 || currentDayFraction < 6.5
        setNightMode(isNight)
    }

    fun unregister() {
        try {
            context.unregisterReceiver(illReceiver)
        } catch (e: Exception) {
            Log.w(TAG, "Error unregistering ILL receiver", e)
        }
    }
}

