package com.appvoyager.cloudphotos.data.media.util

import com.amplifyframework.storage.StorageException
import com.appvoyager.cloudphotos.domain.media.model.UploadError
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.io.FileNotFoundException
import java.io.IOException

class UploadErrorMapperTest {

    @Test
    fun `map returns FileNotFound for FileNotFoundException`() {
        // Arrange
        val throwable = FileNotFoundException("file not found")

        // Act
        val actual = UploadErrorMapper.map(throwable)

        // Assert
        assertEquals(UploadError.FileNotFound("file not found"), actual)
    }

    @Test
    fun `map returns Network for IOException`() {
        // Arrange
        val throwable = IOException("timeout")

        // Act
        val actual = UploadErrorMapper.map(throwable)

        // Assert
        assertEquals(UploadError.Network("timeout"), actual)
    }

    @Test
    fun `map returns AccessDenied for StorageException with access denied message`() {
        // Arrange
        val throwable = StorageException("Access Denied", "check permissions")

        // Act
        val actual = UploadErrorMapper.map(throwable)

        // Assert
        assertEquals(UploadError.AccessDenied("Access Denied"), actual)
    }

    @Test
    fun `map returns NotAuthenticated for StorageException with signed in message`() {
        // Arrange
        val throwable = StorageException("User is not signed in", "sign in first")

        // Act
        val actual = UploadErrorMapper.map(throwable)

        // Assert
        assertEquals(UploadError.NotAuthenticated("User is not signed in"), actual)
    }

    @Test
    fun `map returns NotAuthenticated for StorageException with unauthenticated message`() {
        // Arrange
        val throwable = StorageException("Unauthenticated access", "sign in first")

        // Act
        val actual = UploadErrorMapper.map(throwable)

        // Assert
        assertEquals(UploadError.NotAuthenticated("Unauthenticated access"), actual)
    }

    @Test
    fun `map returns StorageLimitExceeded for StorageException with limit message`() {
        // Arrange
        val throwable = StorageException("Storage limit exceeded", "upgrade plan")

        // Act
        val actual = UploadErrorMapper.map(throwable)

        // Assert
        assertEquals(UploadError.StorageLimitExceeded("Storage limit exceeded"), actual)
    }

    @Test
    fun `map returns StorageLimitExceeded for StorageException with quota message`() {
        // Arrange
        val throwable = StorageException("Quota exceeded", "upgrade plan")

        // Act
        val actual = UploadErrorMapper.map(throwable)

        // Assert
        assertEquals(UploadError.StorageLimitExceeded("Quota exceeded"), actual)
    }

    @Test
    fun `map returns Network for StorageException caused by IOException`() {
        // Arrange
        val throwable = StorageException("upload failed", IOException("connection reset"), "retry")

        // Act
        val actual = UploadErrorMapper.map(throwable)

        // Assert
        assertEquals(UploadError.Network("upload failed"), actual)
    }

    @Test
    fun `map returns Unknown for StorageException with unmapped message`() {
        // Arrange
        val throwable = StorageException("something went wrong", "retry")

        // Act
        val actual = UploadErrorMapper.map(throwable)

        // Assert
        assertEquals(UploadError.Unknown("something went wrong"), actual)
    }

    @Test
    fun `map returns Unknown for unexpected throwable`() {
        // Arrange
        val throwable = IllegalArgumentException("unexpected")

        // Act
        val actual = UploadErrorMapper.map(throwable)

        // Assert
        assertEquals(UploadError.Unknown("unexpected"), actual)
    }
}
