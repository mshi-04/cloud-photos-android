package com.appvoyager.cloudphotos.data.media.repository

import com.appvoyager.cloudphotos.data.media.datasource.UploadRecordLocalDataSource
import com.appvoyager.cloudphotos.domain.media.model.SyncStatus
import com.appvoyager.cloudphotos.domain.media.model.UploadRecord
import com.appvoyager.cloudphotos.domain.media.valueobject.CloudStoragePath
import com.appvoyager.cloudphotos.domain.media.valueobject.IsDeleted
import com.appvoyager.cloudphotos.domain.media.valueobject.MediaId
import com.appvoyager.cloudphotos.domain.media.valueobject.MediaUploadedAt
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class LocalUploadRecordsRepositoryImplTest {

    private val localDataSource = mockk<UploadRecordLocalDataSource>()
    private val repository = LocalUploadRecordsRepositoryImpl(localDataSource)

    @Test
    fun `getUploadRecords returns data source records when ids are supplied`() = runTest {
        // Arrange
        val mediaIds = listOf(MediaId.of("media-1"), MediaId.of("media-2"))
        val expected = listOf(createUploadRecord("media-1"))
        coEvery { localDataSource.getUploadRecords(mediaIds) } returns expected

        // Act
        // Normal: repository preserves data source upload records
        val actual = repository.getUploadRecords(mediaIds)

        // Assert
        assertEquals(expected, actual)
    }

    @Test
    fun `getUploadRecords calls data source with empty id list when ids are empty`() = runTest {
        // Arrange
        coEvery { localDataSource.getUploadRecords(emptyList()) } returns emptyList()

        // Act
        // Boundary/Interaction: empty media id list is delegated without synthetic records
        repository.getUploadRecords(emptyList())

        // Assert
        coVerify(exactly = 1) { localDataSource.getUploadRecords(emptyList()) }
    }

    @Test
    fun `saveUploadRecords calls data source with records when records are supplied`() = runTest {
        // Arrange
        val records = listOf(createUploadRecord("media-1"), createUploadRecord("media-2"))
        coEvery { localDataSource.saveUploadRecords(records) } just runs

        // Act
        // Interaction: save uses the same record collection without filtering duplicates
        repository.saveUploadRecords(records)

        // Assert
        coVerify(exactly = 1) { localDataSource.saveUploadRecords(records) }
    }

    @Test
    fun `deleteUploadRecord calls data source with media id`() = runTest {
        // Arrange
        val mediaId = MediaId.of("media-1")
        coEvery { localDataSource.deleteUploadRecord(mediaId) } just runs

        // Act
        // Interaction: delete delegates the exact media id to persistence
        repository.deleteUploadRecord(mediaId)

        // Assert
        coVerify(exactly = 1) { localDataSource.deleteUploadRecord(mediaId) }
    }

    private fun createUploadRecord(mediaId: String) = UploadRecord(
        mediaId = MediaId.of(mediaId),
        cloudStoragePath = CloudStoragePath.of("private/identity123/$mediaId.jpg"),
        isDeleted = IsDeleted.of(false),
        syncStatus = SyncStatus.PENDING_UPLOAD,
        mediaUploadedAt = MediaUploadedAt.of(1700000000000L)
    )
}
