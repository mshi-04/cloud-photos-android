package com.appvoyager.cloudphotos.data.media.util

import com.appvoyager.cloudphotos.domain.media.model.MediaType
import com.appvoyager.cloudphotos.domain.media.model.SyncStatus
import com.appvoyager.cloudphotos.domain.media.request.CreateUploadRecordRequest
import com.appvoyager.cloudphotos.domain.media.valueobject.CloudStoragePath
import com.appvoyager.cloudphotos.domain.media.valueobject.ContentType
import com.appvoyager.cloudphotos.domain.media.valueobject.IsDeleted
import com.appvoyager.cloudphotos.domain.media.valueobject.MediaId
import com.appvoyager.cloudphotos.domain.media.valueobject.MediaUploadedAt
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class RemoteUploadRecordMapperTest {

    private val request = CreateUploadRecordRequest(
        mediaId = MediaId.of("external_primary_123"),
        cloudStoragePath = CloudStoragePath.of("private/identity123/uuid.jpg"),
        contentType = ContentType.of("image/jpeg"),
        mediaType = MediaType.IMAGE
    )

    @Test
    fun `fromCreateResponse returns record with uploadedAt when uploadedAt is not null`() {
        // Arrange & Act
        val result = RemoteUploadRecordMapper.fromCreateResponse(
            uploadedAt = 1_700_000_000_000L,
            request = request,
            fallbackUploadedAt = 9999L
        )

        // Assert
        assertEquals(MediaUploadedAt.of(1_700_000_000_000L), result.mediaUploadedAt)
    }

    @Test
    fun `fromCreateResponse returns record with fallbackUploadedAt when uploadedAt is null`() {
        // Arrange & Act
        val result = RemoteUploadRecordMapper.fromCreateResponse(
            uploadedAt = null,
            request = request,
            fallbackUploadedAt = 1_700_000_000_000L
        )

        // Assert
        assertEquals(MediaUploadedAt.of(1_700_000_000_000L), result.mediaUploadedAt)
    }

    @Test
    fun `fromCreateResponse sets mediaId from request when invoked`() {
        // Arrange & Act
        val result = RemoteUploadRecordMapper.fromCreateResponse(
            uploadedAt = 1_700_000_000_000L,
            request = request,
            fallbackUploadedAt = 0L
        )

        // Assert
        assertEquals(request.mediaId, result.mediaId)
    }

    @Test
    fun `fromCreateResponse sets cloudStoragePath from request when invoked`() {
        // Arrange & Act
        val result = RemoteUploadRecordMapper.fromCreateResponse(
            uploadedAt = 1_700_000_000_000L,
            request = request,
            fallbackUploadedAt = 0L
        )

        // Assert
        assertEquals(request.cloudStoragePath, result.cloudStoragePath)
    }

    @Test
    fun `fromCreateResponse sets syncStatus to SYNCED when invoked`() {
        // Arrange & Act
        val result = RemoteUploadRecordMapper.fromCreateResponse(
            uploadedAt = 1_700_000_000_000L,
            request = request,
            fallbackUploadedAt = 0L
        )

        // Assert
        assertEquals(SyncStatus.SYNCED, result.syncStatus)
    }

    @Test
    fun `fromCreateResponse sets isDeleted to false when invoked`() {
        // Arrange & Act
        val result = RemoteUploadRecordMapper.fromCreateResponse(
            uploadedAt = 1_700_000_000_000L,
            request = request,
            fallbackUploadedAt = 0L
        )

        // Assert
        assertEquals(IsDeleted.of(false), result.isDeleted)
    }
}
