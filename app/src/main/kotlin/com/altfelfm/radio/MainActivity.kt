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
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.altfelfm.radio.audio.CircularAudioVisualizer
import com.altfelfm.radio.audio.RadioPlayer
import com.altfelfm.radio.audio.StreamQuality
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import java.net.URL
import org.json.JSONObject

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
    var songTitle by remember { mutableStateOf("Se încarcă piesa...") }

    // Preia piesă live la fiecare 5 secunde
    LaunchedEffect(Unit) {
        while (true) {
            try {
                withContext(Dispatchers.IO) {
                    val jsonText = URL("https://live.altfelfm.ro:8120/stats?sid=1&json=1").readText()
                    val json = JSONObject(jsonText)
                    if (json.has("songtitle")) {
                        val title = json.getString("songtitle")
                        if (title.isNotEmpty()) songTitle = title
                    }
                }
            } catch (e: Exception) {
                // Păstrează titlul existent
            }
            delay(5000)
        }
    }

    // Schimbare de fundal pe melodie nouă
    var paletteIndex by remember(songTitle) { mutableIntStateOf((0..2).random()) }
    val colorPalettes = listOf(
        listOf(Color(0xFF0F2027), Color(0xFF203A43), Color(0xFF2C5364)),
        listOf(Color(0xFF1F1C2C), Color(0xFF4A0E17), Color(0xFF101010)),
        listOf(Color(0xFF1A1A2E), Color(0xFF16213E), Color(0xFF0F3460))
    )

    val startColor by animateColorAsState(
        targetValue = colorPalettes[paletteIndex][0],
        animationSpec = tween(1500), label = "startColor"
    )
    val endColor by animateColorAsState(
        targetValue = colorPalettes[paletteIndex][1],
        animationSpec = tween(1500), label = "endColor"
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    Text(
                        "Altfel FM", 
                        fontWeight = FontWeight.Bold, 
                        fontSize = 22.sp,
                        color = Color.White,
                        modifier = Modifier.fillMaxWidth(),
                        textAlign = TextAlign.Center
                    ) 
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
            )
        },
        containerColor = Color.Transparent
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(Brush.verticalGradient(listOf(startColor, endColor, Color.Black)))
                .padding(horizontal = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = songTitle,
                color = Color.White.copy(alpha = 0.9f),
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(vertical = 4.dp)
            )

            Spacer(modifier = Modifier.height(8.dp))

            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.size(200.dp)
            ) {
                CircularAudioVisualizer(
                    isPlaying = isPlaying,
                    songTitle = songTitle,
                    modifier = Modifier.fillMaxSize()
                )

                IconButton(
                    onClick = { player.togglePlay() },
                    modifier = Modifier
                        .size(100.dp)
                        .background(Color(0xFFFF0055), shape = CircleShape)
                ) {
                    Icon(
                        imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                        contentDescription = "Play/Pause",
                        tint = Color.White,
                        modifier = Modifier.size(54.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Button(
                    onClick = { player.setQuality(StreamQuality.HIGH) },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (currentQuality == StreamQuality.HIGH) Color(0xFFFF0055) else Color.White.copy(alpha = 0.2f)
                    ),
                    shape = RoundedCornerShape(20.dp)
                ) { Text("328 kbps", fontWeight = FontWeight.Bold) }

                Button(
                    onClick = { player.setQuality(StreamQuality.LOW) },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (currentQuality == StreamQuality.LOW) Color(0xFFFF0055) else Color.White.copy(alpha = 0.2f)
                    ),
                    shape = RoundedCornerShape(20.dp)
                ) { Text("128 kbps", fontWeight = FontWeight.Bold) }
            }

            // Chat integrat
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .padding(top = 12.dp, bottom = 8.dp)
                    .clip(RoundedCornerShape(20.dp)),
                colors = CardDefaults.cardColors(containerColor = Color(0x33000000))
            ) {
                AndroidView(
                    factory = { context ->
                        WebView(context).apply {
                            setBackgroundColor(0x00000000)
                            webViewClient = object : WebViewClient() {
                                override fun onPageFinished(view: WebView?, url: String?) {
                                    evaluateJavascript(
                                        """
                                        (function() {
                                            document.body.style.background = 'transparent';
                                            var elementsToHide = document.querySelectorAll('header, footer, .player, .player-holder, .now-playing, .radio-player, img');
                                            elementsToHide.forEach(function(el) { el.style.display = 'none'; });
                                        })();
                                        """.trimIndent(), null
                                    )
                                }
                            }
                            settings.javaScriptEnabled = true
                            settings.domStorageEnabled = true
                            loadUrl("https://altfelfm.ro/chat")
                        }
                    },
                    modifier = Modifier.fillMaxSize()
                )
            }
        }
    }
}
