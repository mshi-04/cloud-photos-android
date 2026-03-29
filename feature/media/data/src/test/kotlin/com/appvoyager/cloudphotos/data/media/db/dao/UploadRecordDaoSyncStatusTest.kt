package com.appvoyager.cloudphotos.data.media.db.dao

import com.appvoyager.cloudphotos.domain.media.model.SyncStatus
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class UploadRecordDaoSyncStatusTest {

    @Test
    fun `SyncStatus entries returns PENDING_UPLOAD entry when enum is defined`() {
        val enumNames = SyncStatus.entries.map { it.name }
        val name = "PENDING_UPLOAD"
        assertTrue(name in enumNames) {
            "'$name' is hard-coded in UploadRecordDao.getPendingMediaIds but does not exist in SyncStatus. " +
                "Update the SQL query to match the renamed enum entry."
        }
    }

    @Test
    fun `SyncStatus entries returns PENDING_DELETE entry when enum is defined`() {
        val enumNames = SyncStatus.entries.map { it.name }
        val name = "PENDING_DELETE"
        assertTrue(name in enumNames) {
            "'$name' is hard-coded in UploadRecordDao.getPendingMediaIds but does not exist in SyncStatus. " +
                "Update the SQL query to match the renamed enum entry."
        }
    }
}
