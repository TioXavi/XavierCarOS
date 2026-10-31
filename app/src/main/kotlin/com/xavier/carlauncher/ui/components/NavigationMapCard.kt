package com.xavier.carlauncher.ui.components

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.net.Uri
import android.os.Environment
import android.util.Log
import android.view.MotionEvent
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.xavier.carlauncher.location.VehicleLocationManager
import com.xavier.carlauncher.navigation.MapDownloadWorker
import com.xavier.carlauncher.navigation.NavigationGuidance
import com.xavier.carlauncher.navigation.NavigationManager
import com.xavier.carlauncher.ui.theme.ColorBorder
import com.xavier.carlauncher.ui.theme.ColorGrayDark
import com.xavier.carlauncher.ui.theme.ColorGrayMid
import com.xavier.carlauncher.ui.theme.ColorSurfaceDeep
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.modules.ArchiveFileFactory
import org.osmdroid.tileprovider.modules.OfflineTileProvider
import org.osmdroid.tileprovider.tilesource.FileBasedTileSource
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.tileprovider.util.SimpleRegisterReceiver
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker
import java.io.File

/**
 * Automotive Navigation Card displaying Maps and Waze app launchers
 * over a stylized dark cybernetic vector road map background.
 */
@Composable
fun NavigationMapCard(
    modifier: Modifier = Modifier,
    onMapClick: () -> Unit = {}
) {
    val context = LocalContext.current
    val guidance by NavigationManager.guidance.collectAsState()
    val vehicleLoc by VehicleLocationManager.locationState.collectAsState()
    var recenterCounter by remember { mutableStateOf(0) }

    fun launchWaze(favorite: String? = null) {
        try {
            val uri = when (favorite) {
                "home" -> Uri.parse("waze://?favorite=home&navigate=yes")
                "work" -> Uri.parse("waze://?favorite=work&navigate=yes")
                else -> Uri.parse("waze://?navigate=yes")
            }
            val wazeIntent = Intent(Intent.ACTION_VIEW, uri).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(wazeIntent)
        } catch (_: Exception) {
            val pkgIntent = context.packageManager.getLaunchIntentForPackage("com.waze")
            if (pkgIntent != null) {
                pkgIntent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
                context.startActivity(pkgIntent)
            }
        }
    }

    fun launchGoogleMaps(favorite: String? = null) {
        try {
            val uri = when (favorite) {
                "home" -> Uri.parse("google.navigation:q=Home")
                "work" -> Uri.parse("google.navigation:q=Work")
                else -> Uri.parse("geo:0,0?q=")
            }
            val mapsIntent = Intent(Intent.ACTION_VIEW, uri).apply {
                setPackage("com.google.android.apps.maps")
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(mapsIntent)
        } catch (_: Exception) {
            val fallback = Intent(Intent.ACTION_VIEW, Uri.parse("geo:0,0?q=")).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            try { context.startActivity(fallback) } catch (_: Exception) {}
        }
    }

    fun launchOrganicMaps() {
        val pm = context.packageManager
        val intent = pm.getLaunchIntentForPackage("app.organicmaps.web")
            ?: pm.getLaunchIntentForPackage("app.organicmaps")
        if (intent != null) {
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
            try { context.startActivity(intent) } catch (_: Exception) {}
        }
    }

    Card(
        modifier = modifier,
        shape = RoundedCornerShape(24.dp),
        border = BorderStroke(1.dp, Color(0xFF38BDF8).copy(alpha = 0.45f)),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent)
    ) {
        Box(modifier = Modifier.fillMaxSize()) {

            // ── 1. Full-bleed Live Real-Time Osmdroid GPS MapView ──────────
            RealTimeOsmdroidMapView(
                modifier = Modifier.fillMaxSize(),
                recenterTrigger = recenterCounter
            )

            // ── 2. Top & Bottom Subtle Scrims (keeps map clear in the center) ───
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                Color(0xDD070D1E),
                                Color(0x00070D1E)
                            )
                        )
                    )
                    .align(Alignment.TopCenter)
            )

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(60.dp)
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                Color(0x00070D1E),
                                Color(0xEE070D1E)
                            )
                        )
                    )
                    .align(Alignment.BottomCenter)
            )

            // ── 3. Floating Overlay Elements ──────────────────────────────────
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 10.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                // Top Bar: Card Title, Navigation HUD, GPS Telemetry & Recenter Button
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Title Pill
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color(0xFF0F172A).copy(alpha = 0.85f))
                            .padding(horizontal = 9.dp, vertical = 5.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .background(Color(0xFF38BDF8), CircleShape)
                        )
                        Text(
                            text = "Navegación GPS",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }

                    // Turn-by-Turn Maneuver Pill (if navigating)
                    if (guidance.isNavigating) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            modifier = Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .background(Color(0xFF0284C7).copy(alpha = 0.88f))
                                .clickable { launchWaze() }
                                .padding(horizontal = 10.dp, vertical = 5.dp)
                        ) {
                            Icon(
                                imageVector = when {
                                    guidance.instruction.contains("izq", ignoreCase = true) -> Icons.Default.TurnLeft
                                    guidance.instruction.contains("der", ignoreCase = true) -> Icons.Default.TurnRight
                                    else -> Icons.Default.Straight
                                },
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(15.dp)
                            )
                            Text(
                                text = "${guidance.distance} · ${guidance.street.ifBlank { guidance.instruction }}",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }

                    // Right Actions: GPS Status + Recenter Icon
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        // GPS Status Pill
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(5.dp),
                            modifier = Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .background(Color(0xFF0F172A).copy(alpha = 0.85f))
                                .padding(horizontal = 8.dp, vertical = 5.dp)
                        ) {
                            val dotColor = when {
                                guidance.isNavigating -> Color(0xFF38BDF8)
                                vehicleLoc.hasFix -> Color(0xFF10B981)
                                else -> Color(0xFFFBBF24)
                            }
                            val statusText = when {
                                guidance.isNavigating -> "En Ruta"
                                vehicleLoc.hasFix && vehicleLoc.satellites > 0 -> "GPS (${vehicleLoc.satellites} Sats)"
                                vehicleLoc.hasFix -> "GPS Activo"
                                else -> "Buscando GPS..."
                            }
                            Box(
                                modifier = Modifier
                                    .size(7.dp)
                                    .background(dotColor, CircleShape)
                            )
                            Text(
                                text = statusText,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = dotColor
                            )
                        }

                        // Recenter GPS Button
                        Box(
                            modifier = Modifier
                                .size(28.dp)
                                .clip(CircleShape)
                                .background(Color(0xFF0F172A).copy(alpha = 0.85f))
                                .clickable { recenterCounter++ },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.MyLocation,
                                contentDescription = "Recentrar GPS",
                                tint = Color(0xFF38BDF8),
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }

                // Middle area is fully clear to reveal the live real-time vehicle road map
                Spacer(modifier = Modifier.weight(1f))

                // Bottom Floating Navigation Dock: Maps, Waze, Organic Maps, Casa, Trabajo
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(Color(0xFF090E1D).copy(alpha = 0.88f))
                        .padding(horizontal = 6.dp, vertical = 5.dp),
                    horizontalArrangement = Arrangement.spacedBy(5.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Google Maps
                    FloatingDockNavButton(
                        label = "Maps",
                        icon = Icons.Default.Place,
                        iconColor = Color(0xFF34A853),
                        onClick = { launchGoogleMaps() },
                        modifier = Modifier.weight(1f)
                    )

                    // Waze GPS
                    FloatingDockNavButton(
                        label = "Waze",
                        icon = Icons.Default.Navigation,
                        iconColor = Color(0xFF00E5FF),
                        onClick = { launchWaze() },
                        modifier = Modifier.weight(1f)
                    )

                    // Organic Maps (Offline GPS)
                    FloatingDockNavButton(
                        label = "Organic",
                        icon = Icons.Default.Map,
                        iconColor = Color(0xFF10B981),
                        onClick = { launchOrganicMaps() },
                        modifier = Modifier.weight(1f)
                    )

                    // Casa Quick Nav
                    FloatingDockNavButton(
                        label = "Casa",
                        icon = Icons.Default.Home,
                        iconColor = Color(0xFF38BDF8),
                        onClick = { launchWaze("home") },
                        modifier = Modifier.weight(1f)
                    )

                    // Trabajo Quick Nav
                    FloatingDockNavButton(
                        label = "Trabajo",
                        icon = Icons.Default.Work,
                        iconColor = Color(0xFFA855F7),
                        onClick = { launchWaze("work") },
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }
}

