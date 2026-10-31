package com.xavier.carlauncher.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.xavier.carlauncher.ui.theme.ColorBorder
import com.xavier.carlauncher.ui.theme.ColorGrayMid
import com.xavier.carlauncher.ui.theme.ColorNeonBlue
import com.xavier.carlauncher.ui.theme.ColorSurface
import com.xavier.carlauncher.ui.theme.ColorSurfaceDeep
import kotlin.math.cos
import kotlin.math.sin

private val speedometerCardBrush = Brush.verticalGradient(
    colors = listOf(ColorSurface, ColorSurfaceDeep)
)

/**
 * Interactive Speedometer card with Canvas arc gauge.
 * Tapping the card toggles between MPH and KM/H dynamically.
 *
 * @param speedMph  Current vehicle speed in mph (GPS calibrated).
 */
@Composable
fun SpeedometerCard(
    speedMph: Float,
    modifier: Modifier = Modifier
) {
    var isKmh by remember { mutableStateOf(false) }

    val currentSpeed = if (isKmh) speedMph * 1.60934f else speedMph
    val maxGaugeSpeed = if (isKmh) 200f else 120f
    val unitLabel = if (isKmh) "km/h" else "mph"

    Card(
        modifier = modifier.clickable { isKmh = !isKmh },
        shape    = RoundedCornerShape(24.dp),
        border   = BorderStroke(1.dp, ColorBorder),
        colors   = CardDefaults.cardColors(containerColor = Color.Transparent)
    ) {
        Box(
            modifier = Modifier
                .background(speedometerCardBrush)
                .fillMaxSize()
                .padding(16.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier            = Modifier.fillMaxSize()
            ) {
                // Animated speed value for smooth arc sweep
                val animatedSpeed by animateFloatAsState(
                    targetValue   = currentSpeed.coerceIn(0f, maxGaugeSpeed),
                    animationSpec = tween(durationMillis = 300),
                    label         = "speedAnim"
                )

                Box(
                    modifier         = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    SpeedometerCanvas(
                        animatedSpeed = animatedSpeed,
                        currentSpeed  = currentSpeed,
                        maxSpeed      = maxGaugeSpeed,
                        unit          = unitLabel
                    )
                }

                Text(
                    text     = "Toca para cambiar unidad ($unitLabel)",
                    fontSize = 11.sp,
                    color    = ColorGrayMid
                )
            }
        }
    }
}

@Composable
private fun SpeedometerCanvas(
    animatedSpeed: Float,
    currentSpeed:  Float,
    maxSpeed:      Float,
    unit:          String
) {
    Box(contentAlignment = Alignment.Center) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val strokeWidth  = 12.dp.toPx()
            val arcPadding   = strokeWidth / 2f + 8.dp.toPx()
            val diameter     = minOf(size.width, size.height) - arcPadding * 2f
            val topLeft      = Offset(
                x = (size.width  - diameter) / 2f,
                y = (size.height - diameter) / 2f + 12.dp.toPx()
            )
            val arcSize      = Size(diameter, diameter)
            val startAngle   = 150f
            val totalSweep   = 240f

            // Background arc (dark gray track)
            drawArc(
                color      = Color(0xFF2D3748),
                startAngle = startAngle,
                sweepAngle = totalSweep,
                useCenter  = false,
                topLeft    = topLeft,
                size       = arcSize,
                style      = Stroke(width = strokeWidth, cap = StrokeCap.Round)
            )

            // Progress arc (neon blue)
            val progressSweep = (animatedSpeed / maxSpeed) * totalSweep
            if (progressSweep > 0f) {
                drawArc(
                    color      = ColorNeonBlue,
                    startAngle = startAngle,
                    sweepAngle = progressSweep,
                    useCenter  = false,
                    topLeft    = topLeft,
                    size       = arcSize,
                    style      = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                )
            }

            // Tick marks
            val centerX   = size.width  / 2f
            val centerY   = size.height / 2f + 12.dp.toPx()
            val radius    = diameter / 2f
            val outerR    = radius + strokeWidth / 2f + 4.dp.toPx()
            val innerR    = radius - strokeWidth / 2f - 4.dp.toPx()
            val tickColor = Color(0xFF4B5563)
            val tickCount = 7

            for (i in 0 until tickCount) {
                val fraction   = i / (tickCount - 1).toFloat()
                val angleDeg   = startAngle + fraction * totalSweep
                val angleRad   = Math.toRadians(angleDeg.toDouble())
                val cosA       = cos(angleRad).toFloat()
                val sinA       = sin(angleRad).toFloat()
                drawLine(
                    color       = tickColor,
                    start       = Offset(centerX + outerR * cosA, centerY + outerR * sinA),
                    end         = Offset(centerX + innerR * cosA, centerY + innerR * sinA),
                    strokeWidth = 2.dp.toPx()
                )
            }
        }

        // Center text overlay
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text       = currentSpeed.toInt().toString(),
                fontSize   = 46.sp,
                fontWeight = FontWeight.Bold,
                color      = Color.White
            )
            Text(
                text     = unit,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                color    = Color(0xFF38BDF8)
            )
        }
    }
}
