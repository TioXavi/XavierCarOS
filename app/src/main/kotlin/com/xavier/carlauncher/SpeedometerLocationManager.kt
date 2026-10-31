package com.xavier.carlauncher

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.xavier.carlauncher.location.VehicleLocationManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * SpeedometerViewModel
 *
 * GPS-based speedometer observing the unified [VehicleLocationManager]
 * with Kalman speed smoothing and heading stabilization.
 */
class SpeedometerViewModel(application: Application) : AndroidViewModel(application) {

    private val _speedMph = MutableStateFlow(0f)
    val speedMph: StateFlow<Float> = _speedMph.asStateFlow()

    private val _bearing = MutableStateFlow(0f)
    val bearing: StateFlow<Float> = _bearing.asStateFlow()

    private val _hasGpsFix = MutableStateFlow(false)
    val hasGpsFix: StateFlow<Boolean> = _hasGpsFix.asStateFlow()

    init {
        VehicleLocationManager.initialize(application)
        viewModelScope.launch {
            VehicleLocationManager.locationState.collect { loc ->
                _speedMph.value = loc.speedMph
                _bearing.value = loc.bearing
                _hasGpsFix.value = loc.hasFix
            }
        }
    }

    fun startListening() {
        VehicleLocationManager.startListening()
    }

    fun stopListening() {
        // Shared manager keeps tracking for widgets
    }
}
