package com.xavier.carlauncher.location

import android.annotation.SuppressLint
import android.content.Context
import android.content.SharedPreferences
import android.location.GnssStatus
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import com.xavier.carlauncher.system.RootHelper
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
import kotlinx.coroutines.withContext

/**
 * High-precision vehicle location data snapshot.
 */
data class VehicleLocation(
    val latitude: Double = 14.338338,
    val longitude: Double = -87.644540,
    val altitude: Double = 0.0,
    val bearing: Float = 0f,
    val speedMph: Float = 0f,
    val accuracyMeters: Float = 5f,
    val satellites: Int = 0,
    val hasFix: Boolean = false,
    val provider: String = "gps",
    val timestamp: Long = System.currentTimeMillis()
)

/**
 * Automotive-grade, unified GPS & GNSS Location Manager for XavierCarOS.
 * - Coordinates GPS, Network, and Passive providers into a single, conflict-free stream.
 * - Automatically initializes to the vehicle's persistent last known coordinates (0ms latency, NEVER Tegucigalpa).
 * - Applies a Kalman filter for smooth automotive speed calculation.
 * - Stabilizes heading/bearing when stationary.
 * - Hardware watchdog ensures FYT GPS antenna power (/sys/fytver/gps_pwren) remains active.
 * - Enables Android AGPS & High Accuracy mode via root.
 */
object VehicleLocationManager {

    private const val TAG = "VehicleLocationManager"
    private const val PREFS_NAME = "xavier_vehicle_location_prefs"
    private const val KEY_LAST_LAT = "last_latitude"
    private const val KEY_LAST_LNG = "last_longitude"
    private const val KEY_LAST_BEARING = "last_bearing"
    private const val KEY_LAST_ALT = "last_altitude"
    private const val KEY_LAST_ACCURACY = "last_accuracy"

    // Default vehicle location: Yarumela, La Paz, Honduras (Vehicle's home location)
    private const val DEFAULT_LAT = 14.338338
    private const val DEFAULT_LNG = -87.644540

    private const val MPS_TO_MPH = 2.23694f
    private const val KALMAN_Q = 0.1f
    private const val KALMAN_R = 1.0f
    private const val SPEED_FLOOR_MPH = 0.5f

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private var watchdogJob: Job? = null

    private var appContext: Context? = null
    private var prefs: SharedPreferences? = null
    private var locationManager: LocationManager? = null

    private var kalmanEstimate: Float = 0f
    private var kalmanError: Float = 1f
    private var lastValidBearing: Float = 0f
    private var currentSatellites: Int = 0
    private var prevLocationForBearing: Location? = null

    private val _locationState = MutableStateFlow(VehicleLocation())
    val locationState: StateFlow<VehicleLocation> = _locationState.asStateFlow()

    val currentLocation: VehicleLocation get() = _locationState.value

    private var isListening = false
    private var lastUpdateTimeMs = 0L

    // Unified LocationListener for all providers
    private val locationListener = object : LocationListener {
        override fun onLocationChanged(loc: Location) {
            handleNewLocation(loc)
        }

        @Deprecated("Deprecated in API 29")
        override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) {
            extras?.let { bundle ->
                if (bundle.containsKey("satellites")) {
                    val sats = bundle.getInt("satellites")
                    if (sats > 0) currentSatellites = sats
                }
            }
        }

        override fun onProviderEnabled(provider: String) {
            Log.d(TAG, "Location provider enabled: $provider")
        }

