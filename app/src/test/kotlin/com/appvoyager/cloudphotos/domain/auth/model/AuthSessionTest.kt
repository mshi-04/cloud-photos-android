package com.appvoyager.cloudphotos.domain.auth.model

import com.appvoyager.cloudphotos.domain.auth.testutil.guestSession
import com.appvoyager.cloudphotos.domain.auth.testutil.signedInSession
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class AuthSessionTest {

    @Test
    fun `isSignedIn true and isGuest false when state is SignedIn`() {
        // Arrange
        val session = signedInSession()

        // Act & Assert
        assertTrue(session.isSignedIn)
        assertFalse(session.isGuest)
    }

    @Test
    fun `isGuest true and isSignedIn false when state is Guest`() {
        // Arrange
        val session = guestSession()

        // Act & Assert
        assertTrue(session.isGuest)
        assertFalse(session.isSignedIn)
    }
}
