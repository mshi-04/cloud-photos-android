package com.appvoyager.cloudphotos.data.media.db.dao

import com.appvoyager.cloudphotos.domain.media.model.SyncStatus
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class UploadRecordDaoSyncStatusTest {

    // Names hard-coded in the SQL inside UploadRecordDao.getPendingMediaIds.
    private val hardCodedNames = setOf("PENDING_UPLOAD", "PENDING_DELETE")

    @Test
    fun `pending upload status exists`() {
        val enumNames = SyncStatus.entries.map { it.name }
        val name = hardCodedNames.first { it == "PENDING_UPLOAD" }
        assertTrue(name in enumNames) {
            "'$name' is hard-coded in UploadRecordDao.getPendingMediaIds but does not exist in SyncStatus. " +
                "Update the SQL query to match the renamed enum entry."
        }
    }

    @Test
    fun `pending delete status exists`() {
        val enumNames = SyncStatus.entries.map { it.name }
        val name = hardCodedNames.first { it == "PENDING_DELETE" }
        assertTrue(name in enumNames) {
            "'$name' is hard-coded in UploadRecordDao.getPendingMediaIds but does not exist in SyncStatus. " +
                "Update the SQL query to match the renamed enum entry."
        }
    }
}
