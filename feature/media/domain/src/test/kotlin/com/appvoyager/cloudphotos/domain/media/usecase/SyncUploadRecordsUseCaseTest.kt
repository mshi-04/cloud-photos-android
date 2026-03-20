package com.appvoyager.cloudphotos.domain.media.usecase

import com.appvoyager.cloudphotos.domain.media.model.SyncStatus
import com.appvoyager.cloudphotos.domain.media.model.UploadRecord
import com.appvoyager.cloudphotos.domain.media.repository.LocalUploadRecordsRepository
import com.appvoyager.cloudphotos.domain.media.repository.RemoteUploadRecordsRepository
import com.appvoyager.cloudphotos.domain.media.valueobject.CloudStoragePath
import com.appvoyager.cloudphotos.domain.media.valueobject.IsDeleted
import com.appvoyager.cloudphotos.domain.media.valueobject.MediaId
import com.appvoyager.cloudphotos.domain.media.valueobject.MediaUploadedAt
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import io.mockk.slot
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

class SyncUploadRecordsUseCaseTest {

    private val remoteRepository = mockk<RemoteUploadRecordsRepository>()
    private val localRepository = mockk<LocalUploadRecordsRepository>()

    private lateinit var useCase: SyncUploadRecordsUseCase

    @BeforeEach
    fun setUp() {
        useCase = SyncUploadRecordsUseCase(remoteRepository, localRepository)
    }

    @Test
    fun `saves remote records to local repository when no pending records`() = runTest {
        // Arrange
        val remoteRecords = listOf(
            createUploadRecord("1"),
            createUploadRecord("2")
        )
        coEvery { remoteRepository.fetchUploadRecords() } returns remoteRecords
        coEvery { localRepository.getPendingRecordMediaIds() } returns emptySet()
        val slot = slot<List<UploadRecord>>()
        coEvery { localRepository.saveUploadRecords(capture(slot)) } just runs

        // Act
        useCase()

        // Assert
        assertEquals(remoteRecords, slot.captured)
    }

    @Test
    fun `saves empty list when remote returns no records`() = runTest {
        // Arrange
        coEvery { remoteRepository.fetchUploadRecords() } returns emptyList()
        coEvery { localRepository.getPendingRecordMediaIds() } returns emptySet()
        val slot = slot<List<UploadRecord>>()
        coEvery { localRepository.saveUploadRecords(capture(slot)) } just runs

        // Act
        useCase()

        // Assert
        assertEquals(emptyList<UploadRecord>(), slot.captured)
    }

    @Test
    fun `excludes remote records whose mediaId is pending locally`() = runTest {
        // Arrange
        val remoteRecords = listOf(
            createUploadRecord("1"),
            createUploadRecord("2"),
            createUploadRecord("3")
        )
        coEvery { remoteRepository.fetchUploadRecords() } returns remoteRecords
        coEvery { localRepository.getPendingRecordMediaIds() } returns setOf(
            MediaId.of("1"),
            MediaId.of("3")
        )
        val slot = slot<List<UploadRecord>>()
        coEvery { localRepository.saveUploadRecords(capture(slot)) } just runs

        // Act
        useCase()

        // Assert
        assertEquals(listOf(createUploadRecord("2")), slot.captured)
    }

    @Test
    fun `propagates exception from remote repository`() = runTest {
        // Arrange
        val expected = RuntimeException("network error")
        coEvery { remoteRepository.fetchUploadRecords() } throws expected

        // Act & Assert
        val actual = assertThrows<RuntimeException> {
            useCase()
        }
        assertEquals(expected, actual)
    }

    @Test
    fun `does not save to local when remote fetch fails`() = runTest {
        // Arrange
        coEvery { remoteRepository.fetchUploadRecords() } throws RuntimeException("network error")

        // Act
        try {
            useCase()
        } catch (_: RuntimeException) {
            // expected
        }

        // Assert
        coVerify(exactly = 0) { localRepository.saveUploadRecords(any()) }
    }

    private fun createUploadRecord(id: String): UploadRecord = UploadRecord(
        mediaId = MediaId.of(id),
        cloudStoragePath = CloudStoragePath.of("photos/$id.jpg"),
        isDeleted = IsDeleted.of(false),
        syncStatus = SyncStatus.SYNCED,
        mediaUploadedAt = MediaUploadedAt.of(1700000000000L)
    )
}
