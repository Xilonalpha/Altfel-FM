package com.altfelfm.radio.audio

import android.content.Context
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

enum class StreamQuality(val url: String, val label: String) {
    HIGH("https://live.altfelfm.ro/stream_328.mp3", "328 kbps"),
    LOW("https://live.altfelfm.ro/stream_128.mp3", "128 kbps")
}

class RadioPlayer(private val context: Context) {
    private var exoPlayer: ExoPlayer? = null

    private val _isPlaying = MutableStateFlow(false)
    val isPlaying: StateFlow<Boolean> = _isPlaying

    private val _currentQuality = MutableStateFlow(StreamQuality.HIGH)
    val currentQuality: StateFlow<StreamQuality> = _currentQuality

    private val _audioSessionId = MutableStateFlow(0)
    val audioSessionId: StateFlow<Int> = _audioSessionId

    init {
        initPlayer()
    }

    private fun initPlayer() {
        exoPlayer = ExoPlayer.Builder(context).build().apply {
            addListener(object : Player.Listener {
                override fun onIsPlayingChanged(isPlaying: Boolean) {
                    _isPlaying.value = isPlaying
                }
            })
        }
        _audioSessionId.value = exoPlayer?.audioSessionId ?: 0
    }

    fun play(quality: StreamQuality = _currentQuality.value) {
        _currentQuality.value = quality
        exoPlayer?.let { player ->
            player.stop()
            player.clearMediaItems()
            val mediaItem = MediaItem.fromUri(quality.url)
            player.setMediaItem(mediaItem)
            player.prepare()
            player.play()
        }
    }

    fun pause() {
        exoPlayer?.pause()
    }

    fun togglePlay() {
        if (_isPlaying.value) {
            pause()
        } else {
            play()
        }
    }

    fun setQuality(quality: StreamQuality) {
        if (_currentQuality.value != quality) {
            val werePlaying = _isPlaying.value
            _currentQuality.value = quality
            if (werePlaying) {
                play(quality)
            }
        }
    }

    fun release() {
        exoPlayer?.release()
        exoPlayer = null
    }
}