/**
 * High-tech glassmorphic pill button for floating navigation dock.
 */
@Composable
private fun FloatingDockNavButton(
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    iconColor: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(10.dp),
        color = Color(0xFF1E293B).copy(alpha = 0.65f),
        border = BorderStroke(1.dp, iconColor.copy(alpha = 0.35f)),
        modifier = modifier.height(34.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxSize().padding(horizontal = 3.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = iconColor,
                modifier = Modifier.size(14.dp)
            )
            Spacer(modifier = Modifier.width(3.dp))
            Text(
                text = label,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

/**
 * High-tech glassmorphic tile for Maps & Waze app launchers.
 */
@Composable
private fun NavAppLauncherTile(
    title: String,
    subtitle: String,
    iconColor: Color,
    badgeColor: Color,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(16.dp),
        color = Color(0xFF0F172A).copy(alpha = 0.65f),
        border = BorderStroke(1.dp, iconColor.copy(alpha = 0.35f)),
        modifier = modifier.fillMaxHeight()
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 10.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .background(
                        Brush.radialGradient(
                            listOf(iconColor.copy(alpha = 0.35f), badgeColor.copy(alpha = 0.45f))
                        ),
                        CircleShape
                    )
                    .clip(CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = title,
                    tint = iconColor,
                    modifier = Modifier.size(24.dp)
                )
            }

            Column(
                verticalArrangement = Arrangement.Center,
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = title,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    maxLines = 1
                )
                Text(
                    text = subtitle,
                    fontSize = 10.sp,
                    color = ColorGrayMid,
                    maxLines = 1
                )
            }
        }
    }
}

