package com.example.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.AiMusicDirector
import com.example.data.MusicRepository
import com.example.model.MusicTrack
import com.example.model.Playlist
import com.example.service.PlayerManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class MusicViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = MusicRepository(application)
    private val aiDirector = AiMusicDirector()
    val playerManager = PlayerManager.getInstance(application)

    private val _libraryTracks = MutableStateFlow<List<MusicTrack>>(emptyList())
    val libraryTracks = _libraryTracks.asStateFlow()

    private val _isLoading = MutableStateFlow(true)
    val isLoading = _isLoading.asStateFlow()

    private val _aiPlaylists = MutableStateFlow<List<Playlist>>(emptyList())
    val aiPlaylists = _aiPlaylists.asStateFlow()
    
    private val _whyRecommendedText = MutableStateFlow<String?>(null)
    val whyRecommendedText = _whyRecommendedText.asStateFlow()

    init {
        loadLibrary()
    }

    private fun loadLibrary() {
        viewModelScope.launch {
            _isLoading.value = true
            val tracks = repository.getLocalAudioFiles()
            _libraryTracks.value = tracks
            _isLoading.value = false
            
            // Generate some default AI moods based on library
            if (tracks.isNotEmpty()) {
                val chillPlaylist = aiDirector.generatePlaylist("Chill relaxed evening vibes", tracks)
                val focusPlaylist = aiDirector.generatePlaylist("Deep focus and study music", tracks)
                val newPlaylists = listOfNotNull(chillPlaylist, focusPlaylist)
                _aiPlaylists.value = newPlaylists
            }
        }
    }
    
    fun generateAiPlaylist(prompt: String) {
        viewModelScope.launch {
            val tracks = _libraryTracks.value
            val customPlaylist = aiDirector.generatePlaylist(prompt, tracks)
            if (customPlaylist != null) {
                _aiPlaylists.value = listOf(customPlaylist) + _aiPlaylists.value
                playPlaylist(customPlaylist)
            }
        }
    }

    fun playTrackList(tracks: List<MusicTrack>, startIndex: Int = 0) {
        playerManager.playTrackList(tracks, startIndex)
        if (tracks.isNotEmpty() && startIndex < tracks.size) {
            fetchWhyRecommended(tracks[startIndex])
        }
    }
    
    fun playPlaylist(playlist: Playlist) {
        playTrackList(playlist.tracks)
    }
    
    private fun fetchWhyRecommended(track: MusicTrack) {
        viewModelScope.launch {
            _whyRecommendedText.value = aiDirector.getWhyRecommended(track)
        }
    }

    override fun onCleared() {
        super.onCleared()
        // DO NOT release PlayerManager here because it's a singleton tied to app lifecycle,
        // but we could if we tied it differently.
    }
}
