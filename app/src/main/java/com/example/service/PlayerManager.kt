package com.example.service

import android.content.ComponentName
import android.content.Context
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.Player
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import com.google.common.util.concurrent.ListenableFuture
import com.google.common.util.concurrent.MoreExecutors
import com.example.model.MusicTrack
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class PlayerManager private constructor(private val context: Context) {

    private var controllerFuture: ListenableFuture<MediaController>? = null
    var player: Player? = null
        private set

    private val _currentTrack = MutableStateFlow<MusicTrack?>(null)
    val currentTrack: StateFlow<MusicTrack?> = _currentTrack.asStateFlow()

    private val _isPlaying = MutableStateFlow(false)
    val isPlaying: StateFlow<Boolean> = _isPlaying.asStateFlow()

    private val _currentPosition = MutableStateFlow(0L)
    val currentPosition: StateFlow<Long> = _currentPosition.asStateFlow()

    private val _duration = MutableStateFlow(0L)
    val duration: StateFlow<Long> = _duration.asStateFlow()

    private var tracksList: List<MusicTrack> = emptyList()
    
    private val scope = CoroutineScope(Dispatchers.Main + Job())
    private var progressJob: Job? = null

    init {
        initializeController()
    }

    private fun initializeController() {
        val sessionToken = SessionToken(
            context,
            ComponentName(context, PlaybackService::class.java)
        )
        controllerFuture = MediaController.Builder(context, sessionToken).buildAsync()
        controllerFuture?.addListener(
            {
                player = controllerFuture?.get()
                player?.addListener(object : Player.Listener {
                    override fun onIsPlayingChanged(isPlaying: Boolean) {
                        _isPlaying.value = isPlaying
                        if (isPlaying) startProgressTracker() else stopProgressTracker()
                    }

                    override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
                        updateCurrentTrack(mediaItem)
                    }

                    override fun onPlaybackStateChanged(playbackState: Int) {
                        if (playbackState == Player.STATE_READY) {
                            _duration.value = player?.duration?.coerceAtLeast(0L) ?: 0L
                        }
                    }
                })
            },
            MoreExecutors.directExecutor()
        )
    }

    private fun updateCurrentTrack(mediaItem: MediaItem?) {
        val id = mediaItem?.mediaId
        _currentTrack.value = tracksList.find { it.id == id }
    }

    private fun startProgressTracker() {
        progressJob?.cancel()
        progressJob = scope.launch {
            while (true) {
                _currentPosition.value = player?.currentPosition ?: 0L
                delay(500)
            }
        }
    }

    private fun stopProgressTracker() {
        progressJob?.cancel()
    }

    fun playTrackList(tracks: List<MusicTrack>, startIndex: Int = 0) {
        tracksList = tracks
        val mediaItems = tracks.map { track ->
            MediaItem.Builder()
                .setMediaId(track.id)
                .setUri(track.uri)
                .setMediaMetadata(
                    MediaMetadata.Builder()
                        .setTitle(track.title)
                        .setArtist(track.artist)
                        .setAlbumTitle(track.album)
                        .build()
                )
                .build()
        }
        
        player?.let {
            it.setMediaItems(mediaItems, startIndex, 0)
            it.prepare()
            it.play()
        }
    }

    fun togglePlayPause() {
        player?.let {
            if (it.isPlaying) it.pause() else it.play()
        }
    }
    
    fun playNext() {
        player?.seekToNext()
    }
    
    fun playPrevious() {
        player?.seekToPrevious()
    }

    fun seekTo(position: Long) {
        player?.seekTo(position)
    }

    fun release() {
        controllerFuture?.let { MediaController.releaseFuture(it) }
        stopProgressTracker()
    }

    companion object {
        @Volatile
        private var instance: PlayerManager? = null

        fun getInstance(context: Context): PlayerManager {
            return instance ?: synchronized(this) {
                instance ?: PlayerManager(context.applicationContext).also { instance = it }
            }
        }
    }
}