@Composable
private fun QuickDestinationPill(
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(10.dp),
        color = Color.White.copy(alpha = 0.08f),
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.12f)),
        modifier = modifier
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = Color(0xFF38BDF8),
                modifier = Modifier.size(14.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = label,
                fontSize = 11.sp,
                color = Color.White,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

/**
 * High-performance Compose Canvas rendering a dark cybernetic automotive GPS road map.
 */
@Composable
private fun AutomotiveMapBackground(modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height

        // 1. Road Arteries (Gray-blue highway grid)
        val roadPaint = Color(0xFF1E293B)
        val roadSecondary = Color(0xFF152238)

        // Horizontal avenues
        drawLine(roadPaint, Offset(0f, h * 0.28f), Offset(w, h * 0.28f), strokeWidth = 10f)
        drawLine(roadPaint, Offset(0f, h * 0.72f), Offset(w, h * 0.72f), strokeWidth = 8f)

        // Vertical avenues
        drawLine(roadPaint, Offset(w * 0.35f, 0f), Offset(w * 0.35f, h), strokeWidth = 9f)
        drawLine(roadPaint, Offset(w * 0.82f, 0f), Offset(w * 0.82f, h), strokeWidth = 7f)

        // Diagonal Highway
        val highwayPath = Path().apply {
            moveTo(0f, h * 0.85f)
            cubicTo(w * 0.3f, h * 0.7f, w * 0.6f, h * 0.4f, w, h * 0.15f)
        }
        drawPath(highwayPath, roadSecondary, style = Stroke(width = 16f, cap = StrokeCap.Round))

        // 2. Active Glowing Route Line (Neon Cyan)
        val routePath = Path().apply {
            moveTo(w * 0.35f, h * 0.85f)
            lineTo(w * 0.35f, h * 0.48f)
            cubicTo(w * 0.36f, h * 0.32f, w * 0.55f, h * 0.28f, w * 0.78f, h * 0.28f)
            lineTo(w * 0.88f, h * 0.12f)
        }
        // Outer cyan glow
        drawPath(routePath, Color(0xFF00E5FF).copy(alpha = 0.28f), style = Stroke(width = 10f, cap = StrokeCap.Round))
        // Inner core
        drawPath(routePath, Color(0xFF38BDF8), style = Stroke(width = 4f, cap = StrokeCap.Round))

        // 3. Vehicle GPS Position Dot + Pulse Rings
        val carPos = Offset(w * 0.35f, h * 0.48f)
        drawCircle(Color(0xFF38BDF8).copy(alpha = 0.20f), radius = 22f, center = carPos)
        drawCircle(Color(0xFF38BDF8).copy(alpha = 0.45f), radius = 13f, center = carPos)
        drawCircle(Color(0xFFFFFFFF), radius = 5f, center = carPos)

        // 4. Subtle grid lines / coordinates ticks
        val gridColor = Color(0xFF334155).copy(alpha = 0.25f)
        for (i in 1..4) {
            drawLine(gridColor, Offset(w * (i * 0.2f), 0f), Offset(w * (i * 0.2f), h), strokeWidth = 1f)
            drawLine(gridColor, Offset(0f, h * (i * 0.25f)), Offset(w, h * (i * 0.25f)), strokeWidth = 1f)
        }
    }
}

