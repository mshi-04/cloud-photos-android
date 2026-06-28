package com.appvoyager.cloudphotos.data.media.db.dao

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.appvoyager.cloudphotos.data.media.db.CloudPhotosDatabase
import com.appvoyager.cloudphotos.data.media.db.entity.UploadRecordEntity
import com.appvoyager.cloudphotos.domain.media.model.SyncStatus
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

class UploadRecordDaoInstrumentedTest {

    private lateinit var database: CloudPhotosDatabase
    private lateinit var dao: UploadRecordDao

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(context, CloudPhotosDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        dao = database.uploadRecordDao()
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun upsertAll_returnsStoredRecords_whenDatabaseIsEmpty() = runTest {
        // Arrange
        val records = listOf(
            createRecord("media-1", SyncStatus.PENDING_UPLOAD),
            createRecord("media-2", SyncStatus.SYNCED)
        )

        // Act
        // Normal: Room in-memory database persists inserted upload records
        dao.upsertAll(records)
        val actual = dao.getByMediaIds(listOf("media-1", "media-2"))

        // Assert
        assertEquals(records.sortedBy { it.mediaId }, actual.sortedBy { it.mediaId })
    }

    @Test
    fun upsertAll_setsReplacementRow_whenPrimaryKeyAlreadyExists() = runTest {
        // Arrange
        dao.upsertAll(listOf(createRecord("media-1", SyncStatus.PENDING_UPLOAD)))
        val updated = createRecord(
            mediaId = "media-1",
            syncStatus = SyncStatus.SYNCED,
            cloudStoragePath = "private/identity/media-1.jpg",
            uploadedAt = 1700000001000L
        )

        // Act
        // StateTransition: upsert updates the existing row with the same mediaId primary key
        dao.upsertAll(listOf(updated))
        val actual = dao.getByMediaIds(listOf("media-1")).single()

        // Assert
        assertEquals(updated, actual)
    }

    @Test
    fun getPendingMediaIds_returnsPendingMediaIds_whenStatusesAreMixed() = runTest {
        // Arrange
        dao.upsertAll(
            listOf(
                createRecord("pending-upload", SyncStatus.PENDING_UPLOAD),
                createRecord("pending-delete", SyncStatus.PENDING_DELETE),
                createRecord("synced", SyncStatus.SYNCED),
                createRecord("error", SyncStatus.ERROR)
            )
        )

        // Act
        // Boundary: pending query uses the hard-coded Room SQL status names
        val actual = dao.getPendingMediaIds().toSet()

        // Assert
        assertEquals(setOf("pending-upload", "pending-delete"), actual)
    }

    @Test
    fun getByStatus_returnsMatchingRows_whenStatusesAreMixed() = runTest {
        // Arrange
        val expected = createRecord("media-1", SyncStatus.PENDING_DELETE)
        dao.upsertAll(
            listOf(
                expected,
                createRecord("media-2", SyncStatus.PENDING_UPLOAD),
                createRecord("media-3", SyncStatus.SYNCED)
            )
        )

        // Act
        // Boundary: status query returns rows matching exactly one status string
        val actual = dao.getByStatus(SyncStatus.PENDING_DELETE.name)

        // Assert
        assertEquals(listOf(expected), actual)
    }

    @Test
    fun deleteByMediaId_returnsRemainingRows_whenMultipleRowsExist() = runTest {
        // Arrange
        val remaining = createRecord("media-2", SyncStatus.PENDING_UPLOAD)
        dao.upsertAll(
            listOf(
                createRecord("media-1", SyncStatus.PENDING_UPLOAD),
                remaining
            )
        )

        // Act
        // StateTransition: delete removes only the row matching the requested mediaId
        dao.deleteByMediaId("media-1")
        val actual = dao.getByMediaIds(listOf("media-1", "media-2"))

        // Assert
        assertEquals(listOf(remaining), actual)
    }

    @Test
    fun getByMediaIds_returnsEmptyList_whenMediaIdsAreEmpty() = runTest {
        // Arrange
        dao.upsertAll(listOf(createRecord("media-1", SyncStatus.PENDING_UPLOAD)))

        // Act
        // Boundary: an empty IN query returns no rows and does not fail on SQLite
        val actual = dao.getByMediaIds(emptyList())

        // Assert
        assertEquals(emptyList<UploadRecordEntity>(), actual)
    }

    @Test
    fun getByMediaIds_returnsEmptyList_whenMediaIdDoesNotExist() = runTest {
        // Arrange
        dao.upsertAll(listOf(createRecord("media-1", SyncStatus.PENDING_UPLOAD)))

        // Act
        // Boundary: a missing mediaId returns an empty result instead of a placeholder row
        val actual = dao.getByMediaIds(listOf("missing"))

        // Assert
        assertEquals(emptyList<UploadRecordEntity>(), actual)
    }

    @Test
    fun deleteByMediaId_returnsOriginalRows_whenMediaIdDoesNotExist() = runTest {
        // Arrange
        val existing = createRecord("media-1", SyncStatus.PENDING_UPLOAD)
        dao.upsertAll(listOf(existing))

        // Act
        // StateTransition: deleting a missing mediaId is idempotent for existing rows
        dao.deleteByMediaId("missing")
        val actual = dao.getByMediaIds(listOf("media-1"))

        // Assert
        assertEquals(listOf(existing), actual)
    }

    private fun createRecord(
        mediaId: String,
        syncStatus: SyncStatus,
        cloudStoragePath: String? = null,
        uploadedAt: Long = 1700000000000L
    ) = UploadRecordEntity(
        mediaId = mediaId,
        cloudStoragePath = cloudStoragePath,
        isDeleted = syncStatus == SyncStatus.PENDING_DELETE,
        syncStatus = syncStatus.name,
        uploadedAt = uploadedAt
    )
}
