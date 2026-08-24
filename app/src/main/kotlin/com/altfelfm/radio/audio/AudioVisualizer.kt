package com.altfelfm.radio.audio

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.unit.dp
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

@Composable
fun CircularAudioVisualizer(
    isPlaying: Boolean,
    songTitle: String,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "visualizer_anim")

    val pulse by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = if (isPlaying) 1.0f else 0.2f,
        animationSpec = infiniteRepeatable(
            animation = tween(400, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ), label = "pulse"
    )

    // Generăm un pattern nou la fiecare piesă nouă
    val seed = remember(songTitle) { Random(songTitle.hashCode()).nextInt(100) }

    Canvas(modifier = modifier.fillMaxSize()) {
        val center = Offset(size.width / 2, size.height / 2)
        val radius = (size.width.coerceAtMost(size.height) / 2) - 25.dp.toPx()
        val barCount = 72

        for (i in 0 until barCount) {
            val angle = Math.toRadians((i * (360.0 / barCount)))
            val dynamicFactor = if (isPlaying) {
                ((i + seed) % 7 + 1) * 4.dp.toPx() * pulse
            } else {
                2.dp.toPx()
            }

            val startX = center.x + (radius * cos(angle)).toFloat()
            val startY = center.y + (radius * sin(angle)).toFloat()
            val endX = center.x + ((radius + dynamicFactor) * cos(angle)).toFloat()
            val endY = center.y + ((radius + dynamicFactor) * sin(angle)).toFloat()

            val barColor = if (isPlaying) {
                Color(0xFFFF0055).copy(alpha = (0.5f + (i % 5) * 0.1f).coerceAtMost(1f))
            } else {
                Color.White.copy(alpha = 0.2f)
            }

            drawLine(
                color = barColor,
                start = Offset(startX, startY),
                end = Offset(endX, endY),
                strokeWidth = 3.dp.toPx(),
                cap = StrokeCap.Round
            )
        }
    }
}
