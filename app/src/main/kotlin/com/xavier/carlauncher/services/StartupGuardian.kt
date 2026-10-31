package com.xavier.carlauncher.services

import android.app.ActivityManager
import android.content.Context
import android.content.Intent
import android.util.Log
import com.xavier.carlauncher.MainActivity
import com.xavier.carlauncher.system.RootHelper
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * StartupGuardian guarantees that on vehicle boot or wake (ACC ON),
 * even if the OEM music player (com.syu.music.MAct) opens to start playback,
 * XavierCarOS immediately reclaims the foreground so the dashboard is visible
 * while music plays continuously in the background.
 */
object StartupGuardian {
    private const val TAG = "StartupGuardian"
    private var guardianJob: Job? = null
    private val guardianScope = CoroutineScope(Dispatchers.IO)

    @Volatile
    private var lastStartupTimeMs = 0L

    @Volatile
    private var userRequestedPlayer = false

    /**
     * Call when the user intentionally taps to open the factory music player UI.
     * Cancels auto-reclaim so the player stays visible on screen.
     */
    fun cancel() {
        Log.d(TAG, "StartupGuardian canceled by user request to open player")
        userRequestedPlayer = true
        guardianJob?.cancel()
    }

    /**
     * Engages the startup guardian for a 9-second window following vehicle power/wake.
     */
    fun start(context: Context) {
        userRequestedPlayer = false
        val now = System.currentTimeMillis()
        if (now - lastStartupTimeMs < 4000L) {
            Log.d(TAG, "StartupGuardian already active within recent window; continuing")
            return
        }
        lastStartupTimeMs = now

        guardianJob?.cancel()
        guardianJob = guardianScope.launch {
            Log.i(TAG, "StartupGuardian engaged for 9-second vehicle wake window")
            val startTime = System.currentTimeMillis()
            var reclaimedCount = 0

            while (System.currentTimeMillis() - startTime < 9000L) {
                if (userRequestedPlayer) {
                    Log.d(TAG, "StartupGuardian aborted because user opened player intentionally")
                    break
                }

                delay(250L)

                val shouldReclaim = checkShouldReclaim(context)
                if (shouldReclaim && !userRequestedPlayer) {
                    Log.i(TAG, "OEM music player or settings detected in foreground during startup. Reclaiming foreground for XavierCarOS!")
                    // Allow 300ms for OEM player audio pipeline initialization if it just launched
                    delay(300L)
                    bringLauncherToFront(context)
                    try {
                        context.sendBroadcast(Intent("com.syu.music.stop"))
                    } catch (_: Exception) {}
                    reclaimedCount++
                    delay(700L) // Settle delay
                }
            }
            Log.i(TAG, "StartupGuardian window finished (reclaimed foreground $reclaimedCount times)")
        }
    }

    private suspend fun checkShouldReclaim(context: Context): Boolean {
        // Fast path 1: Check running tasks via ActivityManager
        try {
            val am = context.getSystemService(Context.ACTIVITY_SERVICE) as? ActivityManager
            val tasks = am?.getRunningTasks(1)
            if (!tasks.isNullOrEmpty()) {
                val topPkg = tasks[0].topActivity?.packageName ?: ""
                val topCls = tasks[0].topActivity?.className ?: ""
                if (topPkg == "com.syu.music" || topCls.contains("Settings\$DateTimeSettingsActivity")) {
                    return true
                }
            }
        } catch (_: Exception) {}

        // Fast path 2: Check mCurrentFocus via dumpsys if MainActivity is not resumed
        if (!MainActivity.isActivityResumed) {
            val result = RootHelper.execRoot("dumpsys window windows | grep mCurrentFocus", timeoutMs = 600L)
            if (result.success) {
                val out = result.output
                if (out.contains("com.syu.music") || out.contains("DateTimeSettingsActivity")) {
                    return true
                }
            }
        }

        return false
    }

    fun bringLauncherToFront(context: Context) {
        try {
            val intent = Intent(context, MainActivity::class.java).apply {
                action = Intent.ACTION_MAIN
                addCategory(Intent.CATEGORY_HOME)
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or
                        Intent.FLAG_ACTIVITY_REORDER_TO_FRONT or
                        Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED or
                        Intent.FLAG_ACTIVITY_SINGLE_TOP
            }
            context.startActivity(intent)
        } catch (e: Exception) {
            Log.w(TAG, "Failed to start MainActivity intent", e)
        }

        // Guaranteed foreground elevation via root
        RootHelper.execRootInternal(
            "am start -c android.intent.category.HOME -a android.intent.action.MAIN",
            timeoutMs = 1200L
        )
    }
}

