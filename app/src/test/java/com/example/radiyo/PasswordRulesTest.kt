package com.example.radiyo

import com.example.radiyo.data.repository.isPasswordValid
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PasswordRulesTest {

    @Test
    fun `min length enforced`() {
        assertTrue(isPasswordValid("12345678", minLength = 8))
        assertFalse(isPasswordValid("1234567", minLength = 8))
    }

    @Test
    fun `empty is error`() {
        assertFalse(isPasswordValid(""))
        assertFalse(isPasswordValid("   "))
    }
}
