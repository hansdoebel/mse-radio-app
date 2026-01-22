package com.example.radiyo.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class Rating(
    @SerialName("_id") val id: String,
    val userId: String,
    val targetId: String,
    val targetType: RatingTargetType,
    val value: Double,
    val comment: String?
)

@Serializable
enum class RatingTargetType {
    @SerialName("playlist")
    PLAYLIST,

    @SerialName("moderator")
    MODERATOR,
}

@Serializable
data class RatingStats(
    val targetId: String = "",
    val averageRating: Double,
    val totalRatings: Double
)
