package com.example.radiyo.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.Star
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.radiyo.data.model.Moderator
import com.example.radiyo.data.model.UserRole
import com.example.radiyo.data.repository.UserRepository
import com.example.radiyo.ui.viewmodel.NowPlayingViewModel
import kotlinx.coroutines.delay

@Composable
fun NowPlayingScreen(
    modifier: Modifier = Modifier,
    viewModel: NowPlayingViewModel = viewModel()
) {
    val nowPlaying by viewModel.nowPlaying.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val currentUser by UserRepository.getInstance().currentUser.collectAsState()

    var showRatingDialog by remember { mutableStateOf(false) }

    if (isLoading && nowPlaying == null) {
        Box(
            modifier = modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator()
        }
        return
    }

    if (nowPlaying == null) {
        Box(
            modifier = modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(
                    imageVector = Icons.Default.MusicNote,
                    contentDescription = null,
                    modifier = Modifier.size(64.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "Gerade läuft nichts",
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Schau später vorbei!",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        return
    }

    val currentNowPlaying = nowPlaying!!
    val isListener = currentUser?.role == UserRole.LISTENER
    val isModerator = currentUser?.role == UserRole.MODERATOR
    val hasModerator = currentNowPlaying.moderator != null
    val isPlaying = currentNowPlaying.isPlaying

    var progress by remember(currentNowPlaying.song.id) { mutableFloatStateOf(0f) }
    val receivedAtLocal = remember(currentNowPlaying.song.id, currentNowPlaying.startedAt) {
        System.currentTimeMillis()
    }
    var pausedProgress by remember(currentNowPlaying.song.id) { mutableFloatStateOf(0f) }

    LaunchedEffect(
        currentNowPlaying.song.id,
        currentNowPlaying.startedAt,
        currentNowPlaying.song.durationMs,
        isPlaying
    ) {
        if (currentNowPlaying.song.durationMs > 0) {
            if (!isPlaying) {
                pausedProgress = progress
                return@LaunchedEffect
            }
            val resumeOffset =
                if (pausedProgress > 0) (pausedProgress * currentNowPlaying.song.durationMs).toLong() else 0L
            val adjustedReceivedAt =
                if (pausedProgress > 0) System.currentTimeMillis() - resumeOffset else receivedAtLocal
            while (true) {
                val elapsed = System.currentTimeMillis() - adjustedReceivedAt
                progress = (elapsed.toDouble() / currentNowPlaying.song.durationMs).coerceIn(0.0, 1.0).toFloat()
                delay(500)
            }
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(24.dp)
    ) {
        Text(
            text = "Wiedergabe aus dem Album",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Text(
            text = currentNowPlaying.song.album,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )

        Spacer(modifier = Modifier.height(12.dp))

        Card(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1f),
            shape = RoundedCornerShape(16.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.surfaceVariant),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.MusicNote,
                    contentDescription = null,
                    modifier = Modifier.size(80.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = currentNowPlaying.song.title,
            style = MaterialTheme.typography.headlineSmall,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )

        Spacer(modifier = Modifier.height(4.dp))

        Text(
            text = currentNowPlaying.song.artist,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(16.dp))

        LinearProgressIndicator(
            progress = { progress },
            modifier = Modifier
                .fillMaxWidth()
                .height(4.dp)
                .clip(RoundedCornerShape(2.dp)),
        )

        Spacer(modifier = Modifier.height(4.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = formatDuration((progress * currentNowPlaying.song.durationMs)),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = formatDuration(currentNowPlaying.song.durationMs),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        currentNowPlaying.playlist?.let { playlist ->
            Text(
                text = "Aktuelle Playlist: ${playlist.name}",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary
            )
            playlist.description?.let { description ->
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Beschreibung: $description",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        if (isModerator) {
            Spacer(modifier = Modifier.height(24.dp))
            PlaybackControls(
                isPlaying = isPlaying,
                onPlayPause = { viewModel.togglePlayPause() },
                onSkipPrevious = { viewModel.skipPrevious() },
                onSkipNext = { viewModel.skipNext() }
            )
        }

        Spacer(modifier = Modifier.weight(1f))

        if (isListener) {
            currentNowPlaying.moderator?.let { moderator ->
                ModeratorButton(moderator = moderator)
                Spacer(modifier = Modifier.height(12.dp))
            }
        }

        if (isListener) {
            FilledTonalButton(
                onClick = { showRatingDialog = true },
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(
                    imageVector = Icons.Default.Star,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Text(
                    text = "Bewertung abgeben",
                    modifier = Modifier.padding(start = 8.dp)
                )
            }
        }
    }

    if (showRatingDialog) {
        RatingDialog(
            showModerator = hasModerator,
            onSubmit = { playlistRating, moderatorRating ->
                viewModel.submitRatings(playlistRating, moderatorRating)
                showRatingDialog = false
            },
            onDismiss = { showRatingDialog = false }
        )
    }
}

private fun formatDuration(durationMs: Double): String {
    val totalSeconds = (durationMs / 1000).toLong()
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return "%d:%02d".format(minutes, seconds)
}

@Composable
private fun RatingDialog(
    showModerator: Boolean,
    onSubmit: (playlistRating: Int, moderatorRating: Int) -> Unit,
    onDismiss: () -> Unit
) {
    var playlistRating by remember { mutableIntStateOf(0) }
    var moderatorRating by remember { mutableIntStateOf(0) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(text = "Bewertung")
        },
        text = {
            Column {
                Text(
                    text = "Playlist Bewertungen",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(8.dp))
                StarRating(
                    rating = playlistRating,
                    onRatingChange = { playlistRating = it }
                )

                if (showModerator) {
                    Spacer(modifier = Modifier.height(24.dp))
                    Text(
                        text = "Moderator",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    StarRating(
                        rating = moderatorRating,
                        onRatingChange = { moderatorRating = it }
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onSubmit(playlistRating, moderatorRating) }
            ) {
                Text("Absenden")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Abbrechen")
            }
        }
    )
}

@Composable
private fun StarRating(
    rating: Int,
    onRatingChange: (Int) -> Unit,
    modifier: Modifier = Modifier,
    maxStars: Int = 5
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.Center
    ) {
        for (i in 1..maxStars) {
            IconButton(
                onClick = { onRatingChange(if (rating == i) 0 else i) }
            ) {
                Icon(
                    imageVector = if (i <= rating) Icons.Filled.Star else Icons.Outlined.Star,
                    contentDescription = "Stern $i",
                    tint = if (i <= rating) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    },
                    modifier = Modifier.size(32.dp)
                )
            }
        }
    }
}

@Composable
private fun PlaybackControls(
    isPlaying: Boolean,
    onPlayPause: () -> Unit,
    onSkipPrevious: () -> Unit,
    onSkipNext: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(
            onClick = onSkipPrevious,
            modifier = Modifier.size(56.dp)
        ) {
            Icon(
                imageVector = Icons.Default.SkipPrevious,
                contentDescription = "Vorheriger Song",
                modifier = Modifier.size(32.dp),
                tint = MaterialTheme.colorScheme.onSurface
            )
        }

        Spacer(modifier = Modifier.size(16.dp))

        FilledTonalButton(
            onClick = onPlayPause,
            modifier = Modifier.size(64.dp),
            shape = RoundedCornerShape(32.dp)
        ) {
            Icon(
                imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                contentDescription = if (isPlaying) "Pause" else "Abspielen",
                modifier = Modifier.size(32.dp)
            )
        }

        Spacer(modifier = Modifier.size(16.dp))

        IconButton(
            onClick = onSkipNext,
            modifier = Modifier.size(56.dp)
        ) {
            Icon(
                imageVector = Icons.Default.SkipNext,
                contentDescription = "Nächster Song",
                modifier = Modifier.size(32.dp),
                tint = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

@Composable
private fun ModeratorButton(
    moderator: Moderator,
    modifier: Modifier = Modifier
) {
    FilledTonalButton(
        onClick = { },
        modifier = modifier.fillMaxWidth()
    ) {
        Text(
            text = "Auf Sendung: ${moderator.name}",
            style = MaterialTheme.typography.titleMedium
        )
    }
}
