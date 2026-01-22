package com.example.radiyo.data.model

sealed class InAppNotification(
    val id: String,
    val title: String,
    val message: String
) {
    class NewSongRequest(
        id: String,
        val userName: String,
        val songTitle: String,
        val artistName: String?
    ) : InAppNotification(
        id = id,
        title = "Neue Songanfrage",
        message = "$userName: \"$songTitle\"${artistName?.let { " von $it" } ?: ""}"
    )

    class NewRating(
        id: String,
        val userName: String,
        val targetType: RatingTargetType,
        val targetName: String,
        val value: Double
    ) : InAppNotification(
        id = id,
        title = if (targetType == RatingTargetType.MODERATOR) "Neue Moderator-Bewertung" else "Neue Playlist-Bewertung",
        message = "$userName: ${value.toInt()}/5 Sterne${if (targetName.isNotBlank() && targetName != "Unbekannt") " für $targetName" else ""}"
    )
}
