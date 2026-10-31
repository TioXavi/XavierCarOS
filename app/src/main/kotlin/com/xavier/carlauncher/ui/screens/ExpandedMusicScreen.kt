package com.xavier.carlauncher.ui.screens

import android.content.Context
import android.content.Intent
import android.media.AudioManager
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.Folder
import androidx.compose.material.icons.outlined.HeartBroken
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.xavier.carlauncher.media.AudioSource
import com.xavier.carlauncher.media.FavoritesManager
import com.xavier.carlauncher.media.LocalTrack
import com.xavier.carlauncher.media.LyricEntry
import com.xavier.carlauncher.media.LyricsTranslationManager
import com.xavier.carlauncher.system.AutomotiveIntentResolver
import com.xavier.carlauncher.media.MediaState
import com.xavier.carlauncher.media.PlaybackMode
import com.xavier.carlauncher.media.PowerampNotificationListener
import com.xavier.carlauncher.media.XavierLocalPlayer
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.io.File

/**
 * High-Performance Automotive Expanded Music Studio Screen for XavierCarOS2.
 * Directly integrates 3 responsive cards:
 * 1. Master Player (Source, High-res cover art, interactive seeking, full transport controls, -10s/+10s, volume).
 * 2. Music Explorer (Search, 4 library tabs: Lista, Artistas, Favoritos, Carpetas, animated active audio bars).
 * 3. Live Lyrics / Karaoke XL (Synchronized lyrics with instant timestamp seeking, bilingual EN/ES translation, zoom A-/A+, and expand mode).
 */
