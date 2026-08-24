package com.altfelfm.radio

import android.os.Bundle
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.altfelfm.radio.audio.AudioVisualizer
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

    // Preluare titlu piesă de pe serverul Shoutcast/Icecast la fiecare 5 secunde
    LaunchedEffect(Unit) {
        while (true) {
            try {
                withContext(Dispatchers.IO) {
                    val streamUrl = "https://live.altfelfm.ro:8120/stats?sid=1&json=1"
                    val jsonText = URL(streamUrl).readText()
                    val json = JSONObject(jsonText)
                    if (json.has("songtitle")) {
                        songTitle = json.getString("songtitle")
                    } else if (json.has("title")) {
                        songTitle = json.getString("title")
                    }
                }
            } catch (e: Exception) {
                // Dacă serverul folosește Icecast clasic status-json.xsl
                try {
                    withContext(Dispatchers.IO) {
                        val streamUrl = "https://live.altfelfm.ro:8120/status-json.xsl"
                        val jsonText = URL(streamUrl).readText()
                        val json = JSONObject(jsonText)
                        val source = json.getJSONObject("icestats").get("source")
                        if (source is JSONObject) {
                            songTitle = source.optString("title", "Altfel FM Live")
                        }
                    }
                } catch (ex: Exception) {
                    if (songTitle == "Se încarcă piesa...") {
                        songTitle = "Altfel FM - Live Stream"
                    }
                }
            }
            delay(5000)
        }
    }

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
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Card Piesa Curenta Nativ
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 10.dp),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0x33FFFFFF))
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "ACUM CÂNTĂ",
                        color = Color(0xFFFF0055),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = songTitle,
                        color = Color.White,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center
                    )
                }
            }

            // Controale Player Nativ
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.fillMaxWidth()
            ) {
                IconButton(
                    onClick = { player.togglePlay() },
                    modifier = Modifier
                        .size(80.dp)
                        .background(Color(0xFFFF0055), shape = RoundedCornerShape(40.dp))
                ) {
                    Icon(
                        imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                        contentDescription = "Play/Pause",
                        tint = Color.White,
                        modifier = Modifier.size(48.dp)
                    )
                }

                Spacer(modifier = Modifier.height(20.dp))

                Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
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

                Spacer(modifier = Modifier.height(24.dp))

                AudioVisualizer(
                    isPlaying = isPlaying,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(70.dp)
                )
            }

            Spacer(modifier = Modifier.height(10.dp))
        }
    }
}
