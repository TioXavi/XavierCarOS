package com.xavier.carlauncher.services

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import com.xavier.carlauncher.R
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * Foreground service that monitors ACC ON / ACC OFF signals broadcast by the
 * head unit's MCU (Microntek, SYU, TopWay variants are all handled).
 *
 * On ACC OFF: pauses GPS updates, stops map rendering, reduces refresh rates.
 * On ACC ON:  restores normal operation.
 *
 * Must be declared in AndroidManifest.xml:
 *   <service android:name=".services.AccOffService"
 *            android:foregroundServiceType="specialUse"
 *            android:exported="false" />
 */
class AccOffService : Service() {

    companion object {
        private const val TAG = "AccOffService"
        private const val CHANNEL_ID = "xavier_car_system"
        private const val CHANNEL_NAME = "Xavier Car System"
        private const val NOTIF_ID = 1001

        // ── Local broadcast actions that downstream components listen to ──
        const val ACTION_ACC_OFF        = "com.xavier.carlauncher.ACC_OFF"
        const val ACTION_ACC_ON         = "com.xavier.carlauncher.ACC_ON"
        const val ACTION_PAUSE_GPS      = "com.xavier.carlauncher.PAUSE_GPS"
        const val ACTION_RESUME_GPS     = "com.xavier.carlauncher.RESUME_GPS"
        const val ACTION_REDUCE_REFRESH = "com.xavier.carlauncher.REDUCE_REFRESH"
        const val ACTION_RESTORE_REFRESH = "com.xavier.carlauncher.RESTORE_REFRESH"
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Broadcast receiver — handles all known ACC OFF / ON & Screen wake variants
    // ─────────────────────────────────────────────────────────────────────────
    private val accReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            when (intent?.action) {
                // ── ACC OFF / Sleep signals ───────────────────────────────
                Intent.ACTION_SCREEN_OFF,
                "com.syu.ms.accoff",
                "com.syu.accoff",
                "com.syu.acc_off",
                "syu.intent.action.accoff",
                "com.syu.ms.screen_off",
                "com.microntek.accoff",
                "com.tw.acc_off",
                "android.intent.action.ACTION_SHUTDOWN",
                "com.android.internal.intent.action.REQUEST_SHUTDOWN" -> {
                    Log.i(TAG, "ACC OFF / SCREEN OFF received via: ${intent.action}")
                    handleAccOff()
                }

                // ── ACC ON / Wake / Ignition signals ───────────────────────
                Intent.ACTION_SCREEN_ON,
                Intent.ACTION_USER_PRESENT,
                Intent.ACTION_BOOT_COMPLETED,
                "com.syu.ms.accon",
                "com.syu.accon",
                "com.syu.acc_on",
                "syu.intent.action.accon",
                "com.syu.ms.screen_on",
                "com.microntek.accon",
                "com.tw.acc_on" -> {
                    Log.i(TAG, "ACC ON / SCREEN ON received via: ${intent.action}")
                    handleAccOn()
                }
            }
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Lifecycle
    // ─────────────────────────────────────────────────────────────────────────
    override fun onCreate() {
        super.onCreate()
        Log.d(TAG, "AccOffService created")

        createNotificationChannel()
        try {
            startForeground(NOTIF_ID, buildNotification("XavierCarOS is running"))
            Log.d(TAG, "AccOffService startForeground initialized")
        } catch (e: Exception) {
            Log.e(TAG, "AccOffService startForeground failed in onCreate", e)
        }

        registerAccReceiver()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.d(TAG, "AccOffService started")
        try {
            startForeground(NOTIF_ID, buildNotification("XavierCarOS is running"))
        } catch (e: Exception) {
            Log.e(TAG, "AccOffService startForeground failed in onStartCommand", e)
        }
        return START_STICKY   // Restart if killed by system
    }

    override fun onBind(intent: Intent?): IBinder? = null  // Not a bound service

    override fun onDestroy() {
        super.onDestroy()
        Log.d(TAG, "AccOffService destroyed — unregistering receiver")
        try {
            unregisterReceiver(accReceiver)
        } catch (e: IllegalArgumentException) {
            Log.w(TAG, "Receiver was not registered: ${e.message}")
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Receiver registration
    // ─────────────────────────────────────────────────────────────────────────
    private fun registerAccReceiver() {
        val filter = IntentFilter().apply {
            // Android standard Screen and Power actions
            addAction(Intent.ACTION_SCREEN_ON)
            addAction(Intent.ACTION_SCREEN_OFF)
            addAction(Intent.ACTION_USER_PRESENT)
            addAction("com.android.internal.intent.action.REQUEST_SHUTDOWN")
            addAction("android.intent.action.ACTION_SHUTDOWN")
            addAction("android.intent.action.BOOT_COMPLETED")

            // SYU (UIS7862 / SC9863a FYT Head Unit)
            addAction("com.syu.ms.accoff")
            addAction("com.syu.ms.accon")
            addAction("com.syu.accoff")
            addAction("com.syu.accon")
            addAction("com.syu.acc_off")
            addAction("com.syu.acc_on")
            addAction("syu.intent.action.accoff")
            addAction("syu.intent.action.accon")
            addAction("com.syu.ms.screen_off")
            addAction("com.syu.ms.screen_on")

            // Microntek head units
            addAction("com.microntek.accoff")
            addAction("com.microntek.accon")

            // TopWay / TW head units
            addAction("com.tw.acc_off")
            addAction("com.tw.acc_on")
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(accReceiver, filter, Context.RECEIVER_EXPORTED)
        } else {
            registerReceiver(accReceiver, filter)
        }

        Log.d(TAG, "ACC broadcast receiver registered with comprehensive Screen & Wake intents")
    }

    // ─────────────────────────────────────────────────────────────────────────
    // ACC OFF handler — reduce power usage
    // ─────────────────────────────────────────────────────────────────────────
    private fun handleAccOff() {
        Log.i(TAG, "Handling ACC OFF — reducing resource usage")

        // Update notification
        val nm = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        nm.notify(NOTIF_ID, buildNotification("XavierCarOS — ACC OFF (standby deep sleep)"))

        // 1. Hardware GPS chip power down: release location manager and satellite listeners
        com.xavier.carlauncher.location.VehicleLocationManager.stopListening()

        // 2. Broadcast local intent: pause GPS & reduce UI refresh
        sendLocalBroadcast(ACTION_PAUSE_GPS)
        sendLocalBroadcast(ACTION_REDUCE_REFRESH)
        sendLocalBroadcast(ACTION_ACC_OFF)

        // 3. Reset voice greeting state so next ignition greets again
        com.xavier.carlauncher.voice.CarVoiceAssistant.getInstance(applicationContext).onVehiclePowerOff()

        // 4. Pause native audio playback
        com.xavier.carlauncher.media.XavierLocalPlayer.pause()
    }

    // ─────────────────────────────────────────────────────────────────────────
    // ACC ON handler — restore normal operation
    // ─────────────────────────────────────────────────────────────────────────
    private fun handleAccOn() {
        Log.i(TAG, "Handling ACC ON — restoring normal operation")

        // Update notification
        val nm = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        nm.notify(NOTIF_ID, buildNotification("XavierCarOS is running"))

        // 1. Restore hardware GPS listeners
        com.xavier.carlauncher.location.VehicleLocationManager.startListening()

        // 2. Broadcast local intent: resume GPS & restore UI refresh
        sendLocalBroadcast(ACTION_RESUME_GPS)
        sendLocalBroadcast(ACTION_RESTORE_REFRESH)
        sendLocalBroadcast(ACTION_ACC_ON)

        // Voice greeting upon vehicle ignition or wake (strictly once per ignition cycle)
        com.xavier.carlauncher.voice.CarVoiceAssistant.getInstance(applicationContext).onVehicleStartupOrWake()

        // Resume native audio playback immediately on vehicle wake / ignition
        com.xavier.carlauncher.media.XavierLocalPlayer.onVehicleIgnition()

        // Engage Startup Guardian to ensure launcher reclaims foreground immediately on wake
        com.xavier.carlauncher.services.StartupGuardian.start(applicationContext)
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Helpers
    // ─────────────────────────────────────────────────────────────────────────
    private fun sendLocalBroadcast(action: String) {
        val intent = Intent(action).apply {
            setPackage(packageName)   // Restrict to this app for security
        }
        sendBroadcast(intent)
        Log.d(TAG, "Local broadcast sent: $action")
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_LOW   // Silent — no sound/vibration
            ).apply {
                description = "XavierCarOS system service — manages ACC power states"
                setShowBadge(false)
            }
            val nm = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            nm.createNotificationChannel(channel)
        }
    }

    private fun buildNotification(text: String): Notification {
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("XavierCarOS")
            .setContentText(text)
            .setSmallIcon(R.drawable.ic_stat_car)
            .setOngoing(true)
            .setSilent(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .build()
    }
}