/**
 * Creates a modern automotive vehicle position circle.
 * High-contrast cyan/sky blue core with white border ring and glowing aura.
 */
private fun createVehicleCircleDrawable(context: Context): Drawable {
    val size = 44
    val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
    val canvas = android.graphics.Canvas(bitmap)

    // Outer glow / halo
    val haloPaint = android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG).apply {
        color = android.graphics.Color.argb(55, 0, 229, 255) // Cyan halo
        style = android.graphics.Paint.Style.FILL
    }
    canvas.drawCircle(size / 2f, size / 2f, 20f, haloPaint)

    // Subtle dark drop shadow
    val shadowPaint = android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG).apply {
        color = android.graphics.Color.argb(120, 0, 0, 0)
        style = android.graphics.Paint.Style.FILL
    }
    canvas.drawCircle(size / 2f, size / 2f + 1.5f, 13f, shadowPaint)

    // Crisp white accent ring
    val ringPaint = android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG).apply {
        color = android.graphics.Color.WHITE
        style = android.graphics.Paint.Style.STROKE
        strokeWidth = 3f
    }
    canvas.drawCircle(size / 2f, size / 2f, 12f, ringPaint)

    // Vibrant Electric Cyan core
    val corePaint = android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG).apply {
        color = android.graphics.Color.parseColor("#00E5FF")
        style = android.graphics.Paint.Style.FILL
    }
    canvas.drawCircle(size / 2f, size / 2f, 10.5f, corePaint)

    // Center pinpoint light
    val centerDot = android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG).apply {
        color = android.graphics.Color.WHITE
        style = android.graphics.Paint.Style.FILL
    }
    canvas.drawCircle(size / 2f, size / 2f, 4.5f, centerDot)

    return BitmapDrawable(context.resources, bitmap)
}

/**
 * Real-Time Osmdroid MapView Widget.
 * - Hardware accelerated rendering
 * - High-contrast automotive vehicle circle position indicator
 * - Course-Up / Heading-Up orientation: map rotates smoothly so driving direction is always UP
 * - Instant frame 0 display of vehicle's true location (Yarumela, La Paz), eliminating Tegucigalpa delay
 * - Zero duplicated GNSS listeners to eliminate Spreadtrum HAL socket contention
 * - Offline tile loader from /sdcard/osmdroid/honduras.sqlite via OfflineTileProvider & ArchiveFileFactory
 * - High-contrast automotive cybernetic night mode palette
 */
