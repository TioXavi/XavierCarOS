package com.xavier.carlauncher.media

import android.app.Application
import android.content.BroadcastReceiver
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.AudioManager
import android.media.MediaMetadata
import android.media.session.MediaController
import android.media.session.MediaSessionManager
import android.media.session.PlaybackState
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.os.Handler
import android.os.Looper
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.util.Log
import android.view.KeyEvent
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.io.File
import java.io.InputStream

// ── Data classes ─────────────────────────────────────────────────────────────

/**
 * A single timestamped line of synchronized lyrics (LRC).
 */
data class LyricEntry(
    val timeMs: Long,
    val text: String,
    val translation: String? = null
)

/**
 * Universal media snapshot supporting ANY audio player:
 * Poweramp, Spotify, YouTube Music, Apple Music, VLC, Deezer, Tidal, Amazon Music,
 * Musicolet, Bluetooth AVRCP, and stock Android media players.
 */
data class MediaState(
    val title: String = "",
    val artist: String = "",
    val album: String = "",
    val albumArt: Bitmap? = null,
    val progressMs: Long = 0L,
    val durationMs: Long = 1L,
    val isPlaying: Boolean = false,
    val playerName: String = "",
    val lyricsLine: String = "",
    val fullLyrics: List<LyricEntry> = emptyList(),
    val rawLyricsText: String = "",
    val hasLyrics: Boolean = false,
    val playbackMode: PlaybackMode = PlaybackMode.SEQUENCE
)

enum class AudioSource {
    LOCAL,
    BLUETOOTH,
    USB
}

// ── Universal Media & Notification Listener Service ──────────────────────────

/**
 * System-privileged NotificationListenerService that connects to Android's
 * MediaSessionManager to monitor and control 100% of all media apps running
 * on the head unit (Spotify, Poweramp, YouTube Music, Apple Music, Bluetooth audio, etc.).
 */
class PowerampNotificationListener : NotificationListenerService() {

