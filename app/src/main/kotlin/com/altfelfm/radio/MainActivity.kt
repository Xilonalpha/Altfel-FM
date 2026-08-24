package com.altfelfm.radio

import android.os.Bundle
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.altfelfm.radio.audio.AudioVisualizer
import com.altfelfm.radio.audio.RadioPlayer
import com.altfelfm.radio.audio.StreamQuality
import kotlinx.coroutines.delay

class MainActivity : ComponentActivity() {
    private lateinit var radioPlayer: RadioPlayer

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        radioPlayer = RadioPlayer(this)

        setContent {
            MaterialTheme {
                MainScreen(radioPlayer)
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        radioPlayer.release()
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(player: RadioPlayer) {
    val isPlaying by player.isPlaying.collectAsState()
    val currentQuality by player.currentQuality.collectAsState()

    var colorIndex by remember { mutableIntStateOf(0) }
    val colorPalettes = listOf(
        listOf(Color(0xFF1A1A2E), Color(0xFF16213E), Color(0xFF0F3460)),
        listOf(Color(0xFF2C3E50), Color(0xFF000000), Color(0xFF4CA1AF)),
        listOf(Color(0xFF11998E), Color(0xFF38EF7D), Color(0xFF050505)),
        listOf(Color(0xFF8A2387), Color(0xFFE94057), Color(0xFF121212))
    )

    LaunchedEffect(isPlaying) {
        while (isPlaying) {
            delay(6000)
            colorIndex = (colorIndex + 1) % colorPalettes.size
        }
    }

    val startColor by animateColorAsState(
        targetValue = colorPalettes[colorIndex][0],
        animationSpec = tween(durationMillis = 2000), label = "startColor"
    )
    val endColor by animateColorAsState(
        targetValue = colorPalettes[colorIndex][1],
        animationSpec = tween(durationMillis = 2000), label = "endColor"
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Altfel FM", fontWeight = FontWeight.Bold, color = Color.White) },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color(0xFF121212))
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(Brush.verticalGradient(listOf(startColor, endColor, Color.Black)))
                .padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = if (isPlaying) "În Redare..." else "Oprit",
                    color = Color.White,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.SemiBold
                )

                Spacer(modifier = Modifier.height(10.dp))

                IconButton(
                    onClick = { player.togglePlay() },
                    modifier = Modifier
                        .size(64.dp)
                        .background(Color(0xFFFF0055), shape = RoundedCornerShape(32.dp))
                ) {
                    Icon(
                        imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                        contentDescription = "Play/Pause",
                        tint = Color.White,
                        modifier = Modifier.size(36.dp)
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Button(
                        onClick = { player.setQuality(StreamQuality.HIGH) },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (currentQuality == StreamQuality.HIGH) Color(0xFFFF0055) else Color.DarkGray
                        )
                    ) { Text("328 kbps") }

                    Button(
                        onClick = { player.setQuality(StreamQuality.LOW) },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (currentQuality == StreamQuality.LOW) Color(0xFFFF0055) else Color.DarkGray
                        )
                    ) { Text("128 kbps") }
                }

                Spacer(modifier = Modifier.height(16.dp))

                AudioVisualizer(
                    isPlaying = isPlaying,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(60.dp)
                )
            }

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .clip(RoundedCornerShape(16.dp)),
                colors = CardDefaults.cardColors(containerColor = Color(0xAA1E1E1E))
            ) {
                AndroidView(
                    factory = { context ->
                        WebView(context).apply {
                            webViewClient = WebViewClient()
                            settings.javaScriptEnabled = true
                            settings.useWideViewPort = true
                            settings.loadWithOverviewMode = true
                            loadUrl("https://altfelfm.ro/chat")
                        }
                    },
                    modifier = Modifier.fillMaxSize()
                )
            }
        }
    }
}
