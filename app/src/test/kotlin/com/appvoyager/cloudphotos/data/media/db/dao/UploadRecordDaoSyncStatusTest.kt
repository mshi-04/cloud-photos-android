package com.appvoyager.cloudphotos.data.media.db.dao

import com.appvoyager.cloudphotos.domain.media.model.SyncStatus
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class UploadRecordDaoSyncStatusTest {

    // Names hard-coded in the SQL inside UploadRecordDao.getPendingMediaIds.
    private val hardCodedNames = setOf("PENDING_UPLOAD", "PENDING_DELETE")

    @Test
    fun `getPendingMediaIds hard-coded status names exist in SyncStatus enum`() {
        val enumNames = SyncStatus.entries.map { it.name }.toSet()
        hardCodedNames.forEach { name ->
            assertTrue(name in enumNames) {
                "'$name' is hard-coded in UploadRecordDao.getPendingMediaIds but does not exist in SyncStatus. " +
                    "Update the SQL query to match the renamed enum entry."
            }
        }
    }
}
