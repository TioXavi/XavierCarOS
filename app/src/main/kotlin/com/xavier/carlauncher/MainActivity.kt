package com.xavier.carlauncher

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.media.AudioManager
import android.os.Bundle
import android.os.IBinder
import android.view.KeyEvent
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.lifecycleScope
import com.xavier.carlauncher.system.RootHelper
import com.xavier.carlauncher.ui.screens.DashboardScreen
import com.xavier.carlauncher.ui.theme.ThemeManager
import com.xavier.carlauncher.ui.theme.XavierCarOSTheme
import com.xavier.carlauncher.voice.CarVoiceAssistant
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    companion object {
        @Volatile
        var isActivityResumed: Boolean = false
            private set
    }

    private lateinit var audioManager: AudioManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Engage Startup Guardian to reclaim foreground immediately when music starts on boot
        com.xavier.carlauncher.services.StartupGuardian.start(applicationContext)

        // Make fullscreen, hide system bars (nav bar + status bar)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        WindowInsetsControllerCompat(window, window.decorView).apply {
            hide(WindowInsetsCompat.Type.systemBars())
            systemBarsBehavior =
                WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        }

        audioManager = getSystemService(Context.AUDIO_SERVICE) as AudioManager

        // Start AccOffService for vehicle power management and deep sleep
        val accIntent = Intent(this, com.xavier.carlauncher.services.AccOffService::class.java)
        try {
            startService(accIntent)
        } catch (e: Exception) {
            android.util.Log.w("MainActivity", "Cannot start AccOffService", e)
        }

        // Start FytMusicService safely
        val fytIntent = Intent(this, com.xavier.carlauncher.media.FytMusicService::class.java)
        try {
            startService(fytIntent)
        } catch (e: Exception) {
            android.util.Log.w("MainActivity", "Cannot start FytMusicService", e)
        }

        // Initialize persistent favorites and lyrics translation pipeline
        com.xavier.carlauncher.media.FavoritesManager.initialize(applicationContext)
        com.xavier.carlauncher.media.LyricsTranslationManager.initialize(applicationContext)

        // Initialize native audio player engine (with instant seeking, USB scanning, and synced lyrics)
        com.xavier.carlauncher.media.XavierLocalPlayer.initialize(applicationContext)

        // Initialize unified automotive GPS location manager
        com.xavier.carlauncher.location.VehicleLocationManager.initialize(applicationContext)

        // Initialize vehicle illumination & darkness detection pipeline
        com.xavier.carlauncher.illumination.VehicleIlluminationManager.initialize(applicationContext)

        // Automatically start native music playback on vehicle startup / ignition
        com.xavier.carlauncher.media.XavierLocalPlayer.onVehicleIgnition()

        // One-time setup on initial install (persisted so it never executes or spams root toasts again)
        val initPrefs = applicationContext.getSharedPreferences("xavier_init_prefs", Context.MODE_PRIVATE)
        if (!initPrefs.getBoolean("initial_setup_completed", false)) {
            initPrefs.edit().putBoolean("initial_setup_completed", true).apply()
            lifecycleScope.launch(kotlinx.coroutines.Dispatchers.IO) {
                if (RootHelper.isRootAvailable()) {
                    RootHelper.performOneTimeSetup(applicationContext)
                }
            }
        }

        // Voice Assistant: Welcome Xavier with warm Colombian female voice on startup (strictly once per ignition session)
        CarVoiceAssistant.getInstance(applicationContext).onVehicleStartupOrWake()

        // Schedule silent background offline map updates on Wi-Fi
        com.xavier.carlauncher.navigation.MapDownloadWorker.schedule(applicationContext)

        // Pre-warm App Drawer cache in background for 0ms instant display
        lifecycleScope.launch(kotlinx.coroutines.Dispatchers.IO) {
            com.xavier.carlauncher.ui.screens.AppDrawerCache.getOrLoad(applicationContext)
        }

        // Initialize active automotive theme
        ThemeManager.initialize(applicationContext)

        setContent {
            val currentTheme by ThemeManager.currentTheme.collectAsState()
            XavierCarOSTheme(theme = currentTheme) {
                DashboardScreen()
            }
        }
    }

    override fun dispatchKeyEvent(event: KeyEvent): Boolean {
        if (event.action == KeyEvent.ACTION_DOWN) {
            if (handleHardwareMediaKey(event.keyCode)) {
                return true
            }
        } else if (event.action == KeyEvent.ACTION_UP) {
            if (isMediaKeyCode(event.keyCode)) {
                return true
            }
        }
        return super.dispatchKeyEvent(event)
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        if (handleHardwareMediaKey(keyCode)) {
            return true
        }
        return super.onKeyDown(keyCode, event)
    }

    private fun isMediaKeyCode(keyCode: Int): Boolean {
        return when (keyCode) {
            KeyEvent.KEYCODE_MEDIA_NEXT,
            KeyEvent.KEYCODE_MEDIA_PREVIOUS,
            KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE,
            KeyEvent.KEYCODE_MEDIA_PLAY,
            KeyEvent.KEYCODE_MEDIA_PAUSE,
            KeyEvent.KEYCODE_MEDIA_STOP,
            KeyEvent.KEYCODE_HEADSETHOOK,
            KeyEvent.KEYCODE_MEDIA_FAST_FORWARD,
            KeyEvent.KEYCODE_MEDIA_REWIND,
            KeyEvent.KEYCODE_MEDIA_STEP_FORWARD,
            KeyEvent.KEYCODE_MEDIA_STEP_BACKWARD,
            KeyEvent.KEYCODE_NAVIGATE_NEXT,
            KeyEvent.KEYCODE_NAVIGATE_PREVIOUS,
            KeyEvent.KEYCODE_CHANNEL_UP,
            KeyEvent.KEYCODE_CHANNEL_DOWN,
            KeyEvent.KEYCODE_VOLUME_UP,
            KeyEvent.KEYCODE_VOLUME_DOWN,
            KeyEvent.KEYCODE_VOICE_ASSIST,
            KeyEvent.KEYCODE_SEARCH -> true
            else -> false
        }
    }

    private fun handleHardwareMediaKey(keyCode: Int): Boolean {
        return when (keyCode) {
            KeyEvent.KEYCODE_MEDIA_NEXT,
            KeyEvent.KEYCODE_MEDIA_FAST_FORWARD,
            KeyEvent.KEYCODE_MEDIA_STEP_FORWARD,
            KeyEvent.KEYCODE_NAVIGATE_NEXT,
            KeyEvent.KEYCODE_CHANNEL_UP -> {
                handleTrackNext()
                true
            }
            KeyEvent.KEYCODE_MEDIA_PREVIOUS,
            KeyEvent.KEYCODE_MEDIA_REWIND,
            KeyEvent.KEYCODE_MEDIA_STEP_BACKWARD,
            KeyEvent.KEYCODE_NAVIGATE_PREVIOUS,
            KeyEvent.KEYCODE_CHANNEL_DOWN -> {
                handleTrackPrev()
                true
            }
            KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE,
            KeyEvent.KEYCODE_HEADSETHOOK -> {
                handlePlayPause()
                true
            }
            KeyEvent.KEYCODE_MEDIA_PLAY -> {
                handlePlay()
                true
            }
            KeyEvent.KEYCODE_MEDIA_PAUSE,
            KeyEvent.KEYCODE_MEDIA_STOP -> {
                handlePause()
                true
            }
            KeyEvent.KEYCODE_VOLUME_UP -> {
                audioManager.adjustStreamVolume(
                    AudioManager.STREAM_MUSIC,
                    AudioManager.ADJUST_RAISE,
                    AudioManager.FLAG_SHOW_UI
                )
                true
            }
            KeyEvent.KEYCODE_VOLUME_DOWN -> {
                audioManager.adjustStreamVolume(
                    AudioManager.STREAM_MUSIC,
                    AudioManager.ADJUST_LOWER,
                    AudioManager.FLAG_SHOW_UI
                )
                true
            }
            KeyEvent.KEYCODE_VOICE_ASSIST,
            KeyEvent.KEYCODE_SEARCH -> {
                com.xavier.carlauncher.voice.OfflineVoiceController.getInstance(applicationContext).startListening()
                true
            }
            else -> false
        }
    }

    private fun handleTrackNext() {
        android.util.Log.i("MainActivity", "Hardware SWC NEXT pressed")
        com.xavier.carlauncher.media.PowerampNotificationListener.skipToNext(applicationContext)
    }

    private fun handleTrackPrev() {
        android.util.Log.i("MainActivity", "Hardware SWC PREV pressed")
        com.xavier.carlauncher.media.PowerampNotificationListener.skipToPrevious(applicationContext)
    }

    private fun handlePlayPause() {
        android.util.Log.i("MainActivity", "Hardware SWC PLAY_PAUSE pressed")
        com.xavier.carlauncher.media.PowerampNotificationListener.togglePlayPause(applicationContext)
    }

    private fun handlePlay() {
        android.util.Log.i("MainActivity", "Hardware SWC PLAY pressed")
        com.xavier.carlauncher.media.PowerampNotificationListener.togglePlayPause(applicationContext)
    }

    private fun handlePause() {
        android.util.Log.i("MainActivity", "Hardware SWC PAUSE pressed")
        com.xavier.carlauncher.media.PowerampNotificationListener.togglePlayPause(applicationContext)
    }

    override fun onResume() {
        super.onResume()
        isActivityResumed = true
        // Passive status update for lyrics & track title (never steals UI focus)
        com.xavier.carlauncher.media.FytMusicService.requestStateUpdate(applicationContext)
    }

    override fun onPause() {
        super.onPause()
        isActivityResumed = false
    }

    @Deprecated("Deprecated in Java")
    override fun onBackPressed() {
        if (onBackPressedDispatcher.hasEnabledCallbacks()) {
            onBackPressedDispatcher.onBackPressed()
        }
        // Essential for Automotive Launcher: Never finish or half-collapse on back press when at root
    }
}
