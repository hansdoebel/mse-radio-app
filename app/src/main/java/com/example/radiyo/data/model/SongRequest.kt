package com.example.radiyo.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class SongRequest(
    @SerialName("_id") val id: String,
    val userId: String,
    val songTitle: String,
    val artistName: String? = null,
    val status: SongRequestStatus,
    val processedAt: Double? = null
)

@Serializable
enum class SongRequestStatus {
    @SerialName("pending")
    PENDING,

    @SerialName("approved")
    APPROVED,

    @SerialName("rejected")
    REJECTED,

    @SerialName("played")
    PLAYED
}
