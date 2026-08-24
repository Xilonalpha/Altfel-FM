package com.altfelfm.radio.audio

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.media.audiofx.Visualizer
import android.util.Log
import androidx.core.content.ContextCompat
import androidx.media3.common.MediaItem
import androidx.media3.exoplayer.ExoPlayer
import kotlin.math.sqrt

enum class StreamQuality(val bitrate: String, val displayName: String, val streamUrl: String) {
    QUALITY_320("320k", "320 kbps", "http://live.altfelfm.ro:8120/320.mp3"),
    QUALITY_128("128k", "128 kbps", "http://live.altfelfm.ro:8120/128.mp3")
}

/**
 * @param onMagnitudesUpdate primit de fiecare dată când Visualizer capturează un nou cadru FFT
 *        REAL din sesiunea audio a ExoPlayer-ului. Nu e simulare - array-ul reflectă exact
 *        energia pe fiecare bandă de frecvență din sunetul redat în acel moment.
 */
class RadioPlayer(
    private val context: Context,
    private val onMagnitudesUpdate: (FloatArray) -> Unit
) {
    private var exoPlayer: ExoPlayer? = null
    private var visualizer: Visualizer? = null
    private var currentQuality: StreamQuality = StreamQuality.QUALITY_320

    init {
        initializePlayer()
    }

    private fun initializePlayer() {
        if (exoPlayer == null) {
            exoPlayer = ExoPlayer.Builder(context).build().apply {
                setHandleAudioBecomingNoisy(true)
                addListener(PlayerListener())
            }
        }
    }

    private fun hasRecordAudioPermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.RECORD_AUDIO
        ) == PackageManager.PERMISSION_GRANTED
    }

    /**
     * Leagă un Visualizer de audioSessionId-ul REAL al ExoPlayer-ului (nu de session 0 / master mix).
     * Se re-încearcă la fiecare play() în caz că permisiunea RECORD_AUDIO a fost acordată
     * ulterior de utilizator (dialogul de permisiuni e async față de construcția player-ului).
     */
    private fun setupVisualizer() {
        if (visualizer != null) return
        if (!hasRecordAudioPermission()) {
            Log.w("RadioPlayer", "RECORD_AUDIO neacordat - vizualizatorul rămâne inactiv (fără date simulate)")
            return
        }
        val sessionId = exoPlayer?.audioSessionId ?: return
        if (sessionId == 0) {
            Log.w("RadioPlayer", "audioSessionId invalid, Visualizer nu poate porni încă")
            return
        }
        try {
            val captureRange = Visualizer.getCaptureSizeRange()
            val captureSize = captureRange[1].coerceAtMost(1024).coerceAtLeast(captureRange[0])
            visualizer = Visualizer(sessionId).apply {
                this.captureSize = captureSize
                setDataCaptureListener(
                    object : Visualizer.OnDataCaptureListener {
                        override fun onWaveFormDataCapture(v: Visualizer?, waveform: ByteArray?, samplingRate: Int) {
                            // Folosim doar FFT (spectru), nu waveform brut.
                        }

                        override fun onFftDataCapture(v: Visualizer?, fft: ByteArray?, samplingRate: Int) {
                            fft ?: return
                            onMagnitudesUpdate(computeMagnitudes(fft))
                        }
                    },
                    Visualizer.getMaxCaptureRate() / 2,
                    /* waveform = */ false,
                    /* fft = */ true
                )
                enabled = true
            }
            Log.d("RadioPlayer", "Visualizer real conectat la audioSessionId=$sessionId, captureSize=$captureSize")
        } catch (e: Exception) {
            Log.e("RadioPlayer", "Nu am putut porni Visualizer real: ${e.message}", e)
            visualizer = null
        }
    }

    /**
     * Formatul FFT din Android: fft[0]=Re(DC), fft[1]=Re(Nyquist), apoi perechi (Re,Im)
     * pentru fiecare bandă de frecvență intermediară. Calculăm magnitudinea reală
     * (sqrt(Re^2+Im^2)) pentru fiecare bandă - asta e "energia" sunetului pe frecvența respectivă,
     * exact ceea ce arată un egalizor audio adevărat.
     */
    private fun computeMagnitudes(fft: ByteArray): FloatArray {
        val n = fft.size / 2
        if (n <= 0) return FloatArray(0)
        val mags = FloatArray(n)
        mags[0] = kotlin.math.abs(fft[0].toInt()).toFloat()
        for (k in 1 until n) {
            val re = fft[2 * k].toInt()
            val im = fft[2 * k + 1].toInt()
            mags[k] = sqrt((re * re + im * im).toFloat())
        }
        return mags
    }

    fun play(quality: StreamQuality) {
        try {
            currentQuality = quality
            val streamUrl = buildStreamUrl(quality)

            Log.d("RadioPlayer", "Playing stream: $streamUrl")

            val mediaItem = MediaItem.fromUri(streamUrl)
            exoPlayer?.apply {
                setMediaItem(mediaItem)
                prepare()
                play()
            }
            setupVisualizer()
            visualizer?.enabled = true
        } catch (e: Exception) {
            Log.e("RadioPlayer", "Error playing stream: ${e.message}", e)
        }
    }

    fun stop() {
        visualizer?.enabled = false
        exoPlayer?.apply {
            stop()
            clearMediaItems()
        }
    }

    fun pause() {
        visualizer?.enabled = false
        exoPlayer?.pause()
    }

    fun resume() {
        exoPlayer?.play()
        visualizer?.enabled = true
    }

    fun isPlaying(): Boolean = exoPlayer?.isPlaying ?: false

    fun getAudioSessionId(): Int = exoPlayer?.audioSessionId ?: 0

    fun release() {
        visualizer?.release()
        visualizer = null
        exoPlayer?.apply {
            stop()
            release()
        }
        exoPlayer = null
    }

    private fun buildStreamUrl(quality: StreamQuality): String {
        return quality.streamUrl
    }

    private inner class PlayerListener : androidx.media3.common.Player.Listener {
        override fun onPlayerError(error: androidx.media3.common.PlaybackException) {
            Log.e("RadioPlayer", "Player error: ${error.message}")
        }

        override fun onIsLoadingChanged(isLoading: Boolean) {
            Log.d("RadioPlayer", "Loading: $isLoading")
        }
    }
}