@Composable
fun RealTimeOsmdroidMapView(
    modifier: Modifier = Modifier,
    recenterTrigger: Int = 0
) {
    val context = LocalContext.current
    val mapUpdateEvent by MapDownloadWorker.mapUpdateEvent.collectAsState()
    val vehicleLoc by VehicleLocationManager.locationState.collectAsState()

    var isAutoFollow by remember { mutableStateOf(true) }
    var carMarkerRef by remember { mutableStateOf<Marker?>(null) }
    var mapRef by remember { mutableStateOf<MapView?>(null) }

    val vehicleIcon = remember(context) { createVehicleCircleDrawable(context) }

    val mapView = remember {
        Configuration.getInstance().userAgentValue = context.packageName
        Configuration.getInstance().osmdroidBasePath = MapDownloadWorker.getOsmdroidDir()
        Configuration.getInstance().osmdroidTileCache = File(MapDownloadWorker.getOsmdroidDir(), "tiles")

        MapView(context).apply {
            isTilesScaledToDpi = true
            setBuiltInZoomControls(false)
            setMultiTouchControls(true)
            setUseDataConnection(com.xavier.carlauncher.system.NetworkUtils.isOnline(context))
            controller.setZoom(14.8)
            controller.setCenter(GeoPoint(vehicleLoc.latitude, vehicleLoc.longitude))

            // Automotive Night / Cybernetic Palette Matrix
            val nightMatrix = floatArrayOf(
                -0.85f, 0f, 0f, 0f, 215f,
                0f, -0.85f, 0f, 0f, 225f,
                0f, 0f, -0.85f, 0f, 255f,
                0f, 0f, 0f, 1f, 0f
            )
            overlayManager.tilesOverlay.setColorFilter(android.graphics.ColorMatrixColorFilter(nightMatrix))

            // Dedicated Vehicle Circle Marker
            val marker = Marker(this).apply {
                position = GeoPoint(vehicleLoc.latitude, vehicleLoc.longitude)
                icon = vehicleIcon
                setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_CENTER)
                rotation = 0f
                setInfoWindow(null)
                isDraggable = false
            }
            overlays.add(marker)
            carMarkerRef = marker
            mapRef = this

            setOnTouchListener { _, event ->
                if (event.action == MotionEvent.ACTION_MOVE) {
                    isAutoFollow = false
                }
                false
            }
        }
    }

    // Keep vehicle marker and map position in sync with unified vehicle location
    LaunchedEffect(vehicleLoc.latitude, vehicleLoc.longitude, vehicleLoc.bearing) {
        val newGeoPoint = GeoPoint(vehicleLoc.latitude, vehicleLoc.longitude)
        carMarkerRef?.apply {
            position = newGeoPoint
            rotation = 0f
        }
        if (isAutoFollow) {
            mapRef?.controller?.animateTo(newGeoPoint)
            if (vehicleLoc.bearing != 0f) {
                val targetOrientation = (360f - vehicleLoc.bearing) % 360f
                mapRef?.setMapOrientation(targetOrientation, false)
            }
        }
        mapRef?.invalidate()
    }

    // React to user tapping the Recenter button
    LaunchedEffect(recenterTrigger) {
        if (recenterTrigger > 0) {
            isAutoFollow = true
            val targetGeo = GeoPoint(vehicleLoc.latitude, vehicleLoc.longitude)
            mapRef?.controller?.animateTo(targetGeo)
            mapRef?.controller?.setZoom(15.0)
            if (vehicleLoc.bearing != 0f) {
                val targetOrientation = (360f - vehicleLoc.bearing) % 360f
                mapRef?.setMapOrientation(targetOrientation, false)
            }
            mapRef?.invalidate()
        }
    }

    // Refresh tile source when honduras.sqlite is downloaded or updated
    LaunchedEffect(mapUpdateEvent) {
        val mapFile = MapDownloadWorker.getTargetFile()
        if (mapFile.exists() && mapFile.length() > 0) {
            try {
                val tileProvider = OfflineTileProvider(SimpleRegisterReceiver(context), arrayOf(mapFile))
                mapView.tileProvider = tileProvider
                val sources = tileProvider.archives.firstOrNull()?.tileSources
                if (!sources.isNullOrEmpty()) {
                    mapView.setTileSource(FileBasedTileSource.getSource(sources.first()))
                }
                Log.i("NavigationMapCard", "Offline Honduras map tiles loaded from ${mapFile.name}")
            } catch (e: Exception) {
                Log.w("NavigationMapCard", "Cannot load offline map tiles, using fallback", e)
                mapView.setTileSource(TileSourceFactory.MAPNIK)
            }
        } else {
            mapView.setTileSource(TileSourceFactory.MAPNIK)
        }
    }

    DisposableEffect(mapView) {
        mapView.onResume()
        onDispose {
            mapView.onPause()
        }
    }

    AndroidView(
        factory = { mapView },
        modifier = modifier
    )
}
