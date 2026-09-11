package com.example.ui.screens

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.QueueMusic
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.model.MusicTrack
import com.example.viewmodel.MusicViewModel

@OptIn(ExperimentalAnimationApi::class, ExperimentalMaterial3Api::class)
@Composable
fun NowPlayingScreen(
    viewModel: MusicViewModel,
    onNavigateBack: () -> Unit
) {
    val currentTrack by viewModel.playerManager.currentTrack.collectAsState()
    val isPlaying by viewModel.playerManager.isPlaying.collectAsState()
    val position by viewModel.playerManager.currentPosition.collectAsState()
    val duration by viewModel.playerManager.duration.collectAsState()
    val whyRecommended by viewModel.whyRecommendedText.collectAsState()

    val track = currentTrack ?: return

    Box(modifier = Modifier.fillMaxSize().background(Color.Black)) {
        // Blurred Background
        AnimatedContent(
            targetState = track.artworkUri,
            transitionSpec = { fadeIn(tween(1000)) togetherWith fadeOut(tween(1000)) },
            label = "bg_anim"
        ) { uri ->
            AsyncImage(
                model = uri ?: android.R.drawable.ic_media_play, // Fallback
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxSize()
                    .blur(100.dp)
            )
        }
        
        // Dark Overlay Gradient
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color.Black.copy(alpha = 0.3f),
                            Color.Black.copy(alpha = 0.8f),
                            Color.Black
                        )
                    )
                )
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.systemBars)
                .padding(horizontal = 24.dp)
        ) {
            // Top Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onNavigateBack) {
                    Icon(Icons.Default.KeyboardArrowDown, contentDescription = "Close", tint = Color.White)
                }
                Text(
                    text = "NOW PLAYING",
                    style = MaterialTheme.typography.labelMedium,
                    color = Color.White.copy(alpha = 0.7f),
                    letterSpacing = 2.sp
                )
                IconButton(onClick = { /* More Options */ }) {
                    Icon(Icons.Default.MoreVert, contentDescription = "More", tint = Color.White)
                }
            }

            Spacer(modifier = Modifier.weight(1f))

            // Album Artwork
            AnimatedContent(
                targetState = track.artworkUri,
                transitionSpec = { fadeIn(tween(600)) togetherWith fadeOut(tween(600)) },
                label = "art_anim",
                modifier = Modifier.align(Alignment.CenterHorizontally)
            ) { uri ->
                AsyncImage(
                    model = uri ?: android.R.drawable.ic_media_play,
                    contentDescription = "Album Art",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(1f)
                        .clip(RoundedCornerShape(32.dp))
                )
            }

            Spacer(modifier = Modifier.height(32.dp))

            // AI Insight (Why this song)
            if (whyRecommended != null) {
                Surface(
                    color = Color.White.copy(alpha = 0.1f),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.AutoAwesome, contentDescription = "AI", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = whyRecommended!!,
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.White.copy(alpha = 0.9f),
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
                Spacer(modifier = Modifier.height(24.dp))
            }

            // Track Info
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = track.title,
                        style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
                        color = Color.White,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = track.artist,
                        style = MaterialTheme.typography.titleMedium,
                        color = Color.White.copy(alpha = 0.7f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                IconButton(onClick = { /* Favorite */ }) {
                    Icon(Icons.Default.FavoriteBorder, contentDescription = "Favorite", tint = Color.White)
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Progress Bar
            Slider(
                value = position.toFloat(),
                onValueChange = { viewModel.playerManager.seekTo(it.toLong()) },
                valueRange = 0f..(if (duration > 0) duration.toFloat() else 1f),
                colors = SliderDefaults.colors(
                    thumbColor = Color.White,
                    activeTrackColor = Color.White,
                    inactiveTrackColor = Color.White.copy(alpha = 0.3f)
                ),
                modifier = Modifier.fillMaxWidth()
            )
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(formatTime(position), style = MaterialTheme.typography.labelSmall, color = Color.White.copy(alpha = 0.6f))
                Text(formatTime(duration), style = MaterialTheme.typography.labelSmall, color = Color.White.copy(alpha = 0.6f))
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Playback Controls
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 32.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = { /* Shuffle */ }) {
                    Icon(Icons.Default.Shuffle, contentDescription = "Shuffle", tint = Color.White.copy(alpha = 0.7f))
                }
                IconButton(onClick = { viewModel.playerManager.playPrevious() }, modifier = Modifier.size(56.dp)) {
                    Icon(Icons.Default.SkipPrevious, contentDescription = "Previous", tint = Color.White, modifier = Modifier.size(40.dp))
                }
                
                Surface(
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier
                        .size(80.dp)
                        .clickable { viewModel.playerManager.togglePlayPause() }
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                            contentDescription = "Play/Pause",
                            tint = Color.White,
                            modifier = Modifier.size(48.dp)
                        )
                    }
                }
                
                IconButton(onClick = { viewModel.playerManager.playNext() }, modifier = Modifier.size(56.dp)) {
                    Icon(Icons.Default.SkipNext, contentDescription = "Next", tint = Color.White, modifier = Modifier.size(40.dp))
                }
                IconButton(onClick = { /* Repeat */ }) {
                    Icon(Icons.Default.Repeat, contentDescription = "Repeat", tint = Color.White.copy(alpha = 0.7f))
                }
            }
        }
    }
}

fun formatTime(ms: Long): String {
    val totalSeconds = ms / 1000
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return String.format("%02d:%02d", minutes, seconds)
}
