package com.altfelfm.radio.audio

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import kotlinx.coroutines.flow.StateFlow
import kotlin.math.*

private const val BAR_COUNT = 64

/**
 * Vizualizator audio REAL. Sursa datelor este [magnitudesFlow], populat de RadioPlayer
 * din android.media.audiofx.Visualizer legat de audioSessionId-ul real al ExoPlayer-ului.
 * Nu se folosește Random / date inventate în nicio ramură a acestei funcții: cât timp
 * nu există date reale (buffering, permisiune lipsă, stream oprit) barele rămân aproape
 * plate, în loc să simuleze o mișcare falsă.
 */
@Composable
fun AudioVisualizer(
    modifier: Modifier = Modifier,
    isPlaying: Boolean,
    magnitudesFlow: StateFlow<FloatArray?>
) {
    var bars by remember { mutableStateOf(FloatArray(BAR_COUNT) { 0f }) }
    var rotation by remember { mutableStateOf(0f) }
    var pulseValue by remember { mutableStateOf(0f) }
    var runningMax by remember { mutableStateOf(1f) }

    val latestMagnitudes by magnitudesFlow.collectAsState()

    LaunchedEffect(isPlaying) {
        if (isPlaying) {
            while (true) {
                val mags = latestMagnitudes
                if (mags != null && mags.isNotEmpty()) {
                    // Grupăm benzile FFT reale în BAR_COUNT bare, pe o curbă logaritmică
                    // (ca la orice egalizor: mai multă rezoluție vizuală pe frecvențe joase).
                    val targets = mapMagnitudesToBars(mags, BAR_COUNT)

                    // Auto-gain: normalizăm după un maxim recent (cu decădere lentă), pentru că
                    // volumul streamului radio variază; fără asta barele ar fi mereu la maxim
                    // sau mereu invizibile în funcție de nivelul brut al semnalului.
                    val frameMax = targets.maxOrNull() ?: 0f
                    runningMax = maxOf(frameMax, runningMax * 0.98f, 1f)

                    val newBars = FloatArray(BAR_COUNT)
                    for (i in targets.indices) {
                        val targetNorm = (targets[i] / runningMax).coerceIn(0f, 1f)
                        newBars[i] = bars[i] + (targetNorm - bars[i]) * 0.35f
                    }
                    bars = newBars

                    val avgLevel = (targets.average().toFloat() / runningMax).coerceIn(0f, 1f)
                    pulseValue = pulseValue + (avgLevel - pulseValue) * 0.2f
                    rotation = (rotation + 1.2f + avgLevel * 3f) % 360f
                } else {
                    // Fără date reale încă (buffering/pornire) - relaxăm spre aproape-plat,
                    // NU generăm o animație de umplutură.
                    val newBars = FloatArray(BAR_COUNT)
                    for (i in bars.indices) newBars[i] = bars[i] + (0.03f - bars[i]) * 0.1f
                    bars = newBars
                    pulseValue *= 0.95f
                    rotation *= 0.98f
                }
                kotlinx.coroutines.delay(16) // ~60 FPS pe interpolarea de randare; datele-sursă sunt reale
            }
        } else {
            bars = FloatArray(BAR_COUNT) { 0f }
            rotation = 0f
            pulseValue = 0f
            runningMax = 1f
        }
    }

    Canvas(modifier = modifier) {
        val centerX = size.width / 2
        val centerY = size.height / 2
        val maxRadius = minOf(size.width, size.height) / 2 - 20

        drawIntoCanvas { _ ->
            drawBackgroundEffect(centerX, centerY, maxRadius, pulseValue)
            drawCircularWaveform(centerX, centerY, maxRadius, bars, rotation, pulseValue)
            drawRadialBars(centerX, centerY, maxRadius * 0.7f, bars, rotation, pulseValue)
            drawNucleus(centerX, centerY, pulseValue)
        }
    }
}

/**
 * Mapează cele ~N/2 benzi de frecvență FFT reale pe BAR_COUNT bare vizuale, folosind o
 * curbă pătratică (mai multe bare "dedicate" frecvențelor joase, unde e de obicei energia
 * dominantă în muzică/voce - la fel ca un egalizor grafic clasic).
 */
private fun mapMagnitudesToBars(mags: FloatArray, barCount: Int): FloatArray {
    val binCount = mags.size
    val bars = FloatArray(barCount)
    for (i in 0 until barCount) {
        val t0 = i.toFloat() / barCount
        val t1 = (i + 1).toFloat() / barCount
        val start = (t0.pow(2f) * (binCount - 1)).toInt().coerceIn(0, binCount - 1)
        val end = (t1.pow(2f) * (binCount - 1)).toInt().coerceIn(start + 1, binCount)
        var sum = 0f
        for (b in start until end) sum += mags[b]
        bars[i] = sum / (end - start)
    }
    return bars
}