@Composable
fun ExpandedMusicScreen(
    mediaState: MediaState,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    // ── Persistent state & collections ───────────────────────────────────────
    val playlist by XavierLocalPlayer.playlistFlow.collectAsState()
    val favorites by FavoritesManager.favoritesFlow.collectAsState()
    val playbackMode by XavierLocalPlayer.playbackMode.collectAsState()
    val activeQueueTitle by XavierLocalPlayer.queueTitleFlow.collectAsState()

    // ── Local Screen UI States ───────────────────────────────────────────────
    val currentSource by PowerampNotificationListener.currentSource.collectAsState()
    var searchQuery by remember { mutableStateOf("") }
    var activeLibraryTab by remember { mutableStateOf(0) } // 0: Lista, 1: Artistas, 2: Favoritos, 3: Carpetas
    var selectedArtistFilter by remember { mutableStateOf<String?>(null) }
    var selectedFolderFilter by remember { mutableStateOf<String?>(null) }

    // Karaoke states
    var showTranslation by remember { mutableStateOf(false) }
    var fontScale by remember { mutableStateOf(0) } // -1, 0, 1, 2, 3
    var isKaraokeExpanded by remember { mutableStateOf(false) }

    // Volume state
    val audioManager = remember { context.getSystemService(Context.AUDIO_SERVICE) as AudioManager }
    val maxVolume = remember { audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC).toFloat().coerceAtLeast(1f) }
    var currentVolume by remember {
        mutableStateOf(audioManager.getStreamVolume(AudioManager.STREAM_MUSIC).toFloat())
    }

    // Interactive seek state
    var isUserSeeking by remember { mutableStateOf(false) }
    var seekPositionMs by remember { mutableStateOf(0L) }
    val effectiveProgressMs = if (isUserSeeking) seekPositionMs else mediaState.progressMs

    // Audio format badge (e.g. FLAC · 24-Bit / WAV · Sin pérdida / MP3 · 320k)
    val audioQualityBadge = remember(mediaState.title, mediaState.artist) {
        val activePath = XavierLocalPlayer.currentTrack?.path ?: ""
        val ext = activePath.substringAfterLast('.', "").uppercase()
        when (ext) {
            "FLAC" -> "HI-RES FLAC"
            "WAV" -> "WAV LOSSLESS"
            "M4A", "AAC" -> "AAC 256K"
            else -> "MP3 320K"
        }
    }

    Row(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF050811))
            .padding(8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {

        // =====================================================================
        // 1. COLUMNA IZQUIERDA (27%): REPRODUCTOR MASTER
        // =====================================================================
        Card(
            modifier = Modifier
                .weight(0.27f)
                .fillMaxHeight(),
            shape = RoundedCornerShape(22.dp),
            border = BorderStroke(1.dp, Color(0x618B5CF6)),
            colors = CardDefaults.cardColors(containerColor = Color.Transparent)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.linearGradient(
                            colors = listOf(Color(0xFF27154E), Color(0xFF150B2E))
                        )
                    )
                    .padding(14.dp)
            ) {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.SpaceBetween
                ) {

                    // ── 1.1 Header Fuente de Audio + Badge ───────────────────
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(22.dp)
                                        .clip(CircleShape)
                                        .background(Color(0xFF0F0B1E).copy(alpha = 0.8f))
                                        .clickable { onNavigateBack() },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.ArrowBack,
                                        contentDescription = "Volver a Inicio",
                                        tint = Color(0xFFDDD6FE),
                                        modifier = Modifier.size(13.dp)
                                    )
                                }
                                Text(
                                    text = "FUENTE DE AUDIO",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFFDDD6FE),
                                    letterSpacing = 0.8.sp
                                )
                            }
                            Text(
                                text = "● $audioQualityBadge",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = Color(0xFF67E8F9)
                            )
                        }

                        Spacer(modifier = Modifier.height(6.dp))

                        // Selector de Fuentes
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(Color(0xFF0F0B1E).copy(alpha = 0.85f), RoundedCornerShape(12.dp))
                                .border(BorderStroke(1.dp, Color(0x33A855F7)), RoundedCornerShape(12.dp))
                                .padding(3.dp),
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            listOf(
                                Triple(AudioSource.LOCAL, "Local", Icons.Default.Album),
                                Triple(AudioSource.ONLINE, "Online", Icons.Default.Cloud),
                                Triple(AudioSource.BLUETOOTH, "BT", Icons.Default.Bluetooth),
                                Triple(AudioSource.USB, "USB", Icons.Default.Usb)
                            ).forEach { (source, label, icon) ->
                                val isActive = currentSource == source
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(if (isActive) Color(0xFF9333EA) else Color.Transparent)
                                        .clickable {
                                            PowerampNotificationListener.setSource(source)
                                            when (source) {
                                                AudioSource.BLUETOOTH -> {
                                                    AutomotiveIntentResolver.launchSafely(context, AutomotiveIntentResolver.getPhoneIntent(context))
                                                }
                                                AudioSource.ONLINE -> {
                                                    PowerampNotificationListener.togglePlayPause(context)
                                                }
                                                else -> {
                                                    XavierLocalPlayer.play()
                                                }
                                            }
                                        }
                                        .padding(vertical = 4.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(3.dp)
                                    ) {
                                        Icon(
                                            imageVector = icon,
                                            contentDescription = null,
                                            tint = if (isActive) Color.White else Color(0xFF94A3B8),
                                            modifier = Modifier.size(13.dp)
                                        )
                                        Text(
                                            text = label,
                                            fontSize = 11.sp,
                                            fontWeight = if (isActive) FontWeight.Bold else FontWeight.Medium,
                                            color = if (isActive) Color.White else Color(0xFFCBD5E1)
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // ── 1.2 Carátula de Álbum + Botón Favorito ───────────────
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f, fill = false),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        val activePath = XavierLocalPlayer.currentTrack?.path ?: ""
                        val isCurrentFav = FavoritesManager.isFavorite(activePath)

                        Box(
                            modifier = Modifier
                                .size(148.dp)
                                .shadow(24.dp, RoundedCornerShape(20.dp))
                                .border(BorderStroke(1.5.dp, Color(0x66C084FC)), RoundedCornerShape(20.dp))
                                .clip(RoundedCornerShape(20.dp))
                                .background(
                                    Brush.radialGradient(
                                        listOf(Color(0xFF381B6D), Color(0xFF160D2E))
                                    )
                                )
                        ) {
                            if (mediaState.albumArt != null) {
                                Image(
                                    bitmap = mediaState.albumArt.asImageBitmap(),
                                    contentDescription = "Carátula",
                                    modifier = Modifier.fillMaxSize()
                                )
                            } else {
                                Box(
                                    modifier = Modifier.fillMaxSize(),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.MusicNote,
                                        contentDescription = null,
                                        tint = Color(0xFFA855F7).copy(alpha = 0.5f),
                                        modifier = Modifier.size(54.dp)
                                    )
                                }
                            }

                            // Botón Favorito Flotante
                            Box(
                                modifier = Modifier
                                    .align(Alignment.BottomEnd)
                                    .offset(x = (-6).dp, y = (-6).dp)
                                    .size(36.dp)
                                    .shadow(12.dp, CircleShape)
                                    .background(Color(0xFF0B0718).copy(alpha = 0.92f), CircleShape)
                                    .border(BorderStroke(1.dp, Color(0x66C084FC)), CircleShape)
                                    .clickable {
                                        FavoritesManager.toggleFavorite(activePath)
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = if (isCurrentFav) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                                    contentDescription = "Favorito",
                                    tint = if (isCurrentFav) Color(0xFFF43F5E) else Color(0xFF94A3B8),
                                    modifier = Modifier.size(17.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        // Metadatos
                        Text(
                            text = mediaState.title.ifEmpty { "Sin reproducción" },
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Black,
                            color = Color.White,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth()
                        )
                        Text(
                            text = mediaState.artist.ifEmpty { "XavierCarOS Player" },
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color(0xFFDDD6FE),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth().padding(top = 2.dp)
                        )
                        if (mediaState.album.isNotBlank()) {
                            Text(
                                text = mediaState.album,
                                fontSize = 10.sp,
                                color = Color(0xFF94A3B8),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.fillMaxWidth().padding(top = 1.dp)
                            )
                        }

                        // Secuencia Activa (Contextual Queue Badge)
                        Box(
                            modifier = Modifier
                                .padding(top = 4.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(Color(0xFF0F0B1E).copy(alpha = 0.85f))
                                .border(BorderStroke(0.8.dp, Color(0xFF67E8F9).copy(alpha = 0.4f)), RoundedCornerShape(8.dp))
                                .padding(horizontal = 7.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = "▶ $activeQueueTitle",
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF67E8F9),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }

                    // ── 1.3 Barra de Tiempo y Transporte ─────────────────────
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        // Seekbar
                        Slider(
                            value = effectiveProgressMs.coerceIn(0L, mediaState.durationMs.coerceAtLeast(1L)).toFloat(),
                            onValueChange = { newVal ->
                                isUserSeeking = true
                                seekPositionMs = newVal.toLong()
                            },
                            onValueChangeFinished = {
                                if (currentSource == AudioSource.ONLINE || currentSource == AudioSource.BLUETOOTH) {
                                    PowerampNotificationListener.seekTo(context, seekPositionMs)
                                } else {
                                    XavierLocalPlayer.seekTo(seekPositionMs)
                                }
                                isUserSeeking = false
                            },
                            valueRange = 0f..mediaState.durationMs.coerceAtLeast(1L).toFloat(),
                            colors = SliderDefaults.colors(
                                thumbColor = Color.White,
                                activeTrackColor = Color.White,
                                inactiveTrackColor = Color(0xFF3B0764)
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(20.dp)
                        )

                        // Tiempos
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = if (effectiveProgressMs > 0L) formatDuration(effectiveProgressMs) else "0:00",
                                fontSize = 11.sp,
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.SemiBold,
                                color = Color(0xFFCBD5E1)
                            )
                            Text(
                                text = if (mediaState.durationMs > 0L) formatDuration(mediaState.durationMs) else "--:--",
                                fontSize = 11.sp,
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.SemiBold,
                                color = Color(0xFFCBD5E1)
                            )
                        }

                        // Botones de Transporte
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Aleatorio
                            val isShuffle = playbackMode == PlaybackMode.SHUFFLE
                            IconButton(
                                onClick = { XavierLocalPlayer.toggleShuffle() },
                                modifier = Modifier.size(34.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Shuffle,
                                    contentDescription = "Aleatorio",
                                    tint = if (isShuffle) Color(0xFF38BDF8) else Color(0xFF64748B),
                                    modifier = Modifier.size(18.dp)
                                )
                            }

                            // Anterior
                            IconButton(
                                onClick = {
                                    if (currentSource == AudioSource.ONLINE || currentSource == AudioSource.BLUETOOTH) {
                                        PowerampNotificationListener.skipToPrevious(context)
                                    } else {
                                        XavierLocalPlayer.skipToPrevious()
                                    }
                                },
                                modifier = Modifier
                                    .size(38.dp)
                                    .background(Color(0xFF0F0B1E).copy(alpha = 0.6f), CircleShape)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.SkipPrevious,
                                    contentDescription = "Anterior",
                                    tint = Color.White,
                                    modifier = Modifier.size(20.dp)
                                )
                            }

                            // Play / Pause Grande
                            Box(
                                modifier = Modifier
                                    .size(46.dp)
                                    .shadow(16.dp, CircleShape)
                                    .background(Color.White, CircleShape)
                                    .clickable {
                                        if (currentSource == AudioSource.ONLINE || currentSource == AudioSource.BLUETOOTH) {
                                            PowerampNotificationListener.togglePlayPause(context)
                                        } else {
                                            if (mediaState.isPlaying) {
                                                XavierLocalPlayer.pause()
                                            } else {
                                                XavierLocalPlayer.play()
                                            }
                                        }
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = if (mediaState.isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                                    contentDescription = "Play/Pausa",
                                    tint = Color(0xFF0F172A),
                                    modifier = Modifier.size(26.dp)
                                )
                            }

                            // Siguiente
                            IconButton(
                                onClick = {
                                    if (currentSource == AudioSource.ONLINE || currentSource == AudioSource.BLUETOOTH) {
                                        PowerampNotificationListener.skipToNext(context)
                                    } else {
                                        XavierLocalPlayer.skipToNext()
                                    }
                                },
                                modifier = Modifier
                                    .size(38.dp)
                                    .background(Color(0xFF0F0B1E).copy(alpha = 0.6f), CircleShape)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.SkipNext,
                                    contentDescription = "Siguiente",
                                    tint = Color.White,
                                    modifier = Modifier.size(20.dp)
                                )
                            }

                            // Repetir
                            val isRepeat = playbackMode == PlaybackMode.REPEAT_ONE
                            IconButton(
                                onClick = { XavierLocalPlayer.toggleRepeat() },
                                modifier = Modifier.size(34.dp)
                            ) {
                                Icon(
                                    imageVector = if (isRepeat) Icons.Default.RepeatOne else Icons.Default.Repeat,
                                    contentDescription = "Repetir",
                                    tint = if (isRepeat) Color(0xFF38BDF8) else Color(0xFF64748B),
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }

                        // ── 1.4 Salto -10s/+10s y Barra de Volumen ───────────
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .border(BorderStroke(0.5.dp, Color(0x33A855F7)), RoundedCornerShape(10.dp))
                                .background(Color(0xFF0B0718).copy(alpha = 0.5f), RoundedCornerShape(10.dp))
                                .padding(horizontal = 6.dp, vertical = 3.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            // -10s
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(Color(0xFF1E1438))
                                    .clickable {
                                        if (currentSource == AudioSource.ONLINE || currentSource == AudioSource.BLUETOOTH) {
                                            val target = (effectiveProgressMs - 10000L).coerceAtLeast(0L)
                                            PowerampNotificationListener.seekTo(context, target)
                                        } else {
                                            XavierLocalPlayer.skipSeconds(-10)
                                        }
                                    }
                                    .padding(horizontal = 7.dp, vertical = 4.dp)
                            ) {
                                Text("-10s", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color.White)
                            }

                            // Volumen
                            Icon(
                                imageVector = Icons.Default.VolumeUp,
                                contentDescription = "Volumen",
                                tint = Color(0xFFC084FC),
                                modifier = Modifier.size(14.dp)
                            )

                            Slider(
                                value = currentVolume,
                                onValueChange = { newVol ->
                                    currentVolume = newVol
                                    audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, newVol.toInt(), 0)
                                },
                                valueRange = 0f..maxVolume,
                                colors = SliderDefaults.colors(
                                    thumbColor = Color(0xFFC084FC),
                                    activeTrackColor = Color(0xFFA855F7),
                                    inactiveTrackColor = Color(0xFF1E1438)
                                ),
                                modifier = Modifier
                                    .weight(1f)
                                    .height(16.dp)
                            )

                            // +10s
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(Color(0xFF1E1438))
                                    .clickable {
                                        if (currentSource == AudioSource.ONLINE || currentSource == AudioSource.BLUETOOTH) {
                                            val target = (effectiveProgressMs + 10000L).coerceAtMost(mediaState.durationMs)
                                            PowerampNotificationListener.seekTo(context, target)
                                        } else {
                                            XavierLocalPlayer.skipSeconds(10)
                                        }
                                    }
                                    .padding(horizontal = 7.dp, vertical = 4.dp)
                            ) {
                                Text("+10s", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color.White)
                            }
                        }
                    }
                }
            }
        }

        // =====================================================================
        // 2. COLUMNA CENTRAL (36%): EXPLORADOR MUSICAL
        // =====================================================================
        if (!isKaraokeExpanded) {
            Card(
                modifier = Modifier
                    .weight(0.36f)
                    .fillMaxHeight(),
                shape = RoundedCornerShape(22.dp),
                border = BorderStroke(1.dp, Color(0x6B1E3A8A)),
                colors = CardDefaults.cardColors(containerColor = Color.Transparent)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.linearGradient(
                                colors = listOf(Color(0xFF0B1326), Color(0xFF080E1C))
                            )
                        )
                        .padding(14.dp)
                ) {
                    val filteredTracks = remember(playlist, searchQuery, activeLibraryTab, selectedArtistFilter, selectedFolderFilter, favorites) {
                        playlist.filter { track ->
                            val folder = File(track.path).parentFile?.name ?: "Música"
                            val matchesSearch = searchQuery.isBlank() ||
                                    track.title.contains(searchQuery, ignoreCase = true) ||
                                    track.artist.contains(searchQuery, ignoreCase = true) ||
                                    track.album.contains(searchQuery, ignoreCase = true) ||
                                    folder.contains(searchQuery, ignoreCase = true)

                            val matchesTab = when (activeLibraryTab) {
                                2 -> FavoritesManager.isFavorite(track.path)
                                else -> true
                            }

                            val matchesArtist = selectedArtistFilter == null || track.artist.equals(selectedArtistFilter, ignoreCase = true)
                            val matchesFolder = selectedFolderFilter == null || folder.equals(selectedFolderFilter, ignoreCase = true)

                            matchesSearch && matchesTab && matchesArtist && matchesFolder
                        }
                    }

                    val tracksListState = rememberLazyListState()
                    val activeTrackPath = XavierLocalPlayer.currentTrack?.path ?: ""
                    val activePlayingIndex = remember(activeTrackPath, mediaState.title, filteredTracks) {
                        if (activeTrackPath.isNotBlank()) {
                            val idx = filteredTracks.indexOfFirst { it.path == activeTrackPath }
                            if (idx >= 0) idx else filteredTracks.indexOfFirst { it.title.equals(mediaState.title, ignoreCase = true) }
                        } else {
                            filteredTracks.indexOfFirst { it.title.equals(mediaState.title, ignoreCase = true) }
                        }
                    }

                    // Auto-scroll strictly when the playing track changes to position it in view
                    LaunchedEffect(activeTrackPath, mediaState.title) {
                        if (activePlayingIndex >= 0) {
                            val targetScroll = (activePlayingIndex - 1).coerceAtLeast(0)
                            tracksListState.animateScrollToItem(targetScroll)
                        }
                    }

                    Column(modifier = Modifier.fillMaxSize()) {

                        // ── 2.1 Cabecera Explorador + Conteo + Volver ─────────
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.QueueMusic,
                                        contentDescription = null,
                                        tint = Color(0xFF38BDF8),
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Text(
                                        text = "Explorador Musical",
                                        fontSize = 15.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = Color.White
                                    )
                                }
                                Text(
                                    text = when {
                                        selectedArtistFilter != null -> "Artista: $selectedArtistFilter"
                                        selectedFolderFilter != null -> "Carpeta: $selectedFolderFilter"
                                        activeLibraryTab == 2 -> "${playlist.count { FavoritesManager.isFavorite(it.path) }} favoritos"
                                        else -> "${playlist.size} pistas en la biblioteca"
                                    },
                                    fontSize = 11.sp,
                                    color = Color(0xFF94A3B8),
                                    modifier = Modifier.padding(top = 1.dp)
                                )
                            }

                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                // Chip de pista actual en reproducción (acceso directo / reubicación)
                                if (activePlayingIndex >= 0) {
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(Color(0xFF2563EB).copy(alpha = 0.25f))
                                            .border(BorderStroke(1.dp, Color(0xFF38BDF8).copy(alpha = 0.45f)), RoundedCornerShape(8.dp))
                                            .clickable {
                                                coroutineScope.launch {
                                                    val target = (activePlayingIndex - 1).coerceAtLeast(0)
                                                    tracksListState.animateScrollToItem(target)
                                                }
                                            }
                                            .padding(horizontal = 7.dp, vertical = 4.dp)
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                                        ) {
                                            AnimatedEqualizerBars(modifier = Modifier.size(11.dp))
                                            Text(
                                                text = "Pista #${activePlayingIndex + 1}",
                                                fontSize = 10.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = Color(0xFF38BDF8)
                                            )
                                        }
                                    }
                                }

                                if (selectedArtistFilter != null || selectedFolderFilter != null) {
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(Color(0xFF0284C7).copy(alpha = 0.25f))
                                            .border(BorderStroke(1.dp, Color(0xFF38BDF8).copy(alpha = 0.5f)), RoundedCornerShape(8.dp))
                                            .clickable {
                                                selectedArtistFilter = null
                                                selectedFolderFilter = null
                                            }
                                            .padding(horizontal = 8.dp, vertical = 4.dp)
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(3.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.ArrowBack,
                                                contentDescription = "Volver",
                                                tint = Color(0xFF38BDF8),
                                                modifier = Modifier.size(12.dp)
                                            )
                                            Text(
                                                text = "Volver",
                                                fontSize = 10.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = Color(0xFF38BDF8)
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        // ── 2.2 Buscador Dinámico ─────────────────────────────
                        OutlinedTextField(
                            value = searchQuery,
                            onValueChange = { searchQuery = it },
                            placeholder = {
                                Text(
                                    text = "Buscar canción, artista o carpeta...",
                                    fontSize = 11.sp,
                                    color = Color(0xFF64748B)
                                )
                            },
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Default.Search,
                                    contentDescription = "Buscar",
                                    tint = Color(0xFF64748B),
                                    modifier = Modifier.size(16.dp)
                                )
                            },
                            trailingIcon = {
                                if (searchQuery.isNotEmpty()) {
                                    IconButton(onClick = { searchQuery = "" }) {
                                        Icon(
                                            imageVector = Icons.Default.Close,
                                            contentDescription = "Limpiar",
                                            tint = Color(0xFF94A3B8),
                                            modifier = Modifier.size(14.dp)
                                        )
                                    }
                                }
                            },
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedContainerColor = Color(0xFF030712).copy(alpha = 0.9f),
                                unfocusedContainerColor = Color(0xFF030712).copy(alpha = 0.8f),
                                focusedBorderColor = Color(0xFF3B82F6),
                                unfocusedBorderColor = Color(0xFF1E293B),
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White
                            ),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(46.dp)
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        // ── 2.3 Las 4 Pestañas: Lista | Artistas | Favoritos | Carpetas
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(Color(0xFF030712).copy(alpha = 0.9f), RoundedCornerShape(12.dp))
                                .border(BorderStroke(1.dp, Color(0xFF1E293B)), RoundedCornerShape(12.dp))
                                .padding(3.dp),
                            horizontalArrangement = Arrangement.spacedBy(3.dp)
                        ) {
                            listOf(
                                Pair(0, "Lista"),
                                Pair(1, "Artistas"),
                                Pair(2, "Favoritos"),
                                Pair(3, "Carpetas")
                            ).forEach { (idx, title) ->
                                val isSelected = activeLibraryTab == idx
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(if (isSelected) Color(0xFF2563EB) else Color.Transparent)
                                        .clickable {
                                            activeLibraryTab = idx
                                            selectedArtistFilter = null
                                            selectedFolderFilter = null
                                        }
                                        .padding(vertical = 5.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = title,
                                        fontSize = 11.sp,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                        color = if (isSelected) Color.White else Color(0xFF94A3B8)
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        // Vistas por Pestaña
                        when {
                            // 2.4.1 Vista de Artistas (si no hay uno seleccionado)
                            activeLibraryTab == 1 && selectedArtistFilter == null -> {
                                val artistsMap = remember(playlist, searchQuery) {
                                    playlist.groupBy { it.artist.ifBlank { "Desconocido" } }
                                        .filter { (artist, _) -> searchQuery.isBlank() || artist.contains(searchQuery, ignoreCase = true) }
                                }

                                LazyColumn(
                                    modifier = Modifier.fillMaxSize(),
                                    verticalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    items(artistsMap.keys.toList()) { artistName ->
                                        val artistTracks = artistsMap[artistName] ?: emptyList()
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .clip(RoundedCornerShape(14.dp))
                                                .background(Color(0xFF0F172A).copy(alpha = 0.8f))
                                                .border(BorderStroke(1.dp, Color(0xFF1E293B)), RoundedCornerShape(14.dp))
                                                .clickable { selectedArtistFilter = artistName }
                                                .padding(10.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                                                modifier = Modifier.weight(1f)
                                            ) {
                                                Box(
                                                    modifier = Modifier
                                                        .size(38.dp)
                                                        .clip(CircleShape)
                                                        .background(Color(0xFF1E3A8A).copy(alpha = 0.6f))
                                                        .border(BorderStroke(1.dp, Color(0xFF38BDF8).copy(alpha = 0.4f)), CircleShape),
                                                    contentAlignment = Alignment.Center
                                                ) {
                                                    Icon(
                                                        imageVector = Icons.Default.Person,
                                                        contentDescription = null,
                                                        tint = Color(0xFF38BDF8),
                                                        modifier = Modifier.size(20.dp)
                                                    )
                                                }
                                                Column {
                                                    Text(
                                                        text = artistName,
                                                        fontSize = 13.sp,
                                                        fontWeight = FontWeight.Bold,
                                                        color = Color.White
                                                    )
                                                    Text(
                                                        text = "${artistTracks.size} canciones",
                                                        fontSize = 11.sp,
                                                        color = Color(0xFF94A3B8)
                                                    )
                                                }
                                            }
                                            Icon(
                                                imageVector = Icons.Default.ChevronRight,
                                                contentDescription = null,
                                                tint = Color(0xFF64748B),
                                                modifier = Modifier.size(16.dp)
                                            )
                                        }
                                    }
                                }
                            }

                            // 2.4.2 Vista de Carpetas (si no hay una seleccionada)
                            activeLibraryTab == 3 && selectedFolderFilter == null -> {
                                val foldersMap = remember(playlist, searchQuery) {
                                    playlist.groupBy { File(it.path).parentFile?.name ?: "Música" }
                                        .filter { (folder, _) -> searchQuery.isBlank() || folder.contains(searchQuery, ignoreCase = true) }
                                }

                                LazyColumn(
                                    modifier = Modifier.fillMaxSize(),
                                    verticalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    items(foldersMap.keys.toList()) { folderName ->
                                        val folderTracks = foldersMap[folderName] ?: emptyList()
                                        val samplePath = folderTracks.firstOrNull()?.path?.let { File(it).parent } ?: ""
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .clip(RoundedCornerShape(14.dp))
                                                .background(Color(0xFF0F172A).copy(alpha = 0.8f))
                                                .border(BorderStroke(1.dp, Color(0xFF1E293B)), RoundedCornerShape(14.dp))
                                                .clickable { selectedFolderFilter = folderName }
                                                .padding(10.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                                                modifier = Modifier.weight(1f)
                                            ) {
                                                Box(
                                                    modifier = Modifier
                                                        .size(38.dp)
                                                        .clip(RoundedCornerShape(10.dp))
                                                        .background(Color(0xFF1E293B))
                                                        .border(BorderStroke(1.dp, Color(0xFFF59E0B).copy(alpha = 0.4f)), RoundedCornerShape(10.dp)),
                                                    contentAlignment = Alignment.Center
                                                ) {
                                                    Icon(
                                                        imageVector = Icons.Outlined.Folder,
                                                        contentDescription = null,
                                                        tint = Color(0xFFF59E0B),
                                                        modifier = Modifier.size(20.dp)
                                                    )
                                                }
                                                Column(modifier = Modifier.weight(1f)) {
                                                    Text(
                                                        text = folderName,
                                                        fontSize = 13.sp,
                                                        fontWeight = FontWeight.Bold,
                                                        color = Color.White,
                                                        maxLines = 1,
                                                        overflow = TextOverflow.Ellipsis
                                                    )
                                                    Text(
                                                        text = samplePath,
                                                        fontSize = 10.sp,
                                                        fontFamily = FontFamily.Monospace,
                                                        color = Color(0xFF38BDF8).copy(alpha = 0.8f),
                                                        maxLines = 1,
                                                        overflow = TextOverflow.Ellipsis
                                                    )
                                                }
                                            }
                                            Text(
                                                text = "${folderTracks.size} pistas",
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = Color(0xFFCBD5E1),
                                                modifier = Modifier
                                                    .background(Color(0xFF020617), RoundedCornerShape(6.dp))
                                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                                            )
                                        }
                                    }
                                }
                            }

                            // 2.4.3 Lista de Canciones
                            else -> {
                                if (filteredTracks.isEmpty()) {
                                    Box(
                                        modifier = Modifier.fillMaxSize(),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = if (activeLibraryTab == 2) "No tienes canciones favoritas aún.\nToca el corazón en cualquier canción." else "No se encontraron canciones",
                                            fontSize = 12.sp,
                                            color = Color(0xFF64748B),
                                            textAlign = TextAlign.Center
                                        )
                                    }
                                } else {
                                    LazyColumn(
                                        state = tracksListState,
                                        modifier = Modifier.fillMaxSize(),
                                        verticalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        itemsIndexed(filteredTracks) { index, track ->
                                            val isCurrentPlaying = (activeTrackPath.isNotBlank() && track.path == activeTrackPath) ||
                                                    track.title.equals(mediaState.title, ignoreCase = true)
                                            val isFav = FavoritesManager.isFavorite(track.path)
                                            val folder = File(track.path).parentFile?.name ?: "Música"

                                            val rowBrush = if (isCurrentPlaying) {
                                                Brush.horizontalGradient(
                                                    listOf(Color(0xFF2563EB).copy(alpha = 0.35f), Color(0xFF9333EA).copy(alpha = 0.25f))
                                                )
                                            } else {
                                                Brush.horizontalGradient(
                                                    listOf(Color(0xFF0F172A).copy(alpha = 0.75f), Color(0xFF0F172A).copy(alpha = 0.75f))
                                                )
                                            }

                                            Row(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .clip(RoundedCornerShape(14.dp))
                                                    .background(rowBrush)
                                                    .border(
                                                        BorderStroke(
                                                            1.dp,
                                                            if (isCurrentPlaying) Color(0xFF38BDF8).copy(alpha = 0.6f) else Color(0xFF1E293B)
                                                        ),
                                                        RoundedCornerShape(14.dp)
                                                    )
                                                    .clickable {
                                                        val contextTitle = when {
                                                            selectedArtistFilter != null -> "Artista: $selectedArtistFilter"
                                                            selectedFolderFilter != null -> "Carpeta: $selectedFolderFilter"
                                                            activeLibraryTab == 2 -> "Favoritos"
                                                            else -> "Biblioteca"
                                                        }
                                                        XavierLocalPlayer.playTrackInQueue(track, filteredTracks, contextTitle)
                                                    }
                                                    .padding(horizontal = 8.dp, vertical = 6.dp),
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.SpaceBetween
                                            ) {
                                                Row(
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                                    modifier = Modifier.weight(1f)
                                                ) {
                                                    // Número de Pista
                                                    Text(
                                                        text = "${index + 1}",
                                                        fontSize = 11.sp,
                                                        fontWeight = FontWeight.Bold,
                                                        color = if (isCurrentPlaying) Color(0xFF38BDF8) else Color(0xFF64748B),
                                                        modifier = Modifier.width(18.dp),
                                                        textAlign = TextAlign.Center
                                                    )

                                                    // Miniatura + EQ Animado si está sonando
                                                    Box(
                                                        modifier = Modifier
                                                            .size(36.dp)
                                                            .clip(RoundedCornerShape(8.dp))
                                                            .background(Color(0xFF1E293B))
                                                            .border(
                                                                BorderStroke(
                                                                    1.dp,
                                                                    if (isCurrentPlaying) Color(0xFF38BDF8) else Color(0xFF334155)
                                                                ),
                                                                RoundedCornerShape(8.dp)
                                                            ),
                                                        contentAlignment = Alignment.Center
                                                    ) {
                                                        if (isCurrentPlaying && mediaState.isPlaying) {
                                                            AnimatedEqualizerBars(modifier = Modifier.size(16.dp))
                                                        } else {
                                                            Icon(
                                                                imageVector = Icons.Default.MusicNote,
                                                                contentDescription = null,
                                                                tint = if (isCurrentPlaying) Color(0xFF38BDF8) else Color(0xFF64748B),
                                                                modifier = Modifier.size(16.dp)
                                                            )
                                                        }
                                                    }

                                                    // Datos de la Pista
                                                    Column(modifier = Modifier.weight(1f)) {
                                                        Text(
                                                            text = track.title,
                                                            fontSize = 12.sp,
                                                            fontWeight = FontWeight.Bold,
                                                            color = if (isCurrentPlaying) Color(0xFF38BDF8) else Color.White,
                                                            maxLines = 1,
                                                            overflow = TextOverflow.Ellipsis
                                                        )
                                                        Text(
                                                            text = "${track.artist} · ${track.album.ifBlank { folder }}",
                                                            fontSize = 10.sp,
                                                            color = Color(0xFF94A3B8),
                                                            maxLines = 1,
                                                            overflow = TextOverflow.Ellipsis
                                                        )
                                                    }
                                                }

                                                // Favorito + Duración
                                                Row(
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                                ) {
                                                    IconButton(
                                                        onClick = {
                                                            FavoritesManager.toggleFavorite(track.path)
                                                        },
                                                        modifier = Modifier.size(28.dp)
                                                    ) {
                                                        Icon(
                                                            imageVector = if (isFav) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                                                            contentDescription = "Favorito",
                                                            tint = if (isFav) Color(0xFFF43F5E) else Color(0xFF475569),
                                                            modifier = Modifier.size(14.dp)
                                                        )
                                                    }

                                                    Text(
                                                        text = formatDuration(track.durationMs),
                                                        fontSize = 11.sp,
                                                        fontFamily = FontFamily.Monospace,
                                                        fontWeight = FontWeight.Bold,
                                                        color = if (isCurrentPlaying) Color(0xFF38BDF8) else Color(0xFF94A3B8)
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // =====================================================================
        // 3. COLUMNA DERECHA (37% ó 73% Expandida): LETRA EN VIVO / KARAOKE XL
        // =====================================================================
        Card(
            modifier = Modifier
                .weight(if (isKaraokeExpanded) 0.73f else 0.37f)
                .fillMaxHeight(),
            shape = RoundedCornerShape(22.dp),
            border = BorderStroke(1.dp, Color(0x526366F1)),
            colors = CardDefaults.cardColors(containerColor = Color.Transparent)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.linearGradient(
                            colors = listOf(Color(0xFF1B1738), Color(0xFF0E1024))
                        )
                    )
                    .padding(14.dp)
            ) {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.SpaceBetween
                ) {

                    // ── 3.1 Cabecera Karaoke + EN/ES + A-/A+ + Expandir ──────
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(BorderStroke(0.dp, Color.Transparent))
                            .padding(bottom = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(34.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(Color(0xFF4F46E5).copy(alpha = 0.25f))
                                    .border(BorderStroke(1.dp, Color(0xFF818CF8).copy(alpha = 0.4f)), RoundedCornerShape(10.dp)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.MusicNote,
                                    contentDescription = null,
                                    tint = Color(0xFFA5B4FC),
                                    modifier = Modifier.size(17.dp)
                                )
                            }
                            Column {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Text(
                                        text = "Letra en Vivo",
                                        fontSize = 15.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = Color.White
                                    )
                                    Text(
                                        text = "● Sincronizada",
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF10B981),
                                        modifier = Modifier
                                            .background(Color(0xFF064E3B).copy(alpha = 0.7f), RoundedCornerShape(10.dp))
                                            .border(BorderStroke(0.5.dp, Color(0xFF10B981).copy(alpha = 0.4f)), RoundedCornerShape(10.dp))
                                            .padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                                Text(
                                    text = "Toca cualquier línea para saltar a ese verso",
                                    fontSize = 10.sp,
                                    color = Color(0xFF94A3B8)
                                )
                            }
                        }

                        // Botones de Acción
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            // EN / ES (Traducción Bilingüe)
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(if (showTranslation) Color(0xFF4F46E5).copy(alpha = 0.5f) else Color(0xFF312E81).copy(alpha = 0.35f))
                                    .border(
                                        BorderStroke(
                                            1.dp,
                                            if (showTranslation) Color(0xFF818CF8) else Color(0xFF6366F1).copy(alpha = 0.4f)
                                        ),
                                        RoundedCornerShape(10.dp)
                                    )
                                    .clickable {
                                        showTranslation = !showTranslation
                                        if (showTranslation) {
                                            XavierLocalPlayer.ensureLyricsTranslation()
                                        }
                                    }
                                    .padding(horizontal = 8.dp, vertical = 5.dp)
                            ) {
                                Text(
                                    text = "EN / ES",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (showTranslation) Color.White else Color(0xFFC7D2FE)
                                )
                            }

                            // A- (Reducir Fuente)
                            Box(
                                modifier = Modifier
                                    .size(28.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(Color(0xFF0F172A))
                                    .border(BorderStroke(1.dp, Color(0xFF334155)), RoundedCornerShape(8.dp))
                                    .clickable { if (fontScale > -1) fontScale-- },
                                contentAlignment = Alignment.Center
                            ) {
                                Text("A-", fontSize = 10.sp, fontWeight = FontWeight.Black, color = Color(0xFFE2E8F0))
                            }

                            // A+ (Aumentar Fuente)
                            Box(
                                modifier = Modifier
                                    .size(28.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(Color(0xFF0F172A))
                                    .border(BorderStroke(1.dp, Color(0xFF334155)), RoundedCornerShape(8.dp))
                                    .clickable { if (fontScale < 3) fontScale++ },
                                contentAlignment = Alignment.Center
                            ) {
                                Text("A+", fontSize = 10.sp, fontWeight = FontWeight.Black, color = Color(0xFFE2E8F0))
                            }

                            // Expandir / Reducir Karaoke XL
                            Box(
                                modifier = Modifier
                                    .size(28.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(Color(0xFF0284C7).copy(alpha = 0.25f))
                                    .border(BorderStroke(1.dp, Color(0xFF38BDF8).copy(alpha = 0.4f)), RoundedCornerShape(8.dp))
                                    .clickable { isKaraokeExpanded = !isKaraokeExpanded },
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = if (isKaraokeExpanded) Icons.Default.FullscreenExit else Icons.Default.Fullscreen,
                                    contentDescription = "Expandir Karaoke",
                                    tint = Color(0xFF38BDF8),
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }

                    // ── 3.2 Visor de Letras con Desplazamiento Suave ─────────
                    val lyricsList = mediaState.fullLyrics
                    val lyricsState = rememberLazyListState()

                    // Encuentra el índice activo
                    val activeIndex = remember(lyricsList, effectiveProgressMs) {
                        if (lyricsList.isEmpty()) -1
                        else lyricsList.indexOfLast { it.timeMs <= effectiveProgressMs }.coerceAtLeast(0)
                    }

                    // Auto-scroll centrado en el verso activo
                    LaunchedEffect(activeIndex) {
                        if (activeIndex >= 0 && lyricsList.isNotEmpty()) {
                            val target = (activeIndex - 2).coerceAtLeast(0)
                            lyricsState.animateScrollToItem(target)
                        }
                    }

                    if (lyricsList.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(
                                    imageVector = Icons.Default.Lyrics,
                                    contentDescription = null,
                                    tint = Color(0xFF6366F1).copy(alpha = 0.5f),
                                    modifier = Modifier.size(40.dp)
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = if (mediaState.title.isNotBlank()) "Buscando letras sincronizadas..." else "Sin reproducción activa",
                                    fontSize = 12.sp,
                                    color = Color(0xFF94A3B8)
                                )
                            }
                        }
                    } else {
                        val baseFontSize = 16 + (fontScale * 3)

                        LazyColumn(
                            state = lyricsState,
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f)
                                .padding(vertical = 4.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            itemsIndexed(lyricsList) { idx, entry ->
                                val isActive = idx == activeIndex
                                val translation = entry.translation ?: LyricsTranslationManager.getTranslationSync(entry.text)

                                val lyricBrush = if (isActive) {
                                    Brush.horizontalGradient(
                                        listOf(Color(0xFF2563EB).copy(alpha = 0.35f), Color(0xFF2563EB).copy(alpha = 0.06f))
                                    )
                                } else {
                                    Brush.horizontalGradient(
                                        listOf(Color.Transparent, Color.Transparent)
                                    )
                                }

                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(lyricBrush)
                                        .clickable {
                                            if (currentSource == AudioSource.ONLINE || currentSource == AudioSource.BLUETOOTH) {
                                                PowerampNotificationListener.seekTo(context, entry.timeMs)
                                            } else {
                                                XavierLocalPlayer.seekTo(entry.timeMs)
                                            }
                                        }
                                        .padding(horizontal = 12.dp, vertical = 8.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = entry.text,
                                                fontSize = (if (isActive) baseFontSize + 2 else baseFontSize).sp,
                                                lineHeight = (if (isActive) baseFontSize + 7 else baseFontSize + 5).sp,
                                                fontWeight = if (isActive) FontWeight.ExtraBold else FontWeight.Medium,
                                                color = if (isActive) Color(0xFF60A5FA) else Color(0xFFCBD5E1).copy(alpha = 0.7f)
                                            )

                                            // Traducción al Español
                                            if (showTranslation && !translation.isNullOrBlank()) {
                                                Text(
                                                    text = translation,
                                                    fontSize = (baseFontSize - 3).coerceAtLeast(11).sp,
                                                    fontWeight = if (isActive) FontWeight.SemiBold else FontWeight.Normal,
                                                    color = if (isActive) Color(0xFFBAE6FD) else Color(0xFF818CF8).copy(alpha = 0.75f),
                                                    modifier = Modifier.padding(top = 2.dp)
                                                )
                                            }
                                        }

                                        Text(
                                            text = formatDuration(entry.timeMs),
                                            fontSize = 11.sp,
                                            fontFamily = FontFamily.Monospace,
                                            fontWeight = FontWeight.Bold,
                                            color = if (isActive) Color(0xFF38BDF8) else Color(0xFF475569),
                                            modifier = Modifier.padding(start = 8.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // ── 3.3 Pie de Página Karaoke XL ─────────────────────────
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(BorderStroke(0.dp, Color.Transparent))
                            .padding(top = 6.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            AnimatedEqualizerBars(modifier = Modifier.size(16.dp))
                            Text(
                                text = "${mediaState.artist} — ${mediaState.title}",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF38BDF8),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }

                        Text(
                            text = "Modo Karaoke XL",
                            fontSize = 10.sp,
                            color = Color(0xFF94A3B8)
                        )
                    }
                }
            }
        }
    }
}

/**
 * Animated equalizer visualizer with 3 bouncing bars for active audio tracks.
 */
@Composable
fun AnimatedEqualizerBars(modifier: Modifier = Modifier) {
    val infiniteTransition = rememberInfiniteTransition(label = "eq")

    val h1 by infiniteTransition.animateFloat(
        initialValue = 4f,
        targetValue = 15f,
        animationSpec = infiniteRepeatable(
            animation = tween(450, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "h1"
    )

    val h2 by infiniteTransition.animateFloat(
        initialValue = 14f,
        targetValue = 5f,
        animationSpec = infiniteRepeatable(
            animation = tween(550, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "h2"
    )

    val h3 by infiniteTransition.animateFloat(
        initialValue = 6f,
        targetValue = 16f,
        animationSpec = infiniteRepeatable(
            animation = tween(500, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "h3"
    )

    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(2.dp),
        verticalAlignment = Alignment.Bottom
    ) {
        Box(
            modifier = Modifier
                .width(2.5.dp)
                .height(h1.dp)
                .clip(RoundedCornerShape(2.dp))
                .background(Color(0xFF38BDF8))
        )
        Box(
            modifier = Modifier
                .width(2.5.dp)
                .height(h2.dp)
                .clip(RoundedCornerShape(2.dp))
                .background(Color(0xFF38BDF8))
        )
        Box(
            modifier = Modifier
                .width(2.5.dp)
                .height(h3.dp)
                .clip(RoundedCornerShape(2.dp))
                .background(Color(0xFF38BDF8))
        )
    }
}

/**
 * Utility helper to format milliseconds into "mm:ss".
 */
private fun formatDuration(ms: Long): String {
    if (ms <= 0L) return "--:--"
    val totalSec = ms / 1000L
    val minutes = totalSec / 60L
    val seconds = totalSec % 60L
    return "%d:%02d".format(minutes, seconds)
}

