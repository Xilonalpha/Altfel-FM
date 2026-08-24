package com.altfelfm.radio.audio

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color

@Composable
fun AudioVisualizer(
    isPlaying: Boolean,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "visualizer")
    
    val bar1Height by infiniteTransition.animateFloat(
        initialValue = 0.2f,
        targetValue = if (isPlaying) 0.9f else 0.2f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 400, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ), label = "b1"
    )

    val bar2Height by infiniteTransition.animateFloat(
        initialValue = 0.5f,
        targetValue = if (isPlaying) 1.0f else 0.2f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 300, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ), label = "b2"
    )

    val bar3Height by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = if (isPlaying) 0.8f else 0.2f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 500, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ), label = "b3"
    )

    Canvas(modifier = modifier.fillMaxSize()) {
        val width = size.width
        val height = size.height
        val barWidth = width / 7
        val bars = listOf(bar1Height, bar2Height, bar3Height, bar1Height, bar2Height)

        bars.forEachIndexed { index, animatedFactor ->
            val x = (index * 1.4f + 0.5f) * barWidth
            val currentBarHeight = height * animatedFactor
            val startY = height - currentBarHeight

            drawLine(
                color = Color(0xFFFF0055),
                start = Offset(x, height),
                end = Offset(x, startY),
                strokeWidth = barWidth * 0.6f
            )
        }
    }
}
