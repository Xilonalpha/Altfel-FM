package com.altfelfm.radio

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewmodel.compose.viewModel
import com.altfelfm.radio.audio.AudioVisualizer
import com.altfelfm.radio.audio.RadioPlayer
import com.altfelfm.radio.audio.StreamQuality
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    // Cerem ambele permisiuni deodată: POST_NOTIFICATIONS (pt. notificarea de redare) și
    // RECORD_AUDIO (obligatorie pentru android.media.audiofx.Visualizer - fără ea,
    // vizualizatorul rămâne inactiv, dar NU trece pe date simulate).
    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { /* RadioPlayer verifică singur permisiunea la fiecare play(), nu e nevoie de acțiune aici */ }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val toRequest = mutableListOf<String>()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED
            ) {
                toRequest += Manifest.permission.POST_NOTIFICATIONS
            }
        }
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO)
            != PackageManager.PERMISSION_GRANTED
        ) {
            toRequest += Manifest.permission.RECORD_AUDIO
        }
        if (toRequest.isNotEmpty()) {
            permissionLauncher.launch(toRequest.toTypedArray())
        }

        setContent {
            AltfelRadioApp()
        }
    }
}

@Composable
fun AltfelRadioApp(
    viewModel: RadioViewModel = viewModel()
) {
    val context = LocalContext.current
    // FIX: în varianta originală RadioViewModel.initializePlayer() exista dar nu era
    // apelat niciodată - radioPlayer rămânea null, deci play() nu pornea nimic.
    LaunchedEffect(Unit) {
        viewModel.initializePlayer(context)
    }

    MaterialTheme(
        colorScheme = darkColorScheme(
            primary = Color(0xFF6200EE),
            secondary = Color(0xFF03DAC6),
            tertiary = Color(0xFF1F1F1F),
            background = Color(0xFF121212),
            surface = Color(0xFF1E1E1E),
        )
    ) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background
        ) {
            RadioScreen(viewModel)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RadioScreen(viewModel: RadioViewModel) {
    var showQualityMenu by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    val isPlaying by viewModel.isPlaying.collectAsState()
    val currentQuality by viewModel.currentQuality.collectAsState()
    val stationName by viewModel.stationName.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        TopAppBar(
            title = {
                Text(
                    "Altfel FM",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF6200EE)
                )
            },
            actions = {
                Box {
                    IconButton(onClick = { showQualityMenu = true }) {
                        Icon(Icons.Default.MoreVert, contentDescription = "Quality")
                    }
                    DropdownMenu(
                        expanded = showQualityMenu,
                        onDismissRequest = { showQualityMenu = false }
                    ) {
                        StreamQuality.values().forEach { quality ->
                            DropdownMenuItem(
                                text = {
                                    Text(
                                        quality.displayName,
                                        fontSize = 14.sp
                                    )
                                },
                                onClick = {
                                    scope.launch {
                                        viewModel.changeQuality(quality)
                                    }
                                    showQualityMenu = false
                                }
                            )
                        }
                    }
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = Color.Transparent,
                titleContentColor = Color.White
            )
        )

        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            AudioVisualizerComponent(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(300.dp)
                    .padding(16.dp),
                isPlaying = isPlaying,
                magnitudesFlow = viewModel.fftMagnitudes
            )

            Spacer(modifier = Modifier.height(40.dp))

            Text(
                stationName,
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )

            Text(
                "Live Stream",
                fontSize = 14.sp,
                color = Color(0xFF03DAC6)
            )

            Spacer(modifier = Modifier.height(20.dp))

            Card(
                modifier = Modifier
                    .padding(16.dp)
                    .fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = Color(0xFF2A2A2A)
                )
            ) {
                Text(
                    "Calitate: ${currentQuality.displayName}",
                    modifier = Modifier
                        .align(Alignment.CenterHorizontally)
                        .padding(16.dp),
                    fontSize = 16.sp,
                    color = Color(0xFF03DAC6),
                    fontWeight = FontWeight.Medium
                )
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Button(
                onClick = {
                    if (isPlaying) {
                        viewModel.stop()
                    } else {
                        viewModel.play()
                    }
                },
                modifier = Modifier.size(80.dp),
                shape = MaterialTheme.shapes.extraLarge,
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF6200EE)
                )
            ) {
                Icon(
                    imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                    contentDescription = if (isPlaying) "Pauză" else "Play",
                    modifier = Modifier.size(40.dp),
                    tint = Color.White
                )
            }
        }

        Spacer(modifier = Modifier.height(20.dp))
    }
}

@Composable
fun AudioVisualizerComponent(
    modifier: Modifier = Modifier,
    isPlaying: Boolean,
    magnitudesFlow: StateFlow<FloatArray?>
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = Color(0xFF2A2A2A)),
        shape = MaterialTheme.shapes.large
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            contentAlignment = Alignment.Center
        ) {
            AudioVisualizer(
                modifier = Modifier.fillMaxSize(),
                isPlaying = isPlaying,
                magnitudesFlow = magnitudesFlow
            )
        }
    }
}

class RadioViewModel : ViewModel() {
    private val _isPlaying = MutableStateFlow(false)
    val isPlaying: StateFlow<Boolean> = _isPlaying.asStateFlow()

    private val _currentQuality = MutableStateFlow(StreamQuality.QUALITY_320)
    val currentQuality: StateFlow<StreamQuality> = _currentQuality.asStateFlow()

    private val _stationName = MutableStateFlow("Altfel FM")
    val stationName: StateFlow<String> = _stationName.asStateFlow()

    // Date FFT REALE, populate direct din callback-ul android.media.audiofx.Visualizer
    // (vezi RadioPlayer). Rămâne null cât timp nu se redă nimic.
    private val _fftMagnitudes = MutableStateFlow<FloatArray?>(null)
    val fftMagnitudes: StateFlow<FloatArray?> = _fftMagnitudes.asStateFlow()

    private var radioPlayer: RadioPlayer? = null

    fun initializePlayer(context: Context) {
        if (radioPlayer == null) {
            radioPlayer = RadioPlayer(context) { magnitudes ->
                _fftMagnitudes.value = magnitudes
            }
        }
    }

    fun play() {
        radioPlayer?.play(_currentQuality.value)
        _isPlaying.value = true
    }

    fun stop() {
        radioPlayer?.stop()
        _isPlaying.value = false
        _fftMagnitudes.value = null
    }

    suspend fun changeQuality(quality: StreamQuality) {
        val wasPlaying = _isPlaying.value
        if (wasPlaying) {
            radioPlayer?.stop()
        }
        _currentQuality.value = quality
        if (wasPlaying) {
            radioPlayer?.play(quality)
        }
    }

    override fun onCleared() {
        radioPlayer?.release()
        super.onCleared()
    }
}
