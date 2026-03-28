package com.appvoyager.cloudphotos.data.media.util

import com.amplifyframework.storage.StorageException
import com.appvoyager.cloudphotos.domain.media.model.UploadError
import java.io.FileNotFoundException
import java.io.IOException
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class UploadErrorMapperTest {

    @Test
    fun `map returns FileNotFound when throwable is FileNotFoundException`() {
        // Arrange
        val throwable = FileNotFoundException("file not found")

        // Act
        val actual = UploadErrorMapper.map(throwable)

        // Assert
        assertEquals(UploadError.FileNotFound("file not found"), actual)
    }

    @Test
    fun `map returns Network when throwable is IOException`() {
        // Arrange
        val throwable = IOException("timeout")

        // Act
        val actual = UploadErrorMapper.map(throwable)

        // Assert
        assertEquals(UploadError.Network("timeout"), actual)
    }

    @Test
    fun `map returns AccessDenied when StorageException message contains access denied`() {
        // Arrange
        val throwable = StorageException("Access Denied", "check permissions")

        // Act
        val actual = UploadErrorMapper.map(throwable)

        // Assert
        assertEquals(UploadError.AccessDenied("Access Denied"), actual)
    }

    @Test
    fun `map returns NotAuthenticated when StorageException message contains signed in`() {
        // Arrange
        val throwable = StorageException("User is not signed in", "sign in first")

        // Act
        val actual = UploadErrorMapper.map(throwable)

        // Assert
        assertEquals(UploadError.NotAuthenticated("User is not signed in"), actual)
    }

    @Test
    fun `map returns NotAuthenticated when StorageException message contains Unauthenticated`() {
        // Arrange
        val throwable = StorageException("Unauthenticated access", "sign in first")

        // Act
        val actual = UploadErrorMapper.map(throwable)

        // Assert
        assertEquals(UploadError.NotAuthenticated("Unauthenticated access"), actual)
    }

    @Test
    fun `map returns StorageLimitExceeded when StorageException message contains limit`() {
        // Arrange
        val throwable = StorageException("Storage limit exceeded", "upgrade plan")

        // Act
        val actual = UploadErrorMapper.map(throwable)

        // Assert
        assertEquals(UploadError.StorageLimitExceeded("Storage limit exceeded"), actual)
    }

    @Test
    fun `map returns StorageLimitExceeded when StorageException message contains quota`() {
        // Arrange
        val throwable = StorageException("Quota exceeded", "upgrade plan")

        // Act
        val actual = UploadErrorMapper.map(throwable)

        // Assert
        assertEquals(UploadError.StorageLimitExceeded("Quota exceeded"), actual)
    }

    @Test
    fun `map returns Network when StorageException is caused by IOException`() {
        // Arrange
        val throwable = StorageException("upload failed", IOException("connection reset"), "retry")

        // Act
        val actual = UploadErrorMapper.map(throwable)

        // Assert
        assertEquals(UploadError.Network("upload failed"), actual)
    }

    @Test
    fun `map returns Unknown when StorageException message is not mapped`() {
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
