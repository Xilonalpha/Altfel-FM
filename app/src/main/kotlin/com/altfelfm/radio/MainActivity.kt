package com.altfelfm.radio

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.View
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

@SuppressLint("SetJavaScriptEnabled")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(player: RadioPlayer) {
    val isPlaying by player.isPlaying.collectAsState()
    val currentQuality by player.currentQuality.collectAsState()

    var songTitle by remember { mutableStateOf("Gata să asculți") }

    LaunchedEffect(Unit) {
        while (true) {
            try {
                withContext(Dispatchers.IO) {
                    val jsonText = URL("https://live.altfelfm.ro:8120/stats?sid=1&json=1").readText()
                    val json = JSONObject(jsonText)
                    if (json.has("songtitle")) {
                        songTitle = json.getString("songtitle")
                    }
                }
            } catch (e: Exception) {
            }
            delay(5000)
        }
    }

    var colorIndex by remember { mutableIntStateOf(0) }
    val colorPalettes = listOf(
        listOf(Color(0xFF0F2027), Color(0xFF203A43), Color(0xFF2C5364)),
        listOf(Color(0xFF1F1C2C), Color(0xFF928DAB), Color(0xFF101010)),
        listOf(Color(0xFF1A1A2E), Color(0xFF16213E), Color(0xFF0F3460))
    )

    LaunchedEffect(isPlaying) {
        while (isPlaying) {
            delay(8000)
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
                title = { 
                    Text(
                        "Altfel FM", 
                        fontWeight = FontWeight.Bold, 
                        fontSize = 20.sp,
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
                .padding(horizontal = 12.dp, vertical = 4.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Player Nativ Android + Visualizer
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = songTitle,
                    color = Color.White.copy(alpha = 0.9f),
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium,
                    textAlign = TextAlign.Center,
                    maxLines = 1,
                    modifier = Modifier.padding(horizontal = 8.dp)
                )

                Spacer(modifier = Modifier.height(6.dp))

                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier.size(150.dp)
                ) {
                    CircularAudioVisualizer(
                        isPlaying = isPlaying,
                        songTitle = songTitle,
                        modifier = Modifier.fillMaxSize()
                    )

                    IconButton(
                        onClick = { player.togglePlay() },
                        modifier = Modifier
                            .size(70.dp)
                            .background(Color(0xFFFF0055), shape = CircleShape)
                    ) {
                        Icon(
                            imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                            contentDescription = "Play/Pause",
                            tint = Color.White,
                            modifier = Modifier.size(38.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))

                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Button(
                        onClick = { player.setQuality(StreamQuality.HIGH) },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (currentQuality == StreamQuality.HIGH) Color(0xFFFF0055) else Color.White.copy(alpha = 0.15f)
                        ),
                        shape = RoundedCornerShape(14.dp),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 2.dp)
                    ) { Text("328 kbps", fontSize = 11.sp, fontWeight = FontWeight.Bold) }

                    Button(
                        onClick = { player.setQuality(StreamQuality.LOW) },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (currentQuality == StreamQuality.LOW) Color(0xFFFF0055) else Color.White.copy(alpha = 0.15f)
                        ),
                        shape = RoundedCornerShape(14.dp),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 2.dp)
                    ) { Text("128 kbps", fontSize = 11.sp, fontWeight = FontWeight.Bold) }
                }
            }

            // Chat WebView corectat
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .padding(top = 8.dp, bottom = 4.dp)
                    .clip(RoundedCornerShape(20.dp)),
                colors = CardDefaults.cardColors(containerColor = Color(0x22000000))
            ) {
                AndroidView(
                    factory = { context ->
                        WebView(context).apply {
                            setBackgroundColor(0x00000000)
                            isVerticalScrollBarEnabled = false
                            isHorizontalScrollBarEnabled = false
                            overScrollMode = View.OVER_SCROLL_NEVER

                            settings.apply {
                                javaScriptEnabled = true
                                domStorageEnabled = true
                                loadsImagesAutomatically = true
                                mediaPlaybackRequiresUserGesture = true
                            }

                            webViewClient = object : WebViewClient() {
                                override fun onPageFinished(view: WebView?, url: String?) {
                                    super.onPageFinished(view, url)
                                    view?.evaluateJavascript(
                                        """
                                        (function() {
                                            // 1. Oprește sunetul web pentru a folosi playerul nativ Android
                                            document.querySelectorAll('audio, video').forEach(el => { el.pause(); el.src = ''; });

                                            // 2. Ascunde barul de scroll
                                            var style = document.createElement('style');
                                            style.innerHTML = '::-webkit-scrollbar { display: none !important; } html, body { background: transparent !important; }';
                                            document.head.appendChild(style);

                                            // 3. Căutăm exact cardurile Revolut și Bitcoin și le ascundem
                                            var allDivs = document.querySelectorAll('div');
                                            allDivs.forEach(function(el) {
                                                var text = el.innerText || '';
                                                if ((text.includes('Revolut') || text.includes('bitcoin')) && text.includes('Susține prin')) {
                                                    el.style.display = 'none';
                                                }
                                            });

                                            // 4. Ascundem link-urile secundare din footer (Leo1Romania, Pescar Amator, Dap Design, iconițe)
                                            var footerLinks = document.querySelectorAll('a, span');
                                            footerLinks.forEach(function(el) {
                                                var text = el.innerText || '';
                                                if (text.includes('Leo1Romania') || text.includes('Pescar Amator') || text.includes('Dap Design')) {
                                                    el.style.display = 'none';
                                                }
                                            });

                                            // 5. Ne asigurăm că chat-ul este în vizor și vizibil
                                            allDivs.forEach(function(el) {
                                                if (el.innerText && el.innerText.includes('SALUTA ALTFEL FM')) {
                                                    el.style.display = 'block';
                                                    el.scrollIntoView({ behavior: 'instant', block: 'start' });
                                                }
                                            });
                                        })();
                                        """.trimIndent(), null
                                    )
                                }
                            }
                            loadUrl("https://altfelfm.ro/chat")
                        }
                    },
                    modifier = Modifier.fillMaxSize()
                )
            }
        }
    }
}
