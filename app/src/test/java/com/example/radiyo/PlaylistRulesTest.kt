package com.example.radiyo

import com.example.radiyo.data.repository.PlaylistWithId
import com.example.radiyo.data.repository.sanitizePlaylists
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PlaylistRulesTest {

    @Test
    fun `name must not be empty`() {
        val input = listOf(
            PlaylistWithId(id = "1", name = "   "),
            PlaylistWithId(id = "2", name = "Rock")
        )

        val out = sanitizePlaylists(input)

        assertEquals(1, out.size)
        assertEquals("Rock", out.first().name)
    }

    @Test
    fun `duplicates are removed by name ignoring case and whitespace`() {
        val input = listOf(
            PlaylistWithId(id = "1", name = "Rock"),
            PlaylistWithId(id = "2", name = " rock "),
            PlaylistWithId(id = "3", name = "Pop")
        )

        val out = sanitizePlaylists(input)

        assertEquals(2, out.size)
        assertTrue(out.any { it.name == "Rock" })
        assertTrue(out.any { it.name == "Pop" })
    }
}
