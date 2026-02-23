package com.appvoyager.cloudphotos.domain.auth.model

import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class AuthSessionTest {

    @Test
    fun `isSignedIn true and isGuest false when state is SignedIn`() {
        // Arrange
        val session = AuthSession(AuthState.SignedIn)

        // Act & Assert
        assertTrue(session.isSignedIn)
        assertFalse(session.isGuest)
    }

    @Test
    fun `isGuest true and isSignedIn false when state is Guest`() {
        // Arrange
        val session = AuthSession(AuthState.Guest)

        // Act & Assert
        assertTrue(session.isGuest)
        assertFalse(session.isSignedIn)
    }
}
