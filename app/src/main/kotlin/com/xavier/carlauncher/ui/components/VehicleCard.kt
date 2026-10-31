package com.xavier.carlauncher.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.Highlight
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Tune
import com.xavier.carlauncher.illumination.VehicleIlluminationManager
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.xavier.carlauncher.R
import com.xavier.carlauncher.canbus.DoorState
import com.xavier.carlauncher.carassets.CarAssetManager
import com.xavier.carlauncher.carassets.CarSlot
import com.xavier.carlauncher.ui.theme.ColorAccentGreen
import com.xavier.carlauncher.ui.theme.ColorBorder
import com.xavier.carlauncher.ui.theme.ColorGrayMid
import com.xavier.carlauncher.ui.theme.ColorPrimaryVariant
import com.xavier.carlauncher.ui.theme.ColorSurface
import com.xavier.carlauncher.ui.theme.ColorSurfaceDeep
import com.xavier.carlauncher.ui.theme.XavierTheme
import com.xavier.carlauncher.weather.WeatherData

// ── VehicleCard ───────────────────────────────────────────────────────────────

/**
 * Full vehicle overview card: greeting, door‑state overlay, weather widget.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun VehicleCard(
    doorState:   DoorState,
    weatherData: WeatherData?,
    modifier:    Modifier = Modifier,
    onCarDetailsClick: () -> Unit = {}
) {
    val colors = XavierTheme.colors
    val cardBrush = remember(colors) {
        Brush.verticalGradient(listOf(colors.surface, colors.surfaceDeep))
    }
    var showAssetModal by remember { mutableStateOf(false) }
    var showThemeModal by remember { mutableStateOf(false) }
    var assetRefreshKey by remember { mutableStateOf(0) }
    val isLightsOn by VehicleIlluminationManager.isLightsOn.collectAsState()

    val context = LocalContext.current
    val userPrefs = remember(context) { context.getSharedPreferences("xavier_user_prefs", android.content.Context.MODE_PRIVATE) }
    var currentUserName by remember { mutableStateOf(userPrefs.getString("user_custom_name", "Xavier") ?: "Xavier") }
    var showNameDialog by remember { mutableStateOf(false) }
    var editedNameText by remember { mutableStateOf("") }

    Card(
        modifier = modifier,
        shape    = RoundedCornerShape(24.dp),
        border   = BorderStroke(1.dp, colors.border),
        colors   = CardDefaults.cardColors(containerColor = Color.Transparent)
    ) {
        Box(
            modifier = Modifier
                .background(cardBrush)
                .fillMaxSize()
                .padding(20.dp)
        ) {
            Column(modifier = Modifier.fillMaxSize()) {

                // ── Header row ────────────────────────────────────────────────
                val hour = java.util.Calendar.getInstance().get(java.util.Calendar.HOUR_OF_DAY)
                val timeGreeting = when (hour) {
                    in 5..11 -> "Buenos días"
                    in 12..18 -> "Buenas tardes"
                    else -> "Buenas noches"
                }

                Row(
                    modifier            = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment   = Alignment.CenterVertically
                ) {
                    Column(
                        modifier = Modifier.pointerInput(Unit) {
                            detectTapGestures(
                                onLongPress = {
                                    editedNameText = currentUserName
                                    showNameDialog = true
                                }
                            )
                        }
                    ) {
                        Text(
                            text       = "Hola, $currentUserName",
                            fontSize   = 28.sp,
                            fontWeight = FontWeight.Bold,
                            color      = colors.textPrimary
                        )
                        Text(
                            text     = "$timeGreeting · Bienvenido",
                            fontSize = 14.sp,
                            color    = colors.textSecondary
                        )
                    }
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Quick Headlights / Night Mode Toggle Button
                        IconButton(
                            onClick = {
                                VehicleIlluminationManager.toggleManual()
                            },
                            modifier = Modifier
                                .size(40.dp)
                                .background(
                                    color = if (isLightsOn) colors.secondary.copy(alpha = 0.35f) else Color(0xFF1E293B).copy(alpha = 0.6f),
                                    shape = CircleShape
                                )
                        ) {
                            Icon(
                                imageVector = Icons.Default.Highlight,
                                contentDescription = if (isLightsOn) "Faros Encendidos" else "Faros Apagados",
                                tint = if (isLightsOn) colors.secondary else colors.textSecondary,
                                modifier = Modifier.size(20.dp)
                            )
                        }

                        // Circular car button — opens asset customizer on TAP, Theme Menu on LONG-PRESS
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .clip(CircleShape)
                                .background(colors.primaryVariant)
                                .combinedClickable(
                                    onClick = {
                                        showAssetModal = true
                                        onCarDetailsClick()
                                    },
                                    onLongClick = {
                                        showThemeModal = true
                                    }
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector        = Icons.Default.DirectionsCar,
                                contentDescription = "Personalizar auto (Mantener presionado para temas)",
                                tint               = if (colors.isDark) Color.White else colors.primary,
                                modifier           = Modifier.size(22.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // ── Car image with door overlays (Tap for voice status, Long-press to customize) ────
                val context = LocalContext.current
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .pointerInput(doorState) {
                            detectTapGestures(
                                onTap = {
                                    val openDoors = mutableListOf<String>()
                                    if (doorState.frontLeft) openDoors.add("puerta del conductor")
                                    if (doorState.frontRight) openDoors.add("puerta del copiloto")
                                    if (doorState.rearLeft) openDoors.add("puerta trasera izquierda")
                                    if (doorState.rearRight) openDoors.add("puerta trasera derecha")
                                    if (doorState.hood) openDoors.add("capó del motor")
                                    if (doorState.trunk) openDoors.add("maletero")

                                    if (openDoors.isEmpty()) {
                                        com.xavier.carlauncher.voice.CarVoiceAssistant.getInstance(context)
                                            .speak("Todas las puertas del vehículo están cerradas.")
                                    } else {
                                        com.xavier.carlauncher.voice.CarVoiceAssistant.getInstance(context)
                                            .speak("Atención: ${openDoors.joinToString(", ")} abierta.")
                                    }
                                },
                                onLongPress = {
                                    showAssetModal = true
                                }
                            )
                        },
                    contentAlignment = Alignment.Center
                ) {
                    val carWidth = Modifier.fillMaxWidth(0.92f)

                    key(assetRefreshKey, isLightsOn) {
                        // Base car — daytime vs lights-on with smooth crossfade
                        CarSlotLayer(
                            slot = if (isLightsOn) CarSlot.LIGHTS_ON else CarSlot.BASE,
                            visible = true,
                            modifier = carWidth,
                            fallbackRes = if (isLightsOn) R.drawable.corolla_lights_on else R.drawable.corolla_base
                        )

                        // Door overlays — animated alpha with default drawables + custom overrides
                        CarSlotLayer(slot = CarSlot.DOOR_FL, visible = doorState.frontLeft,  modifier = carWidth)
                        CarSlotLayer(slot = CarSlot.DOOR_FR, visible = doorState.frontRight, modifier = carWidth)
                        CarSlotLayer(slot = CarSlot.DOOR_RL, visible = doorState.rearLeft,   modifier = carWidth)
                        CarSlotLayer(slot = CarSlot.DOOR_RR, visible = doorState.rearRight,  modifier = carWidth)
                        CarSlotLayer(slot = CarSlot.TRUNK,   visible = doorState.trunk,      modifier = carWidth)
                        CarSlotLayer(slot = CarSlot.HOOD,    visible = doorState.hood,       modifier = carWidth)
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // ── Weather widget ────────────────────────────────────────────
                WeatherWidget(weatherData = weatherData)
            }
        }
    }

    // Modal to customize car assets manually
    if (showAssetModal) {
        CarAssetPickerModal(
            onDismissRequest = { showAssetModal = false },
            onAssetsUpdated = { assetRefreshKey++ }
        )
    }

    // Modal to select themes on car button long-press
    if (showThemeModal) {
        ThemePickerModal(
            onDismiss = { showThemeModal = false }
        )
    }

    // Modal to customize welcome greeting name on long-press
    if (showNameDialog) {
        AlertDialog(
            onDismissRequest = { showNameDialog = false },
            containerColor = Color(0xFF0F172A),
            shape = RoundedCornerShape(20.dp),
            title = {
                Text(
                    text = "Personalizar Nombre",
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "Introduce el nombre para el saludo en pantalla y la voz del auto:",
                        color = ColorGrayMid,
                        fontSize = 13.sp
                    )
                    OutlinedTextField(
                        value = editedNameText,
                        onValueChange = { editedNameText = it },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedBorderColor = Color(0xFF38BDF8),
                            unfocusedBorderColor = Color(0xFF334155),
                            cursorColor = Color(0xFF38BDF8)
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val clean = editedNameText.trim()
                        val toSave = if (clean.isNotBlank()) clean else "Xavier"
                        currentUserName = toSave
                        userPrefs.edit().putString("user_custom_name", toSave).apply()
                        showNameDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0284C7)),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Guardar", color = Color.White, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showNameDialog = false }
                ) {
                    Text("Cancelar", color = ColorGrayMid)
                }
            }
        )
    }
}

// ── Car slot layer with hardware alpha animation ─────────────────────────────

@Composable
private fun CarSlotLayer(
    slot:        CarSlot,
    visible:     Boolean,
    modifier:    Modifier = Modifier,
    fallbackRes: Int = slot.defaultDrawableRes
) {
    val context = LocalContext.current
    val customBitmap = remember(slot) { CarAssetManager.getCustomBitmap(context, slot) }

    val alpha by animateFloatAsState(
        targetValue    = if (visible) 1f else 0f,
        animationSpec  = tween(durationMillis = 350),
        label          = "${slot.name}Alpha"
    )

    if (alpha > 0f) {
        if (customBitmap != null) {
            Image(
                bitmap             = customBitmap,
                contentDescription = slot.title,
                modifier           = modifier.graphicsLayer { this.alpha = alpha }
            )
        } else {
            Image(
                painter            = painterResource(fallbackRes),
                contentDescription = slot.title,
                modifier           = modifier.graphicsLayer { this.alpha = alpha }
            )
        }
    }
}

// ── Weather widget ────────────────────────────────────────────────────────────

@Composable
private fun WeatherWidget(weatherData: WeatherData?) {
    val colors = XavierTheme.colors
    if (weatherData == null) {
        WeatherShimmer()
        return
    }

    val city = weatherData.cityName?.ifBlank { "Yarumela" } ?: "Yarumela"
    val department = weatherData.department?.ifBlank { "" } ?: ""
    val municipality = weatherData.municipality?.ifBlank { "" } ?: ""
    val country = weatherData.country?.ifBlank { "HN" } ?: "HN"
    val desc = weatherData.description?.ifBlank { "Cielo claro" } ?: "Cielo claro"
    val icon = weatherData.iconCode?.ifBlank { "02d" } ?: "02d"

    val displayLocation = buildString {
        append(city)
        if (department.isNotBlank() && !department.equals(city, ignoreCase = true)) {
            append(", ")
            append(department)
        } else if (municipality.isNotBlank() && !municipality.equals(city, ignoreCase = true)) {
            append(", ")
            append(municipality)
        }
        if (country.isNotBlank()) {
            append(", ")
            append(country)
        }
    }

    Column(modifier = Modifier.fillMaxWidth()) {
        // Location row: Pin + City Name + Department + Country + Update interval
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Icon(
                imageVector = Icons.Default.LocationOn,
                contentDescription = "Ubicación",
                tint = ColorAccentGreen,
                modifier = Modifier.size(15.dp)
            )
            Text(
                text = displayLocation,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = ColorAccentGreen
            )
            Text(
                text = "• Tiempo real (cada 10 min)",
                fontSize = 10.sp,
                color = ColorGrayMid
            )
        }

        Spacer(modifier = Modifier.height(4.dp))

        // Main temp row
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Weather emoji / icon mapped from iconCode
            Text(
                text     = mapIconCodeToEmoji(icon),
                fontSize = 38.sp
            )
            Column {
                Row(verticalAlignment = Alignment.Bottom) {
                    Text(
                        text       = "${weatherData.temp}°",
                        fontSize   = 44.sp,
                        fontWeight = FontWeight.Bold,
                        color      = colors.textPrimary,
                        lineHeight = 46.sp
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text     = "Sensación ${weatherData.feelsLike}°",
                        fontSize = 12.sp,
                        color    = colors.textSecondary,
                        modifier = Modifier.padding(bottom = 6.dp)
                    )
                }
                Text(
                    text     = desc,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium,
                    color    = Color(0xFF38BDF8)
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Bottom pill row: max, min, humidity, wind, pressure, clouds
        Row(
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            WeatherPill(label = "↑ ${weatherData.tempMax}°")
            WeatherPill(label = "↓ ${weatherData.tempMin}°")
            WeatherPill(label = "💧 ${weatherData.humidity}%")
            WeatherPill(label = "💨 ${String.format(java.util.Locale.US, "%.1f", weatherData.windSpeedMph)} mph")
            WeatherPill(label = "⏲ ${weatherData.pressureHpa} hPa")
            WeatherPill(label = "☁ ${weatherData.cloudsPercent}%")
        }
    }
}

@Composable
private fun WeatherPill(label: String) {
    val colors = XavierTheme.colors
    Box(
        modifier = Modifier
            .background(
                color  = colors.border,
                shape  = RoundedCornerShape(50)
            )
            .padding(horizontal = 10.dp, vertical = 4.dp)
    ) {
        Text(text = label, fontSize = 12.sp, color = colors.textPrimary)
    }
}

@Composable
private fun WeatherShimmer() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(80.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(
                brush = Brush.horizontalGradient(
                    colors = listOf(
                        Color(0xFF1E3A8A).copy(alpha = 0.4f),
                        Color(0xFF1E3A8A).copy(alpha = 0.2f),
                        Color(0xFF1E3A8A).copy(alpha = 0.4f)
                    )
                )
            )
    )
}

// ── Helpers ───────────────────────────────────────────────────────────────────

private fun mapIconCodeToEmoji(iconCode: String): String = when {
    iconCode.startsWith("01") -> "☀️"
    iconCode.startsWith("02") -> "🌤️"
    iconCode.startsWith("03") -> "☁️"
    iconCode.startsWith("04") -> "☁️"
    iconCode.startsWith("09") -> "🌧️"
    iconCode.startsWith("10") -> "🌦️"
    iconCode.startsWith("11") -> "⛈️"
    iconCode.startsWith("13") -> "❄️"
    iconCode.startsWith("50") -> "🌫️"
    else                      -> "🌡️"
}