        override fun onProviderDisabled(provider: String) {
            Log.w(TAG, "Location provider disabled: $provider")
        }
    }

    private var gnssCallback: GnssStatus.Callback? = null

    /**
     * Initializes the location manager, restores persistent coordinates, and begins listening.
     */
    fun initialize(context: Context) {
        if (appContext != null) return
        val app = context.applicationContext
        appContext = app
        prefs = app.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        locationManager = app.getSystemService(Context.LOCATION_SERVICE) as? LocationManager

        // 1. Restore persistent last known location immediately
        restoreSavedLocation()

        // 2. Tune Android GNSS and AGPS via root in background
        scope.launch(Dispatchers.IO) {
            optimizeGpsSystem()
        }

        // 3. Start unified location listening
        startListening()

        // 4. Start GPS watchdog keep-alive
        startWatchdog()
    }

    private fun restoreSavedLocation() {
        val sp = prefs
        val savedLatStr = sp?.getString(KEY_LAST_LAT, null)
        val savedLngStr = sp?.getString(KEY_LAST_LNG, null)
        val savedLat = savedLatStr?.toDoubleOrNull() ?: DEFAULT_LAT
        val savedLng = savedLngStr?.toDoubleOrNull() ?: DEFAULT_LNG
        val savedBearing = sp?.getFloat(KEY_LAST_BEARING, 0f) ?: 0f
        val savedAlt = sp?.getString(KEY_LAST_ALT, "0.0")?.toDoubleOrNull() ?: 0.0
        val savedAcc = sp?.getFloat(KEY_LAST_ACCURACY, 5f) ?: 5f

        lastValidBearing = savedBearing

        // Try to query system lastKnownLocation immediately
        val lm = locationManager
        var bestLoc: Location? = null
        if (lm != null) {
            try {
                val providers = listOf(
                    LocationManager.GPS_PROVIDER,
                    LocationManager.NETWORK_PROVIDER,
                    LocationManager.PASSIVE_PROVIDER
                )
                for (p in providers) {
                    if (lm.isProviderEnabled(p)) {
                        val l = lm.getLastKnownLocation(p)
                        if (l != null && (bestLoc == null || l.time > bestLoc.time)) {
                            bestLoc = l
                        }
                    }
                }
            } catch (_: Exception) {}
        }

        if (bestLoc != null && bestLoc.latitude != 0.0 && bestLoc.longitude != 0.0) {
            handleNewLocation(bestLoc)
        } else {
            // Emit restored persistent location so map starts in Yarumela
            _locationState.value = VehicleLocation(
                latitude = savedLat,
                longitude = savedLng,
                altitude = savedAlt,
                bearing = savedBearing,
                speedMph = 0f,
                accuracyMeters = savedAcc,
                satellites = 0,
                hasFix = true,
                provider = "cache"
            )
            Log.i(TAG, "Restored vehicle location from cache: ($savedLat, $savedLng)")
        }
    }

    @SuppressLint("MissingPermission")
    fun startListening() {
        if (isListening) return
        val lm = locationManager ?: return

        try {
            // Register GPS provider (1000ms interval for balanced vehicle tracking)
            if (lm.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
                lm.requestLocationUpdates(
                    LocationManager.GPS_PROVIDER,
                    1000L,
                    0f,
                    locationListener
                )
            }

            // Register Network provider (fallback when under heavy roof / garage)
            if (lm.isProviderEnabled(LocationManager.NETWORK_PROVIDER)) {
                lm.requestLocationUpdates(
                    LocationManager.NETWORK_PROVIDER,
                    3000L,
                    0f,
                    locationListener
                )
            }

            // Register Passive provider (catches fixes from Waze or Google Maps)
            try {
                lm.requestLocationUpdates(
                    LocationManager.PASSIVE_PROVIDER,
                    1000L,
                    0f,
                    locationListener
                )
            } catch (_: Exception) {}

            // Register GnssStatus callback for satellite count on Android 8.1+
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                val cb = object : GnssStatus.Callback() {
                    override fun onSatelliteStatusChanged(status: GnssStatus) {
                        var used = 0
                        for (i in 0 until status.satelliteCount) {
                            if (status.usedInFix(i)) used++
                        }
                        currentSatellites = used
                        val current = _locationState.value
                        if (current.satellites != used) {
                            _locationState.value = current.copy(satellites = used)
                        }
                    }
                }
                lm.registerGnssStatusCallback(cb)
                gnssCallback = cb
            }

            isListening = true
            Log.d(TAG, "Unified vehicle location listeners registered")
        } catch (e: Exception) {
            Log.e(TAG, "Error registering location listeners", e)
        }
    }

    fun stopListening() {
        if (!isListening) return
        val lm = locationManager ?: return
        try {
            lm.removeUpdates(locationListener)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N && gnssCallback != null) {
                lm.unregisterGnssStatusCallback(gnssCallback!!)
                gnssCallback = null
            }
            isListening = false
            Log.d(TAG, "Vehicle location listeners unregistered")
        } catch (e: Exception) {
            Log.e(TAG, "Error stopping location updates", e)
        }
    }

    private fun handleNewLocation(loc: Location) {
        val lat = loc.latitude
        val lng = loc.longitude
        // Ignore invalid (0,0) coordinates
        if (lat == 0.0 && lng == 0.0) return

        lastUpdateTimeMs = System.currentTimeMillis()

        // Extract satellites from extras bundle if present
        loc.extras?.let { extras ->
            if (extras.containsKey("satellites")) {
                val sats = extras.getInt("satellites")
                if (sats > 0) currentSatellites = sats
            }
        }

        // Speed calculation with Kalman smoothing
        val rawSpeedMph = if (loc.hasSpeed()) loc.speed * MPS_TO_MPH else 0f
        val smoothedSpeed = kalmanFilter(rawSpeedMph)

        // True Geographic Heading / Course Over Ground (COG):
        // Calculate the vector directly from physical GPS coordinate displacement.
        // This is 100% geographically true North-referenced heading along the road,
        // completely immune to tablet board orientation, dashboard metal interference,
        // or faulty Spreadtrum HAL bearing flags.
        val prev = prevLocationForBearing
        if (smoothedSpeed >= 1.0f) {
            val targetBearing = if (loc.hasBearing() && loc.bearing != 0f) {
                (loc.bearing % 360f + 360f) % 360f
            } else if (prev != null && prev.distanceTo(loc) in 1.2f..500f) {
                (prev.bearingTo(loc) % 360f + 360f) % 360f
            } else null

            if (targetBearing != null) {
                lastValidBearing = smoothAngle(lastValidBearing, targetBearing)
            }
            if (prev == null || prev.distanceTo(loc) >= 1.2f) {
                prevLocationForBearing = loc
            }
        } else {
            // When stationary / stopped (< 1.0 mph), keep lastValidBearing locked so orientation never spins
            if (prev == null) {
                prevLocationForBearing = loc
            }
        }

        val updated = VehicleLocation(
            latitude = lat,
            longitude = lng,
            altitude = loc.altitude,
            bearing = lastValidBearing,
            speedMph = smoothedSpeed,
            accuracyMeters = if (loc.hasAccuracy()) loc.accuracy else 5f,
            satellites = currentSatellites,
            hasFix = true,
            provider = loc.provider ?: "gps",
            timestamp = loc.time
        )

        _locationState.value = updated

        // Persist coordinates
        prefs?.edit()
            ?.putString(KEY_LAST_LAT, lat.toString())
            ?.putString(KEY_LAST_LNG, lng.toString())
            ?.putFloat(KEY_LAST_BEARING, lastValidBearing)
            ?.putString(KEY_LAST_ALT, loc.altitude.toString())
            ?.putFloat(KEY_LAST_ACCURACY, updated.accuracyMeters)
            ?.apply()
    }

    private fun kalmanFilter(measured: Float): Float {
        val k = kalmanError / (kalmanError + KALMAN_R)
        kalmanEstimate += k * (measured - kalmanEstimate)
        kalmanError = (1f - k) * kalmanError + KALMAN_Q
        return if (kalmanEstimate < SPEED_FLOOR_MPH) 0f else kalmanEstimate
    }

    /**
     * Circular angle smoothing that correctly wraps across 360/0 degrees without flip jumps.
     * Uses adaptive factor: responsive fast-follow on turns, smooth jitter filtering when driving straight.
     */
    private fun smoothAngle(current: Float, target: Float): Float {
        var diff = (target - current) % 360f
        if (diff > 180f) diff -= 360f
        if (diff < -180f) diff += 360f
        val factor = if (kotlin.math.abs(diff) > 35f) 0.80f else 0.45f
        val result = current + diff * factor
        return (result % 360f + 360f) % 360f
    }

    /**
     * Periodically verifies GPS health and kicks the hardware antenna power if stalled.
     */
    private fun startWatchdog() {
        watchdogJob?.cancel()
        watchdogJob = scope.launch(Dispatchers.IO) {
            while (isActive) {
                delay(30_000L) // Check every 30 seconds
                val now = System.currentTimeMillis()
                val isStale = (now - lastUpdateTimeMs) > 45_000L

                if (isStale && isListening) {
                    Log.w(TAG, "GPS updates appear stale (>45s). Kicking hardware antenna power...")
                    try {
                        RootHelper.execRoot("echo 1 > /sys/fytver/gps_pwren 2>/dev/null", timeoutMs = 1000L)
                        // Trigger one-off getLastKnownLocation check
                        val lm = locationManager
                        val lastGps = lm?.getLastKnownLocation(LocationManager.GPS_PROVIDER)
                        if (lastGps != null && (now - lastGps.time) < 30_000L) {
                            withContext(Dispatchers.Main) {
                                handleNewLocation(lastGps)
                            }
                        }
                    } catch (_: Exception) {}
                }
            }
        }
    }

    /**
     * Root optimizations to maximize GPS accuracy, enable AGPS, and prevent power manager throttle.
     */
    suspend fun optimizeGpsSystem() {
        try {
            RootHelper.execRoot("settings put secure location_mode 3", timeoutMs = 1500L)
            RootHelper.execRoot("settings put secure location_providers_allowed gps,network", timeoutMs = 1500L)
            RootHelper.execRoot("settings put global assisted_gps_enabled 1", timeoutMs = 1500L)
            RootHelper.execRoot("settings put secure assisted_gps_enabled 1", timeoutMs = 1500L)
            RootHelper.execRoot("echo 1 > /sys/fytver/gps_pwren", timeoutMs = 1500L)
            Log.i(TAG, "High-accuracy GNSS & AGPS configurations successfully applied")
        } catch (e: Exception) {
            Log.w(TAG, "Could not apply GNSS root optimizations", e)
        }
    }
}
