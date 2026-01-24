package com.example.radiyo

import com.example.radiyo.data.model.InAppNotification
import com.example.radiyo.data.model.RatingTargetType
import org.junit.Assert.assertEquals
import org.junit.Test

class InAppNotificationTest {

    @Test
    fun `NewSongRequest message without artist`() {
        val n = InAppNotification.NewSongRequest(
            id = "1",
            userName = "Max",
            songTitle = "Song X",
            artistName = null
        )

        assertEquals("Neue Songanfrage", n.title)
        assertEquals("Max: \"Song X\"", n.message)
    }

    @Test
    fun `NewSongRequest message with artist`() {
        val n = InAppNotification.NewSongRequest(
            id = "1",
            userName = "Max",
            songTitle = "Song X",
            artistName = "Artist Y"
        )

        assertEquals("Max: \"Song X\" von Artist Y", n.message)
    }

    @Test
    fun `NewRating title depends on targetType`() {
        val modRating = InAppNotification.NewRating(
            id = "1",
            userName = "Lisa",
            targetType = RatingTargetType.MODERATOR,
            targetName = "DJ Tom",
            value = 4.7
        )
        val playlistRating = InAppNotification.NewRating(
            id = "2",
            userName = "Lisa",
            targetType = RatingTargetType.PLAYLIST,
            targetName = "Rock",
            value = 3.2
        )

        assertEquals("Neue Moderator-Bewertung", modRating.title)
        assertEquals("Neue Playlist-Bewertung", playlistRating.title)
    }

    @Test
    fun `NewRating message omits targetName when blank or Unbekannt`() {
        val blank = InAppNotification.NewRating(
            id = "1",
            userName = "Lisa",
            targetType = RatingTargetType.PLAYLIST,
            targetName = "",
            value = 4.0
        )
        val unknown = InAppNotification.NewRating(
            id = "2",
            userName = "Lisa",
            targetType = RatingTargetType.PLAYLIST,
            targetName = "Unbekannt",
            value = 4.0
        )

        assertEquals("Lisa: 4/5 Sterne", blank.message)
        assertEquals("Lisa: 4/5 Sterne", unknown.message)
    }
}
