package com.altfelfm.radio.audio

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun CircularAudioVisualizer(
    isPlaying: Boolean,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "circular_visualizer")

    val pulseFactor by infiniteTransition.animateFloat(
        initialValue = 0.4f,
        targetValue = if (isPlaying) 1.0f else 0.4f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 500, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ), label = "pulse"
    )

    Canvas(modifier = modifier.fillMaxSize()) {
        val center = Offset(size.width / 2, size.height / 2)
        val radius = (size.width.coerceAtMost(size.height) / 2) - 20.dp.toPx()
        val barCount = 60

        for (i in 0 until barCount) {
            val angle = Math.toRadians((i * (360.0 / barCount)))
            val barLength = (15.dp.toPx() + (i % 5 * 6).dp.toPx()) * if (isPlaying) pulseFactor else 0.2f

            val startX = center.x + (radius * cos(angle)).toFloat()
            val startY = center.y + (radius * sin(angle)).toFloat()
            val endX = center.x + ((radius + barLength) * cos(angle)).toFloat()
            val endY = center.y + ((radius + barLength) * sin(angle)).toFloat()

            drawLine(
                color = Color(0xFFFF0055).copy(alpha = if (isPlaying) 0.85f else 0.3f),
                start = Offset(startX, startY),
                end = Offset(endX, endY),
                strokeWidth = 3.dp.toPx(),
                cap = StrokeCap.Round
            )
        }
    }
}
