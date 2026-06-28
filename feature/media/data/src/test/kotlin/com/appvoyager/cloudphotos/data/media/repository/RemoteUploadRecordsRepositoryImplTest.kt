package com.appvoyager.cloudphotos.data.media.repository

import com.appvoyager.cloudphotos.data.media.datasource.UploadRecordRemoteDataSource
import com.appvoyager.cloudphotos.domain.media.model.MediaType
import com.appvoyager.cloudphotos.domain.media.model.SyncStatus
import com.appvoyager.cloudphotos.domain.media.model.UploadRecord
import com.appvoyager.cloudphotos.domain.media.request.CreateUploadRecordRequest
import com.appvoyager.cloudphotos.domain.media.valueobject.CloudStoragePath
import com.appvoyager.cloudphotos.domain.media.valueobject.ContentType
import com.appvoyager.cloudphotos.domain.media.valueobject.IsDeleted
import com.appvoyager.cloudphotos.domain.media.valueobject.MediaId
import com.appvoyager.cloudphotos.domain.media.valueobject.MediaUploadedAt
import com.appvoyager.cloudphotos.domain.media.valueobject.UploadSuccessCount
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import io.mockk.slot
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class RemoteUploadRecordsRepositoryImplTest {

    private val remoteDataSource = mockk<UploadRecordRemoteDataSource>()
    private val repository = RemoteUploadRecordsRepositoryImpl(remoteDataSource)

    @Test
    fun `fetchUploadRecords returns data source records when remote has records`() = runTest {
        // Arrange
        val expected = listOf(createUploadRecord("media-1"), createUploadRecord("media-2"))
        coEvery { remoteDataSource.fetchUploadRecords() } returns expected

        // Act
        // Normal: repository preserves remote upload records
        val actual = repository.fetchUploadRecords()

        // Assert
        assertEquals(expected, actual)
    }

    @Test
    fun `fetchUploadRecords returns empty list when remote has no records`() = runTest {
        // Arrange
        coEvery { remoteDataSource.fetchUploadRecords() } returns emptyList()

        // Act
        // Boundary: empty remote response remains empty
        val actual = repository.fetchUploadRecords()

        // Assert
        assertEquals(emptyList<UploadRecord>(), actual)
    }

    @Test
    fun `createUploadRecord calls data source with request and returns created record`() = runTest {
        // Arrange
        val request = createRequest("media-1")
        val requestSlot = slot<CreateUploadRecordRequest>()
        val expected = createUploadRecord("media-1").copy(syncStatus = SyncStatus.SYNCED)
        coEvery { remoteDataSource.createUploadRecord(capture(requestSlot)) } returns expected

        // Act
        // Interaction: create delegates exact request and returns provider result
        val actual = repository.createUploadRecord(request)

        // Assert
        assertEquals(
            CreateUploadRecordSnapshot(request = request, result = expected),
            CreateUploadRecordSnapshot(request = requestSlot.captured, result = actual)
        )
    }

    @Test
    fun `completeUpload calls data source with success count`() = runTest {
        // Arrange
        val successCount = UploadSuccessCount.of(2)
        coEvery { remoteDataSource.completeUpload(successCount) } just runs

        // Act
        // Interaction: complete delegates the exact success count
        repository.completeUpload(successCount)

        // Assert
        coVerify(exactly = 1) { remoteDataSource.completeUpload(successCount) }
    }

    @Test
    fun `deleteUploadRecord calls data source with media id`() = runTest {
        // Arrange
        val mediaId = MediaId.of("media-1")
        coEvery { remoteDataSource.deleteUploadRecord(mediaId) } just runs

        // Act
        // Interaction: delete delegates the exact media id
        repository.deleteUploadRecord(mediaId)

        // Assert
        coVerify(exactly = 1) { remoteDataSource.deleteUploadRecord(mediaId) }
    }

    private fun createRequest(mediaId: String) = CreateUploadRecordRequest(
        mediaId = MediaId.of(mediaId),
        cloudStoragePath = CloudStoragePath.of("private/identity123/$mediaId.jpg"),
        contentType = ContentType.of("image/jpeg"),
        mediaType = MediaType.IMAGE
    )

    private fun createUploadRecord(mediaId: String) = UploadRecord(
        mediaId = MediaId.of(mediaId),
        cloudStoragePath = CloudStoragePath.of("private/identity123/$mediaId.jpg"),
        isDeleted = IsDeleted.of(false),
        syncStatus = SyncStatus.PENDING_UPLOAD,
        mediaUploadedAt = MediaUploadedAt.of(1700000000000L)
    )

    private data class CreateUploadRecordSnapshot(val request: CreateUploadRecordRequest, val result: UploadRecord)
}
