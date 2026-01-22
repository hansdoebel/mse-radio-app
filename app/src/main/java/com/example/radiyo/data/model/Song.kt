package com.example.radiyo.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class Song(
    @SerialName("_id") val id: String,
    val title: String,
    val artist: String,
    val album: String,
    val durationMs: Double = 0.0
)

@Serializable
data class Playlist(
    @SerialName("_id") val id: String,
    val name: String,
    val description: String? = null
)

@Serializable
data class NowPlaying(
    val song: Song,
    val playlist: Playlist?,
    val startedAt: Double,
    val isPlaying: Boolean = true,
    val pausedAt: Double? = null,
    val moderator: Moderator?
)

@Serializable
data class Moderator(
    @SerialName("_id") val id: String,
    val name: String
)
