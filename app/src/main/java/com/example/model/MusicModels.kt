package com.example.model

import kotlinx.serialization.Serializable

@Serializable
data class MusicTrack(
    val id: String,
    val title: String,
    val artist: String,
    val album: String,
    val durationMs: Long,
    val uri: String,
    val artworkUri: String? = null,
    val dateAdded: Long = 0L,
    // AI enhanced metadata
    val aiMood: String? = null,
    val aiGenre: String? = null,
    val aiEnergy: Float? = null,
    val aiWhyRecommended: String? = null
)

@Serializable
data class Playlist(
    val id: String,
    val name: String,
    val description: String,
    val tracks: List<MusicTrack>,
    val isAiGenerated: Boolean = false
)

enum class PlaybackState {
    PLAYING, PAUSED, STOPPED, BUFFERING
}

enum class RepeatMode {
    OFF, ONE, ALL
}

@Serializable
data class TasteProfile(
    val favoriteGenres: List<String> = emptyList(),
    val favoriteArtists: List<String> = emptyList(),
    val topMoods: List<String> = emptyList(),
    val opennessToNewMusic: Float = 0.5f // 0 to 1
)