private fun DrawScope.drawBackgroundEffect(
    centerX: Float,
    centerY: Float,
    maxRadius: Float,
    pulseValue: Float
) {
    val pulseRadius = maxRadius * (0.3f + pulseValue * 0.1f)

    repeat(3) { index ->
        val alpha = (1f - (index / 3f)) * 0.15f
        val radius = pulseRadius * (1f + index * 0.3f)

        drawCircle(
            color = Color(0xFF6200EE).copy(alpha = alpha),
            center = androidx.compose.ui.geometry.Offset(centerX, centerY),
            radius = radius
        )
    }
}

private fun DrawScope.drawCircularWaveform(
    centerX: Float,
    centerY: Float,
    maxRadius: Float,
    bars: FloatArray,
    rotation: Float,
    pulseValue: Float
) {
    val barCount = bars.size
    val angleStep = 360f / barCount

    repeat(barCount) { index ->
        val angle = (angleStep * index + rotation) * (PI / 180f).toFloat()
        val barHeight = bars[index] * maxRadius * (0.5f + pulseValue * 0.3f)

        val startRadius = maxRadius * 0.4f
        val endRadius = startRadius + barHeight

        val startX = centerX + startRadius * cos(angle)
        val startY = centerY + startRadius * sin(angle)

        val endX = centerX + endRadius * cos(angle)
        val endY = centerY + endRadius * sin(angle)

        val colorHue = (index.toFloat() / barCount * 360f + rotation) % 360f
        val color = getColorFromHue(colorHue)

        drawLine(
            color = color,
            start = androidx.compose.ui.geometry.Offset(startX, startY),
            end = androidx.compose.ui.geometry.Offset(endX, endY),
            strokeWidth = 3f
        )

        drawLine(
            color = color.copy(alpha = 0.3f),
            start = androidx.compose.ui.geometry.Offset(startX, startY),
            end = androidx.compose.ui.geometry.Offset(endX, endY),
            strokeWidth = 8f
        )
    }
}

private fun DrawScope.drawRadialBars(
    centerX: Float,
    centerY: Float,
    radius: Float,
    bars: FloatArray,
    rotation: Float,
    pulseValue: Float
) {
    val barCount = 32
    val angleStep = 360f / barCount

    repeat(barCount) { index ->
        val angle = (angleStep * index - rotation * 2) * (PI / 180f).toFloat()
        val barValue = bars[(index * bars.size / barCount).toInt()].coerceIn(0f, 1f)
        val barHeight = barValue * radius * 0.6f * (0.7f + pulseValue * 0.3f)

        val x1 = centerX + (radius - barHeight) * cos(angle)
        val y1 = centerY + (radius - barHeight) * sin(angle)

        val x2 = centerX + radius * cos(angle)
        val y2 = centerY + radius * sin(angle)

        val colorHue = (index.toFloat() / barCount * 360f - rotation) % 360f
        val color = getColorFromHue(colorHue).copy(alpha = 0.8f)

        drawLine(
            color = color,
            start = androidx.compose.ui.geometry.Offset(x1, y1),
            end = androidx.compose.ui.geometry.Offset(x2, y2),
            strokeWidth = 2.5f
        )
    }
}

private fun DrawScope.drawNucleus(
    centerX: Float,
    centerY: Float,
    pulseValue: Float
) {
    val nucleusRadius = 15f * (0.7f + pulseValue * 0.3f)

    drawCircle(
        color = Color(0xFF03DAC6).copy(alpha = 0.4f),
        center = androidx.compose.ui.geometry.Offset(centerX, centerY),
        radius = nucleusRadius * 1.5f
    )

    drawCircle(
        color = Color(0xFF03DAC6),
        center = androidx.compose.ui.geometry.Offset(centerX, centerY),
        radius = nucleusRadius
    )

    drawCircle(
        color = Color.White.copy(alpha = 0.6f),
        center = androidx.compose.ui.geometry.Offset(centerX, centerY),
        radius = nucleusRadius * 0.6f
    )
}

private fun getColorFromHue(hue: Float): Color {
    val h = hue.coerceIn(0f, 360f)
    val c = 1f
    val x = c * (1 - abs((h / 60f) % 2 - 1))
    val m = 0f

    val (r, g, b) = when {
        h < 60 -> Triple(c, x, 0f)
        h < 120 -> Triple(x, c, 0f)
        h < 180 -> Triple(0f, c, x)
        h < 240 -> Triple(0f, x, c)
        h < 300 -> Triple(x, 0f, c)
        else -> Triple(c, 0f, x)
    }

    return Color(
        red = (r + m).coerceIn(0f, 1f),
        green = (g + m).coerceIn(0f, 1f),
        blue = (b + m).coerceIn(0f, 1f)
    )
}