    companion object {
        private const val TAG = "XavierCarOS_Media"
        private const val POWERAMP_PKG = "com.maxmpz.audioplayer"

        // Poweramp broadcast intents (for Poweramp-specific deep enhancements)
        private const val ACTION_POWERAMP_TRACK_CHANGED = "com.maxmpz.audioplayer.TRACK_CHANGED"
        private const val ACTION_POWERAMP_STATUS_CHANGED = "com.maxmpz.audioplayer.STATUS_CHANGED"
        private const val ACTION_POWERAMP_API_COMMAND = "com.maxmpz.audioplayer.API_COMMAND"

        private val _mediaState = MutableStateFlow(MediaState())
        val mediaState: StateFlow<MediaState> = _mediaState.asStateFlow()

        private var activeController: MediaController? = null
        private var instance: PowerampNotificationListener? = null

        var parsedLyrics: List<LyricEntry> = emptyList()
            private set

        private val _currentSource = MutableStateFlow(AudioSource.LOCAL)
        val currentSource: StateFlow<AudioSource> = _currentSource.asStateFlow()

        fun setSource(source: AudioSource) {
            _currentSource.value = source
        }

        fun toggleSource() {
            _currentSource.value = if (_currentSource.value == AudioSource.LOCAL) AudioSource.BLUETOOTH else AudioSource.LOCAL
            Log.i(TAG, "Audio source toggled to: ${_currentSource.value}")
        }

        @Volatile
        private var lastLocalMediaState: MediaState? = null
        @Volatile
        private var lastLocalLyrics: List<LyricEntry> = emptyList()

        /**
         * Restores local OEM media state and switches back to AudioSource.LOCAL.
         */
        fun restoreLocalMediaState(context: Context? = null) {
            Log.i(TAG, "Restoring local media state (switching fully to AudioSource.LOCAL)")
            _currentSource.value = AudioSource.LOCAL
            val cached = lastLocalMediaState
            if (cached != null && cached.title.isNotBlank()) {
                _mediaState.value = cached.copy(isPlaying = false)
                parsedLyrics = lastLocalLyrics
            } else {
                _mediaState.value = _mediaState.value.copy(
                    playerName = "Música Pantalla"
                )
            }
            if (context != null) {
                FytMusicService.requestStateUpdate(context)
            }
        }

        /**
         * Direct update from Bluetooth AVRCP.
         */
        fun updateFromBluetooth(
            title: String,
            artist: String,
            album: String = "",
            isPlaying: Boolean,
            durationMs: Long = 0L,
            albumArt: Bitmap? = null
        ) {
            if (title.isBlank() && !isPlaying) return

            // If Bluetooth starts active playback, switch active source automatically
            if (isPlaying) {
                _currentSource.value = AudioSource.BLUETOOTH
                XavierLocalPlayer.pause()
            }

            val deviceName = com.xavier.carlauncher.bluetooth.BluetoothViewModel.currentDeviceName.ifBlank { "el teléfono" }
            val resolvedTitle = when {
                title.isNotBlank() -> title
                isPlaying -> "Reproduciendo en $deviceName"
                else -> _mediaState.value.title.ifBlank { "Audio conectado" }
            }
            val resolvedArtist = when {
                artist.isNotBlank() -> artist
                album.isNotBlank() -> album
                isPlaying -> "Audio Bluetooth en vivo"
                else -> "Listo para reproducir"
            }

            if (_currentSource.value == AudioSource.BLUETOOTH || !_mediaState.value.isPlaying) {
                _mediaState.value = _mediaState.value.copy(
                    title = resolvedTitle,
                    artist = resolvedArtist,
                    album = album.ifBlank { _mediaState.value.album },
                    albumArt = albumArt ?: _mediaState.value.albumArt,
                    isPlaying = isPlaying,
                    durationMs = if (durationMs > 0) durationMs else _mediaState.value.durationMs,
                    playerName = "Bluetooth Audio"
                )
                if (title.isNotBlank()) {
                    autoDetectLyrics(artist, title)
                }
            }
        }

        /**
         * Actively polls active MediaSessions for Bluetooth AVRCP metadata.
         */
        fun syncBluetoothSession() {
            instance?.let { listener ->
                try {
                    val component = ComponentName(listener, PowerampNotificationListener::class.java)
                    val controllers = listener.mediaSessionManager?.getActiveSessions(component)
                    listener.updateActiveController(controllers)
                    val bt = listener.bluetoothController
                    if (bt != null) {
                        listener.handleUniversalBluetoothMetadata(bt.metadata)
                        listener.handleUniversalBluetoothPlaybackState(bt.playbackState)
                    }
                    Unit
                } catch (e: Exception) {
                    Log.w(TAG, "Error syncing Bluetooth session", e)
                }
            }
        }

        /**
         * Direct update from XavierLocalPlayer.
         */
        fun updateFromLocalPlayer(
            title: String,
            artist: String,
            album: String,
            albumArt: Bitmap?,
            isPlaying: Boolean,
            progressMs: Long,
            durationMs: Long,
            lyricsLine: String,
            fullLyrics: List<LyricEntry>,
            playbackMode: PlaybackMode = PlaybackMode.SEQUENCE
        ) {
            _currentSource.value = AudioSource.LOCAL
            parsedLyrics = fullLyrics
            _mediaState.value = _mediaState.value.copy(
                title = title,
                artist = artist,
                album = album,
                albumArt = albumArt ?: _mediaState.value.albumArt,
                isPlaying = isPlaying,
                progressMs = progressMs,
                durationMs = durationMs,
                lyricsLine = lyricsLine,
                fullLyrics = fullLyrics,
                hasLyrics = fullLyrics.isNotEmpty(),
                playerName = "Música Local",
                playbackMode = playbackMode
            )
            if (title.isNotBlank()) {
                lastLocalMediaState = _mediaState.value
                lastLocalLyrics = fullLyrics
            }
        }

        /**
         * Direct update from OEM FYT / SYU Car Music Service (com.syu.music).
         */
        fun updateFromFyt(
            title: String,
            artist: String,
            album: String,
            albumArt: Bitmap?,
            isPlaying: Boolean,
            progressMs: Long,
            durationMs: Long,
            lyricsLine: String,
            fullLyrics: List<LyricEntry>,
            playerName: String = "Música Pantalla"
        ) {
            if (isPlaying) {
                _currentSource.value = AudioSource.LOCAL
            }

            parsedLyrics = fullLyrics
            _mediaState.value = _mediaState.value.copy(
                title = title,
                artist = artist,
                album = album,
                albumArt = albumArt ?: _mediaState.value.albumArt,
                isPlaying = isPlaying,
                progressMs = progressMs,
                durationMs = durationMs,
                lyricsLine = lyricsLine,
                fullLyrics = fullLyrics,
                hasLyrics = fullLyrics.isNotEmpty(),
                playerName = playerName
            )

            if (title.isNotBlank()) {
                lastLocalMediaState = _mediaState.value
                lastLocalLyrics = fullLyrics
            }
        }

        fun setOptimisticPlaying(playing: Boolean) {
            _mediaState.value = _mediaState.value.copy(isPlaying = playing)
        }

        fun dispatchHardwareMediaKey(context: Context, keyCode: Int) {
            dispatchMediaKey(context, keyCode)
        }

        // ── Universal Playback Controls ──────────────────────────────────────

        fun togglePlayPause(context: Context) {
            val source = _currentSource.value
            val isBtConnected = com.xavier.carlauncher.bluetooth.BluetoothViewModel.isDeviceConnected

            Log.i(TAG, "togglePlayPause invoked (source=$source, isBtConnected=$isBtConnected)")

            if (isBtConnected && (source == AudioSource.BLUETOOTH || _mediaState.value.playerName == "Bluetooth Audio")) {
                setOptimisticPlaying(!_mediaState.value.isPlaying)
                FytMusicService.sendBtPlayPause(context)
                return
            }

            _currentSource.value = AudioSource.LOCAL
            XavierLocalPlayer.togglePlayPause()
        }

        fun skipToNext(context: Context) {
            val source = _currentSource.value
            val isBtConnected = com.xavier.carlauncher.bluetooth.BluetoothViewModel.isDeviceConnected

            Log.i(TAG, "skipToNext invoked (source=$source, isBtConnected=$isBtConnected)")

            if (isBtConnected && (source == AudioSource.BLUETOOTH || _mediaState.value.playerName == "Bluetooth Audio")) {
                FytMusicService.sendBtNext(context)
                return
            }

            _currentSource.value = AudioSource.LOCAL
            XavierLocalPlayer.skipToNext()
        }

        fun skipToPrevious(context: Context) {
            val source = _currentSource.value
            val isBtConnected = com.xavier.carlauncher.bluetooth.BluetoothViewModel.isDeviceConnected

            Log.i(TAG, "skipToPrevious invoked (source=$source, isBtConnected=$isBtConnected)")

            if (isBtConnected && (source == AudioSource.BLUETOOTH || _mediaState.value.playerName == "Bluetooth Audio")) {
                FytMusicService.sendBtPrev(context)
                return
            }

            _currentSource.value = AudioSource.LOCAL
            XavierLocalPlayer.skipToPrevious()
        }

        fun seekTo(context: Context, positionMs: Long) {
            val source = _currentSource.value
            val isBtConnected = com.xavier.carlauncher.bluetooth.BluetoothViewModel.isDeviceConnected

            if (isBtConnected && (source == AudioSource.BLUETOOTH || _mediaState.value.playerName == "Bluetooth Audio")) {
                activeController?.transportControls?.seekTo(positionMs)
                return
            }

            _currentSource.value = AudioSource.LOCAL
            XavierLocalPlayer.seekTo(positionMs)
        }

        fun toggleShuffle(context: Context) {
            if (_currentSource.value == AudioSource.LOCAL) {
                val mode = XavierLocalPlayer.toggleShuffle()
                val msg = if (mode == PlaybackMode.SHUFFLE) "Modo: Aleatorio activado" else "Modo: Secuencial en bucle"
                android.widget.Toast.makeText(context, msg, android.widget.Toast.LENGTH_SHORT).show()
            }
        }

        fun toggleRepeat(context: Context) {
            if (_currentSource.value == AudioSource.LOCAL) {
                val mode = XavierLocalPlayer.toggleRepeat()
                val msg = if (mode == PlaybackMode.REPEAT_ONE) "Modo: Repetir pista actual" else "Modo: Secuencial en bucle"
                android.widget.Toast.makeText(context, msg, android.widget.Toast.LENGTH_SHORT).show()
            }
        }

        fun cyclePlaybackMode(context: Context) {
            if (_currentSource.value == AudioSource.LOCAL) {
                val mode = XavierLocalPlayer.cyclePlaybackMode()
                val msg = when (mode) {
                    PlaybackMode.SEQUENCE -> "Modo: Secuencial en bucle"
                    PlaybackMode.REPEAT_ONE -> "Modo: Repetir pista actual"
                    PlaybackMode.SHUFFLE -> "Modo: Reproducción aleatoria"
                }
                android.widget.Toast.makeText(context, msg, android.widget.Toast.LENGTH_SHORT).show()
            }
        }

        private fun dispatchMediaKey(context: Context, keyCode: Int) {
            val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager ?: return
            audioManager.dispatchMediaKeyEvent(KeyEvent(KeyEvent.ACTION_DOWN, keyCode))
            audioManager.dispatchMediaKeyEvent(KeyEvent(KeyEvent.ACTION_UP, keyCode))
        }

        // ── Lyrics Parser (.lrc & embedded) ──────────────────────────────────

        fun parseLrcStream(inputStream: InputStream): List<LyricEntry> {
            return try {
                val timestampRegex = Regex("""\[(\d{1,2}):(\d{2})(?:[.:](\d{2,3}))?\]""")
                val result = mutableListOf<LyricEntry>()

                inputStream.bufferedReader().useLines { lines ->
                    lines.forEach { rawLine ->
                        val line = rawLine.trim()
                        if (line.isNotEmpty()) {
                            val matches = timestampRegex.findAll(line).toList()
                            if (matches.isNotEmpty()) {
                                val lastMatch = matches.last()
                                val lyricText = line.substring(lastMatch.range.last + 1).trim()

                                for (match in matches) {
                                    val minutes = match.groupValues[1].toLongOrNull() ?: return@forEach
                                    val seconds = match.groupValues[2].toLongOrNull() ?: return@forEach
                                    val centisStr = match.groupValues[3]
                                    val centis = when {
                                        centisStr.isEmpty() -> 0L
                                        centisStr.length == 2 -> centisStr.toLong() * 10L
                                        else -> centisStr.toLong()
                                    }
                                    val millis = minutes * 60_000L + seconds * 1_000L + centis
                                    result.add(LyricEntry(millis, lyricText))
                                }
                            }
                        }
                    }
                }

                val sorted = result.sortedBy { it.timeMs }
                parsedLyrics = sorted
                _mediaState.value = _mediaState.value.copy(
                    fullLyrics = sorted,
                    hasLyrics = sorted.isNotEmpty(),
                    lyricsLine = getCurrentLyricLine(sorted, _mediaState.value.progressMs)
                )
                sorted
            } catch (e: Exception) {
                Log.e(TAG, "Failed to parse lyrics stream", e)
                emptyList()
            }
        }

        fun parseLrcFile(path: String): List<LyricEntry> {
            val file = File(path)
            if (!file.exists()) return emptyList()
            return parseLrcStream(file.inputStream())
        }

        fun getCurrentLyricLine(lyrics: List<LyricEntry>, positionMs: Long): String {
            if (lyrics.isEmpty()) return ""
            var lo = 0
            var hi = lyrics.size - 1
            var result = ""
            while (lo <= hi) {
                val mid = (lo + hi) / 2
                if (lyrics[mid].timeMs <= positionMs) {
                    result = lyrics[mid].text
                    lo = mid + 1
                } else {
                    hi = mid - 1
                }
            }
            return result
        }

        /**
         * Search for matching local .lrc files in common Android music paths,
         * or fetches synchronized lyrics online via LyricsRepository (LRCLIB).
         */
        fun autoDetectLyrics(artist: String, title: String) {
            if (title.isBlank() || title.contains("Reproduciendo", ignoreCase = true) || title.contains("Bluetooth", ignoreCase = true)) return

            val searchDirs = listOf(
                File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MUSIC), "Lyrics"),
                Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MUSIC),
                Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS),
                File("/sdcard/Music/Lyrics"),
                File("/sdcard/Music"),
                File("/sdcard/Download")
            )

            val cleanTitle = title.lowercase().replace(Regex("""[^a-z0-9]"""), "")
            val cleanArtist = artist.lowercase().replace(Regex("""[^a-z0-9]"""), "")

            for (dir in searchDirs) {
                if (dir.exists() && dir.isDirectory) {
                    val matching = dir.listFiles { f ->
                        if (!f.name.endsWith(".lrc", ignoreCase = true)) return@listFiles false
                        val name = f.nameWithoutExtension.lowercase().replace(Regex("""[^a-z0-9]"""), "")
                        name.contains(cleanTitle) || (cleanArtist.isNotEmpty() && name.contains(cleanArtist))
                    }
                    if (!matching.isNullOrEmpty()) {
                        Log.i(TAG, "Auto-detected matching local LRC file: ${matching[0].absolutePath}")
                        parseLrcFile(matching[0].absolutePath)
                        return
                    }
                }
            }

            // Fallback: Query online LRCLIB database via LyricsRepository
            instance?.let { ctx ->
                CoroutineScope(Dispatchers.IO).launch {
                    try {
                        val lyrics = LyricsRepository.getOrFetchLyrics(ctx, title, artist)
                        if (lyrics.isNotEmpty()) {
                            parsedLyrics = lyrics
                            _mediaState.value = _mediaState.value.copy(
                                fullLyrics = lyrics,
                                hasLyrics = true
                            )
                            Log.i(TAG, "Loaded ${lyrics.size} synchronized lyrics from LRCLIB for '$title' by '$artist'")
                        }
                    } catch (e: Exception) {
                        Log.w(TAG, "Cannot fetch online lyrics for BT track '$title'", e)
                    }
                }
            }
        }
    }

    private var mediaSessionManager: MediaSessionManager? = null
    private val mainHandler = Handler(Looper.getMainLooper())
    private var progressTickerJob: Job? = null

    // ── MediaController Callback ─────────────────────────────────────────────

    private val controllerCallback = object : MediaController.Callback() {
        override fun onMetadataChanged(metadata: MediaMetadata?) {
            Log.d(TAG, "MediaController metadata changed")
            handleUniversalMetadata(metadata, activeController?.packageName)
        }

        override fun onPlaybackStateChanged(state: PlaybackState?) {
            Log.d(TAG, "MediaController playback state changed: ${state?.state}")
            handleUniversalPlaybackState(state)
        }

        override fun onSessionDestroyed() {
            Log.d(TAG, "Media session destroyed")
            activeController = null
            refreshActiveSessions()
        }
    }

    private var bluetoothController: MediaController? = null

    private val bluetoothCallback = object : MediaController.Callback() {
        override fun onMetadataChanged(metadata: MediaMetadata?) {
            Log.i(TAG, "bluetoothController metadata changed callback")
            handleUniversalBluetoothMetadata(metadata)
        }

        override fun onPlaybackStateChanged(state: PlaybackState?) {
            Log.i(TAG, "bluetoothController playback state changed callback: ${state?.state}")
            handleUniversalBluetoothPlaybackState(state)
        }

        override fun onSessionDestroyed() {
            Log.i(TAG, "bluetoothController session destroyed")
            bluetoothController = null
        }
    }

    private val sessionListener = MediaSessionManager.OnActiveSessionsChangedListener { controllers ->
        Log.d(TAG, "Active sessions changed. Found ${controllers?.size ?: 0} controllers")
        updateActiveController(controllers)
    }

    // ── Poweramp Direct Receiver (supplementary) ─────────────────────────────

    private val powerampReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            intent ?: return
            when (intent.action) {
                ACTION_POWERAMP_TRACK_CHANGED -> {
                    val title = intent.getStringExtra("title") ?: ""
                    val artist = intent.getStringExtra("artist") ?: ""
                    val albumArt: Bitmap? = intent.getParcelableExtra("albumArt")
                    val duration = intent.getLongExtra("dur", 0L)

                    val current = _mediaState.value
                    _mediaState.value = current.copy(
                        title = title.ifEmpty { current.title },
                        artist = artist.ifEmpty { current.artist },
                        albumArt = albumArt ?: current.albumArt,
                        durationMs = if (duration > 0) duration else current.durationMs,
                        progressMs = 0L,
                        playerName = "Poweramp"
                    )
                    autoDetectLyrics(artist, title)
                }
                ACTION_POWERAMP_STATUS_CHANGED -> {
                    val stateCode = intent.getIntExtra("state", -1)
                    val isPlaying = stateCode == 1
                    val positionMs = intent.getLongExtra("position", _mediaState.value.progressMs)
                    val current = _mediaState.value
                    _mediaState.value = current.copy(
                        isPlaying = isPlaying,
                        progressMs = positionMs,
                        lyricsLine = getCurrentLyricLine(parsedLyrics, positionMs)
                    )
                }
            }
        }
    }

    // ── Lifecycle ────────────────────────────────────────────────────────────

    override fun onCreate() {
        super.onCreate()
        instance = this
        Log.d(TAG, "Universal MediaNotificationListener created")

        // Register Poweramp direct broadcast receiver
        val filter = IntentFilter().apply {
            addAction(ACTION_POWERAMP_TRACK_CHANGED)
            addAction(ACTION_POWERAMP_STATUS_CHANGED)
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(powerampReceiver, filter, Context.RECEIVER_EXPORTED)
        } else {
            registerReceiver(powerampReceiver, filter)
        }

        // Initialize MediaSessionManager
        try {
            mediaSessionManager = getSystemService(Context.MEDIA_SESSION_SERVICE) as? MediaSessionManager
            val component = ComponentName(this, PowerampNotificationListener::class.java)
            mediaSessionManager?.addOnActiveSessionsChangedListener(sessionListener, component)
            refreshActiveSessions()
        } catch (e: Exception) {
            Log.e(TAG, "Failed to initialize MediaSessionManager", e)
        }

        startProgressTicker()
    }

    override fun onDestroy() {
        try {
            unregisterReceiver(powerampReceiver)
            activeController?.unregisterCallback(controllerCallback)
            bluetoothController?.unregisterCallback(bluetoothCallback)
            mediaSessionManager?.removeOnActiveSessionsChangedListener(sessionListener)
            mainHandler.removeCallbacksAndMessages(null)
            progressTickerJob?.cancel()
        } catch (e: Exception) {
            Log.w(TAG, "Error during cleanup", e)
        }
        instance = null
        super.onDestroy()
    }

    // ── Active Sessions Resolution ───────────────────────────────────────────

    fun refreshActiveSessions() {
        try {
            val component = ComponentName(this, PowerampNotificationListener::class.java)
            val controllers = mediaSessionManager?.getActiveSessions(component)
            updateActiveController(controllers)
        } catch (e: Exception) {
            Log.w(TAG, "Cannot get active sessions — permission may be pending", e)
        }
    }

    private fun updateActiveController(controllers: List<MediaController>?) {
        if (controllers.isNullOrEmpty()) return

        // 1. Dedicated Bluetooth controller for com.android.bluetooth
        val bt = controllers.firstOrNull { it.packageName.contains("bluetooth", ignoreCase = true) }
        if (bt != null) {
            if (bluetoothController?.sessionToken != bt.sessionToken) {
                bluetoothController?.unregisterCallback(bluetoothCallback)
                bluetoothController = bt
                bt.registerCallback(bluetoothCallback, mainHandler)
                Log.i(TAG, "Bound dedicated bluetoothController to ${bt.packageName}")
                handleUniversalBluetoothMetadata(bt.metadata)
                handleUniversalBluetoothPlaybackState(bt.playbackState)
            }
        } else {
            if (bluetoothController != null) {
                bluetoothController?.unregisterCallback(bluetoothCallback)
                bluetoothController = null
            }
        }

        // 2. Select preferred media player for other sessions (excluding dummy com.syu.music and bluetooth)
        val nonBtControllers = controllers.filter { !it.packageName.contains("bluetooth", ignoreCase = true) && it.packageName != "com.syu.music" }
        val preferred = nonBtControllers.firstOrNull { it.playbackState?.state == PlaybackState.STATE_PLAYING }
            ?: nonBtControllers.firstOrNull()

        if (preferred != null) {
            if (activeController?.sessionToken != preferred.sessionToken) {
                activeController?.unregisterCallback(controllerCallback)
                activeController = preferred
                preferred.registerCallback(controllerCallback, mainHandler)

                Log.i(TAG, "Bound to active media player: ${preferred.packageName}")
                handleUniversalMetadata(preferred.metadata, preferred.packageName)
                handleUniversalPlaybackState(preferred.playbackState)
            }
        }
    }

    private fun handleUniversalBluetoothMetadata(metadata: MediaMetadata?) {
        metadata ?: return
        val title = metadata.getString(MediaMetadata.METADATA_KEY_TITLE)
            ?: metadata.getString(MediaMetadata.METADATA_KEY_DISPLAY_TITLE)
            ?: ""
        val artist = metadata.getString(MediaMetadata.METADATA_KEY_ARTIST)
            ?: metadata.getString(MediaMetadata.METADATA_KEY_ALBUM_ARTIST)
            ?: metadata.getString(MediaMetadata.METADATA_KEY_AUTHOR)
            ?: ""
        val album = metadata.getString(MediaMetadata.METADATA_KEY_ALBUM) ?: ""
        val duration = metadata.getLong(MediaMetadata.METADATA_KEY_DURATION)
        val albumArt: Bitmap? = metadata.getBitmap(MediaMetadata.METADATA_KEY_ALBUM_ART)
            ?: metadata.getBitmap(MediaMetadata.METADATA_KEY_ART)

        val isPlaying = bluetoothController?.playbackState?.state == PlaybackState.STATE_PLAYING

        Log.i(TAG, "handleUniversalBluetoothMetadata: title='$title', artist='$artist', album='$album', dur=$duration, isPlaying=$isPlaying")

        if (title.isNotBlank() || isPlaying) {
            com.xavier.carlauncher.bluetooth.BluetoothStateReceiver.updateBtTrack(
                title = title,
                artist = artist,
                album = album,
                isPlaying = isPlaying,
                durationMs = duration
            )
            updateFromBluetooth(
                title = title,
                artist = artist,
                album = album,
                isPlaying = isPlaying,
                durationMs = duration,
                albumArt = albumArt
            )
        }
    }

    private fun handleUniversalBluetoothPlaybackState(state: PlaybackState?) {
        state ?: return
        val isPlaying = state.state == PlaybackState.STATE_PLAYING
        val pos = state.position

        Log.i(TAG, "handleUniversalBluetoothPlaybackState: isPlaying=$isPlaying, pos=$pos")

        if (isPlaying) {
            if (FytMusicService.isOemPlaying || _currentSource.value == AudioSource.LOCAL) {
                Log.i(TAG, "Bluetooth started playback -> auto-pausing local music")
                instance?.let { FytMusicService.pauseMusic(it) }
            }
            _currentSource.value = AudioSource.BLUETOOTH
        }

        val meta = bluetoothController?.metadata
        val title = meta?.getString(MediaMetadata.METADATA_KEY_TITLE)
            ?: meta?.getString(MediaMetadata.METADATA_KEY_DISPLAY_TITLE)
            ?: _mediaState.value.title
        val artist = meta?.getString(MediaMetadata.METADATA_KEY_ARTIST)
            ?: meta?.getString(MediaMetadata.METADATA_KEY_ALBUM_ARTIST)
            ?: _mediaState.value.artist
        val album = meta?.getString(MediaMetadata.METADATA_KEY_ALBUM)
            ?: _mediaState.value.album
        val duration = meta?.getLong(MediaMetadata.METADATA_KEY_DURATION) ?: _mediaState.value.durationMs

        com.xavier.carlauncher.bluetooth.BluetoothStateReceiver.updateBtTrack(
            title = title,
            artist = artist,
            album = album,
            isPlaying = isPlaying,
            durationMs = duration,
            positionMs = pos
        )

        if (_currentSource.value == AudioSource.BLUETOOTH) {
            _mediaState.value = _mediaState.value.copy(
                isPlaying = isPlaying,
                progressMs = if (pos >= 0) pos else _mediaState.value.progressMs,
                lyricsLine = getCurrentLyricLine(parsedLyrics, pos)
            )
        }
    }

    private fun handleUniversalMetadata(metadata: MediaMetadata?, pkg: String?) {
        metadata ?: return
        val title = metadata.getString(MediaMetadata.METADATA_KEY_TITLE)
            ?: metadata.getString(MediaMetadata.METADATA_KEY_DISPLAY_TITLE)
            ?: ""
        val artist = metadata.getString(MediaMetadata.METADATA_KEY_ARTIST)
            ?: metadata.getString(MediaMetadata.METADATA_KEY_ALBUM_ARTIST)
            ?: metadata.getString(MediaMetadata.METADATA_KEY_AUTHOR)
            ?: ""
        val album = metadata.getString(MediaMetadata.METADATA_KEY_ALBUM) ?: ""

        val albumArt: Bitmap? = metadata.getBitmap(MediaMetadata.METADATA_KEY_ALBUM_ART)
            ?: metadata.getBitmap(MediaMetadata.METADATA_KEY_ART)

        val duration = metadata.getLong(MediaMetadata.METADATA_KEY_DURATION)

        // Check for embedded lyrics in metadata
        val embeddedLyrics = metadata.getString(MediaMetadata.METADATA_KEY_DISPLAY_DESCRIPTION)
            ?: metadata.getString("android.media.metadata.LYRICS")
            ?: ""

        val friendlyPlayerName = when {
            pkg?.contains("spotify", ignoreCase = true) == true -> "Spotify"
            pkg?.contains("youtube", ignoreCase = true) == true -> "YouTube Music"
            pkg?.contains("maxmpz", ignoreCase = true) == true -> "Poweramp"
            pkg?.contains("apple", ignoreCase = true) == true -> "Apple Music"
            pkg?.contains("vlc", ignoreCase = true) == true -> "VLC"
            pkg?.contains("bluetooth", ignoreCase = true) == true -> "Bluetooth Audio"
            else -> pkg?.substringAfterLast('.') ?: "Música"
        }

        val current = _mediaState.value
        _mediaState.value = current.copy(
            title = title.ifEmpty { current.title },
            artist = artist.ifEmpty { current.artist },
            album = album.ifEmpty { current.album },
            albumArt = albumArt ?: current.albumArt,
            durationMs = if (duration > 0) duration else current.durationMs,
            playerName = friendlyPlayerName,
            rawLyricsText = embeddedLyrics
        )

        // If embedded lyrics exist, parse or set them
        if (embeddedLyrics.isNotBlank() && (embeddedLyrics.contains("[0") || embeddedLyrics.contains("[1"))) {
            parseLrcStream(embeddedLyrics.byteInputStream())
        } else {
            autoDetectLyrics(artist, title)
        }

        if (pkg?.contains("bluetooth", ignoreCase = true) == true) {
            com.xavier.carlauncher.bluetooth.BluetoothStateReceiver.updateBtTrack(
                title = title,
                artist = artist,
                album = album,
                isPlaying = _mediaState.value.isPlaying,
                durationMs = duration
            )
        }
    }

    private fun handleUniversalPlaybackState(state: PlaybackState?) {
        state ?: return
        val isPlaying = state.state == PlaybackState.STATE_PLAYING
        val positionMs = state.position

        val current = _mediaState.value
        _mediaState.value = current.copy(
            isPlaying = isPlaying,
            progressMs = if (positionMs >= 0) positionMs else current.progressMs,
            lyricsLine = getCurrentLyricLine(parsedLyrics, positionMs)
        )

        if (activeController?.packageName?.contains("bluetooth", ignoreCase = true) == true) {
            com.xavier.carlauncher.bluetooth.BluetoothStateReceiver.updateBtTrack(
                title = _mediaState.value.title,
                artist = _mediaState.value.artist,
                album = _mediaState.value.album,
                isPlaying = isPlaying,
                durationMs = _mediaState.value.durationMs,
                positionMs = positionMs
            )
            if (isPlaying) {
                if (FytMusicService.isOemPlaying || _currentSource.value == AudioSource.LOCAL) {
                    Log.i(TAG, "Bluetooth active session is playing -> pausing local music")
                    instance?.let { FytMusicService.pauseMusic(it) }
                }
                _currentSource.value = AudioSource.BLUETOOTH
            }
        }
    }

    // ── Continuous Progress Ticker (1-second precision) ──────────────────────

    private fun startProgressTicker() {
        progressTickerJob?.cancel()
        // Simple ticker without excessive thread creation
        mainHandler.post(object : Runnable {
            override fun run() {
                val current = _mediaState.value
                if (current.isPlaying && current.durationMs > 0) {
                    val nextPos = (current.progressMs + 1000L).coerceAtMost(current.durationMs)
                    val lyricLine = getCurrentLyricLine(parsedLyrics, nextPos)
                    _mediaState.value = current.copy(
                        progressMs = nextPos,
                        lyricsLine = if (lyricLine.isNotEmpty()) lyricLine else current.lyricsLine
                    )
                }
                mainHandler.postDelayed(this, 1000L)
            }
        })
    }

    // ── Universal Notification Interceptor (Navigation & Bluetooth) ──────────

    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        sbn ?: return
        val pkg = sbn.packageName ?: return
        val extras = sbn.notification?.extras ?: return

        // 1. Navigation Turn-by-Turn Guidance Interceptor (Waze & Google Maps)
        if (pkg == "com.waze" || pkg == "com.google.android.apps.maps") {
            val providerName = if (pkg == "com.waze") "Waze" else "Google Maps"
            val title = extras.getCharSequence(android.app.Notification.EXTRA_TITLE)?.toString() ?: ""
            val text = extras.getCharSequence(android.app.Notification.EXTRA_TEXT)?.toString() ?: ""
            val subText = extras.getCharSequence(android.app.Notification.EXTRA_SUB_TEXT)?.toString() ?: ""

            if (title.isNotBlank() || text.isNotBlank()) {
                val distance = if (title.matches(Regex(""".*\d+\s*(m|km|ft|mi).*""", RegexOption.IGNORE_CASE))) title else ""
                val street = if (distance.isNotEmpty()) text else title
                val eta = subText.ifBlank { if (text.contains(":") || text.contains("min")) text else "" }

                com.xavier.carlauncher.navigation.NavigationManager.updateGuidance(
                    provider = providerName,
                    instruction = if (distance.isNotEmpty()) "$title $text" else text.ifBlank { title },
                    distance = distance,
                    street = street,
                    eta = eta
                )
            }
        }

        // 2. Bluetooth Media session / AVRCP metadata interceptor
        if (pkg.contains("bluetooth", ignoreCase = true)) {
            val title = extras.getString(MediaMetadata.METADATA_KEY_TITLE)
                ?: extras.getCharSequence(android.app.Notification.EXTRA_TITLE)?.toString() ?: ""
            val artist = extras.getString(MediaMetadata.METADATA_KEY_ARTIST)
                ?: extras.getCharSequence(android.app.Notification.EXTRA_TEXT)?.toString() ?: ""
            val album = extras.getString(MediaMetadata.METADATA_KEY_ALBUM)
                ?: extras.getCharSequence(android.app.Notification.EXTRA_SUB_TEXT)?.toString() ?: ""
            if (title.isNotBlank()) {
                com.xavier.carlauncher.bluetooth.BluetoothStateReceiver.updateBtTrack(
                    title = title,
                    artist = artist,
                    album = album,
                    isPlaying = true
                )
            }
        }

        // 3. Media session update
        val isMedia = extras.containsKey(android.app.Notification.EXTRA_MEDIA_SESSION) ||
                      extras.containsKey(MediaMetadata.METADATA_KEY_TITLE) ||
                      extras.containsKey("android.mediaMetadata")
        if (isMedia) {
            refreshActiveSessions()
        }
    }

    override fun onNotificationRemoved(sbn: StatusBarNotification?) {
        val pkg = sbn?.packageName ?: return
        if (pkg == "com.waze" || pkg == "com.google.android.apps.maps") {
            val providerName = if (pkg == "com.waze") "Waze" else "Google Maps"
            com.xavier.carlauncher.navigation.NavigationManager.clearGuidance(providerName)
        }
        if (pkg == activeController?.packageName) {
            refreshActiveSessions()
        }
    }
}

