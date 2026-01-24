package com.example.radiyo

import com.example.radiyo.data.repository.isRatingValid
import com.example.radiyo.data.repository.normalizeComment
import org.junit.Assert.*
import org.junit.Test

class RatingRulesTest {

    @Test
    fun `rating value must be within 1 to 5`() {
        assertFalse(isRatingValid(0))
        assertTrue(isRatingValid(1))
        assertTrue(isRatingValid(5))
        assertFalse(isRatingValid(6))
    }

    @Test
    fun `comment is trimmed and empty becomes null`() {
        assertEquals("nice", normalizeComment("  nice  "))
        assertNull(normalizeComment("   "))
        assertNull(normalizeComment(""))
        assertNull(normalizeComment(null))
    }
}
