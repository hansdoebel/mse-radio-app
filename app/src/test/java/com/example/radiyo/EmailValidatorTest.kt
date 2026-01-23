package com.example.radiyo

import com.example.radiyo.data.repository.isValidEmail
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class EmailValidatorTest {

    @Test
    fun `test@example com is valid`() {
        assertTrue(isValidEmail("test@example.com"))
    }

    @Test
    fun `test@ is invalid`() {
        assertFalse(isValidEmail("test@"))
    }

    @Test
    fun `trims whitespace`() {
        assertTrue(isValidEmail(" test@example.com "))
    }
}