// ── ViewModel ────────────────────────────────────────────────────────────────

/**
 * Universal ViewModel for all media player operations in XavierCarOS.
 */
class PowerampMediaViewModel(application: Application) : AndroidViewModel(application) {

    val mediaState: StateFlow<MediaState> = PowerampNotificationListener.mediaState

    fun togglePlayPause(context: Context) {
        PowerampNotificationListener.togglePlayPause(context)
    }

    fun skipToNext(context: Context) {
        PowerampNotificationListener.skipToNext(context)
    }

    fun skipToPrevious(context: Context) {
        PowerampNotificationListener.skipToPrevious(context)
    }

    fun seekTo(context: Context, positionMs: Long) {
        PowerampNotificationListener.seekTo(context, positionMs)
    }

    fun toggleShuffle(context: Context) {
        PowerampNotificationListener.toggleShuffle(context)
    }

    fun toggleRepeat(context: Context) {
        PowerampNotificationListener.toggleRepeat(context)
    }

    fun cyclePlaybackMode(context: Context) {
        PowerampNotificationListener.cyclePlaybackMode(context)
    }

    fun loadLrcFromUri(context: Context, uri: Uri) {
        viewModelScope.launch {
            try {
                context.contentResolver.openInputStream(uri)?.use { stream ->
                    PowerampNotificationListener.parseLrcStream(stream)
                }
            } catch (e: Exception) {
                Log.e("PowerampMediaViewModel", "Failed to load LRC from URI", e)
            }
        }
    }
}
