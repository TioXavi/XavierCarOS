package com.xavier.carlauncher.ui.screens

import android.Manifest
import android.content.Intent
import android.net.Uri
import android.os.Build
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.xavier.carlauncher.SpeedometerViewModel
import com.xavier.carlauncher.bluetooth.BluetoothViewModel
import com.xavier.carlauncher.canbus.DoorStateViewModel
import com.xavier.carlauncher.media.PowerampMediaViewModel
import com.xavier.carlauncher.ui.components.*
import com.xavier.carlauncher.system.AutomotiveIntentResolver
import com.xavier.carlauncher.ui.theme.DayNightManager
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.border
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.xavier.carlauncher.voice.OfflineVoiceController
import com.xavier.carlauncher.voice.CarVoiceAssistant
import com.xavier.carlauncher.weather.WeatherViewModel
import androidx.compose.foundation.ExperimentalFoundationApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun DashboardScreen() {
    val context = LocalContext.current

    // ─── ViewModels ───────────────────────────────────────────────────────────
    val speedometerVm: SpeedometerViewModel = viewModel()
    val mediaVm: PowerampMediaViewModel    = viewModel()
    val btVm: BluetoothViewModel           = viewModel()
    val doorVm: DoorStateViewModel         = viewModel()
    val weatherVm: WeatherViewModel        = viewModel()

    // ─── State collection ─────────────────────────────────────────────────────
    val gpsSpeedMph    by speedometerVm.speedMph.collectAsState()
    val mediaState     by mediaVm.mediaState.collectAsState()
    val headsetState   by btVm.headsetState.collectAsState()
    val a2dpState      by btVm.a2dpState.collectAsState()
    val doorState      by doorVm.doorState.collectAsState()
    val weatherData    by weatherVm.weatherState.collectAsState()

    // ─── Day/Night & Dimming Manager ──────────────────────────────────────────
    val dayNightManager = remember { DayNightManager(context) }
    val dimmingAlpha by dayNightManager.dimmingAlpha.collectAsState()

    // Active speed: GPS speed
    val displaySpeedMph = gpsSpeedMph

    // ─── Live Clock & Date ────────────────────────────────────────────────────
    var currentTime by remember { mutableStateOf("12:00") }
    var currentDate by remember { mutableStateOf("Hoy") }

    LaunchedEffect(Unit) {
        val timeFmt = SimpleDateFormat("HH:mm", Locale.getDefault())
        val dateFmt = SimpleDateFormat("EEE, d 'de' MMMM", Locale("es", "ES"))
        while (true) {
            val now = Date()
            currentTime = timeFmt.format(now)
            currentDate = dateFmt.format(now).replaceFirstChar { it.uppercase() }
            delay(1000L)
        }
    }

    // ─── Start background listeners ───────────────────────────────────────────
    LaunchedEffect(Unit) {
        speedometerVm.startListening()
        weatherVm.startAutoRefresh() // Uses real-time GPS coordinates (updates every 30 min)
    }

    // ─── Voice Assistant: Real-time Door Announcements & Offline Voice Control ──
    val voiceAssistant = remember { CarVoiceAssistant.getInstance(context) }
    val voiceController = remember { OfflineVoiceController.getInstance(context) }
    val isVoiceListening by voiceController.isListening.collectAsState()
    val recognizedVoiceText by voiceController.recognizedText.collectAsState()
    val assistantFeedback by voiceController.assistantFeedback.collectAsState()
    var previousDoorState by remember { mutableStateOf(doorState) }

    LaunchedEffect(doorState) {
        voiceAssistant.onDoorStateTransition(previousDoorState, doorState)
        previousDoorState = doorState
    }

    // ─── Waze Launcher ────────────────────────────────────────────────────────
    fun launchWaze() {
        val intent = context.packageManager.getLaunchIntentForPackage("com.waze")
            ?: Intent(Intent.ACTION_VIEW, Uri.parse("waze://?navigate=yes"))
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        try { context.startActivity(intent) } catch (_: Exception) {}
    }

    // ─── Navigation Dock selection & handling ──────────────────────────────────
    var selectedTab by remember { mutableStateOf(0) }
    var showAppDrawer by remember { mutableStateOf(false) }
    var currentPage by remember { mutableStateOf(0) } // 0 = Dashboard, 1 = Expanded Music Studio
    var dragAccumulator by remember { mutableFloatStateOf(0f) }

    fun onDockItemClick(index: Int) {
        selectedTab = index
        when (index) {
            0 -> {
                // Home: instant switch to dashboard, closes App Drawer
                showAppDrawer = false
                currentPage = 0
            }
            1 -> {
                // Waze GPS
                showAppDrawer = false
                launchWaze()
            }
            2 -> {
                // Phone / BT dialer (Universal across SYU, TopWay, Microntek, TS10, etc.)
                showAppDrawer = false
                AutomotiveIntentResolver.launchSafely(context, AutomotiveIntentResolver.getPhoneIntent(context))
            }
            3 -> {
                // Expanded Music Studio Screen: instant switch
                showAppDrawer = false
                currentPage = 1
            }
            4 -> {
                // Apps drawer (The 6 dots button - all installed apps)
                showAppDrawer = true
            }
            5 -> {
                // Car Settings (Universal across SYU, TopWay, Microntek, TS10, etc.)
                showAppDrawer = false
                AutomotiveIntentResolver.launchSafely(context, AutomotiveIntentResolver.getCarSettingsIntent(context))
            }
        }
    }

    // ─── Root Layout with Dimming factor ──────────────────────────────────────
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF060B19))
            .alpha(dimmingAlpha)
    ) {
        Row(modifier = Modifier.fillMaxSize()) {

            // ── Left Navigation Dock (56.dp) ──────────────────────────────────
            LeftNavigationDock(
                selectedTab   = selectedTab,
                onTabSelected = ::onDockItemClick,
                modifier      = Modifier
                    .width(56.dp)
                    .fillMaxHeight()
            )

            // ── Main Content Area ─────────────────────────────────────────────
            Column(modifier = Modifier.fillMaxSize()) {

                // ── Top Status Bar (48.dp) ────────────────────────────────────
                TopStatusBar(
                    currentTime = currentTime,
                    currentDate = currentDate,
                    outsideTemp = weatherData?.temp ?: 17,
                    modifier    = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                )

                // ── Instant Screen Switcher: Page 0 = Dashboard, Page 1 = Expanded Music Studio ──
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .weight(1f)
                        .pointerInput(currentPage) {
                            detectHorizontalDragGestures(
                                onDragStart = { dragAccumulator = 0f },
                                onHorizontalDrag = { _, dragAmount ->
                                    dragAccumulator += dragAmount
                                },
                                onDragEnd = {
                                    if (currentPage == 0 && dragAccumulator < -40f) {
                                        currentPage = 1
                                        selectedTab = 3
                                    } else if (currentPage == 1 && dragAccumulator > 40f) {
                                        currentPage = 0
                                        selectedTab = 0
                                    }
                                }
                            )
                        }
                ) {
                    if (currentPage == 0) {
                        // ── 3-Column Asymmetric Grid ──────────────────────────────────
                        Row(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(horizontal = 8.dp, vertical = 8.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {

                            // 1. COLUMNA IZQUIERDA (42%): Auto Toyota Corolla + Puertas + Clima
                            VehicleCard(
                                doorState   = doorState,
                                weatherData = weatherData,
                                modifier    = Modifier
                                    .weight(0.42f)
                                    .fillMaxHeight(),
                                onCarDetailsClick = { /* Abre panel de telemetría / OBD2 */ }
                            )

                            // 2. COLUMNA CENTRAL (33%): Navegación (45%) + Reproductor Pantalla de Fábrica (55%)
                            Column(
                                modifier = Modifier
                                    .weight(0.33f)
                                    .fillMaxHeight(),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                NavigationMapCard(
                                    modifier   = Modifier
                                        .weight(0.45f)
                                        .fillMaxWidth(),
                                    onMapClick = ::launchWaze
                                )
                                MusicPlayerCard(
                                    mediaState      = mediaState,
                                    onPrevious      = { mediaVm.skipToPrevious(context) },
                                    onPlayPause     = { mediaVm.togglePlayPause(context) },
                                    onNext          = { mediaVm.skipToNext(context) },
                                    onSeek          = { pos -> mediaVm.seekTo(context, pos) },
                                    onToggleShuffle = { mediaVm.toggleShuffle(context) },
                                    onToggleRepeat  = { mediaVm.toggleRepeat(context) },
                                    onLoadLrc       = { uri -> mediaVm.loadLrcFromUri(context, uri) },
                                    onOpenPlayer    = {
                                        currentPage = 1
                                        selectedTab = 3
                                    },
                                    modifier        = Modifier
                                        .weight(0.55f)
                                        .fillMaxWidth()
                                )
                            }

                            // 3. COLUMNA DERECHA (25%): Letra en Vivo + Velocímetro + Bluetooth Audio
                            Column(
                                modifier = Modifier
                                    .weight(0.25f)
                                    .fillMaxHeight(),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                MusicLyricsCard(
                                    mediaState = mediaState,
                                    onSeek     = { pos -> mediaVm.seekTo(context, pos) },
                                    modifier   = Modifier
                                        .weight(1.2f)
                                        .fillMaxWidth()
                                        .clickable {
                                            currentPage = 1
                                            selectedTab = 3
                                        }
                                )
                                SpeedometerCard(
                                    speedMph = displaySpeedMph,
                                    modifier = Modifier
                                        .weight(1f)
                                        .fillMaxWidth()
                                )
                                BluetoothAudioCard(
                                    connectionState = a2dpState,
                                    modifier        = Modifier
                                        .weight(0.8f)
                                        .fillMaxWidth()
                                )
                            }
                        }
                    } else {
                        // ── PÁGINA 1: REPRODUCTOR MUSICAL AMPLIADO ─────────────
                        ExpandedMusicScreen(
                            mediaState = mediaState,
                            onNavigateBack = {
                                currentPage = 0
                                selectedTab = 0
                            }
                        )
                    }
                }
            }
        }

        // ── Full-Screen Native Automotive App Drawer ─────────────────────────
        AppDrawerScreen(
            isOpen = showAppDrawer,
            onClose = {
                showAppDrawer = false
                selectedTab = 0
            }
        )

        // ── Voice Assistant Floating HUD Banner ──────────────────────────────
        if (isVoiceListening || (assistantFeedback.isNotBlank() && assistantFeedback != "Listo para escuchar.")) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 10.dp)
                    .clip(RoundedCornerShape(20.dp))
                    .background(Color(0xF00A1026))
                    .border(
                        BorderStroke(
                            1.5.dp,
                            if (isVoiceListening) Color(0xFF38BDF8) else Color(0xFF9333EA)
                        ),
                        RoundedCornerShape(20.dp)
                    )
                    .clickable { voiceController.stopListening() }
                    .padding(horizontal = 20.dp, vertical = 8.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Mic,
                        contentDescription = "Voice Assistant",
                        tint = if (isVoiceListening) Color(0xFF38BDF8) else Color(0xFFA855F7),
                        modifier = Modifier.size(24.dp)
                    )
                    Column {
                        Text(
                            text = if (isVoiceListening) "🎙️ Escuchando comando..." else "Comando de Voz",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isVoiceListening) Color(0xFF38BDF8) else Color(0xFFA855F7)
                        )
                        Text(
                            text = if (recognizedVoiceText.isNotBlank()) "\"$recognizedVoiceText\"" else assistantFeedback,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium,
                            color = Color.White
                        )
                    }
                }
            }
        }
    }
}
