package com.appvoyager.cloudphotos.data.media.worker

import android.content.Context
import android.util.Log
import androidx.test.core.app.ApplicationProvider
import androidx.work.Configuration
import androidx.work.NetworkType
import androidx.work.WorkInfo
import androidx.work.WorkManager
import androidx.work.impl.WorkManagerImpl
import androidx.work.testing.WorkManagerTestInitHelper
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

class MediaSchedulerInstrumentedTest {

    private lateinit var context: Context
    private lateinit var workManager: WorkManager

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        WorkManagerTestInitHelper.initializeTestWorkManager(
            context,
            Configuration.Builder()
                .setMinimumLoggingLevel(Log.DEBUG)
                .build()
        )
        workManager = WorkManager.getInstance(context)
    }

    @After
    fun tearDown() {
        workManager.cancelAllWork().result.get()
    }

    @Test
    fun scheduleUpload_returnsConnectedUploadWork_whenNoExistingWork() {
        // Arrange
        val scheduler = UploadSchedulerImpl(context)

        // Act
        // Interaction: scheduler enqueues one upload WorkSpec with the production unique name
        scheduler.scheduleUpload()
        val actual = workSpecSnapshot(UploadMediaWorker.WORK_NAME)

        // Assert
        assertEquals(
            WorkSpecSnapshot(
                workCount = 1,
                state = WorkInfo.State.ENQUEUED,
                workerClassName = UploadMediaWorker::class.java.name,
                requiredNetworkType = NetworkType.CONNECTED,
                hasInputData = false
            ),
            actual
        )
    }

    @Test
    fun scheduleUpload_returnsSingleUploadWork_whenCalledTwice() {
        // Arrange
        val scheduler = UploadSchedulerImpl(context)

        // Act
        // StateTransition: ExistingWorkPolicy.KEEP prevents duplicate upload work
        scheduler.scheduleUpload()
        scheduler.scheduleUpload()
        val actual = workManager.getWorkInfosForUniqueWork(UploadMediaWorker.WORK_NAME).get().size

        // Assert
        assertEquals(1, actual)
    }

    @Test
    fun scheduleDelete_returnsConnectedDeleteWork_whenNoExistingWork() {
        // Arrange
        val scheduler = DeleteSchedulerImpl(context)

        // Act
        // Interaction: scheduler enqueues one delete WorkSpec with the production unique name
        scheduler.scheduleDelete()
        val actual = workSpecSnapshot(DeleteMediaWorker.WORK_NAME)

        // Assert
        assertEquals(
            WorkSpecSnapshot(
                workCount = 1,
                state = WorkInfo.State.ENQUEUED,
                workerClassName = DeleteMediaWorker::class.java.name,
                requiredNetworkType = NetworkType.CONNECTED,
                hasInputData = false
            ),
            actual
        )
    }

    @Test
    fun scheduleDelete_returnsSingleDeleteWork_whenCalledTwice() {
        // Arrange
        val scheduler = DeleteSchedulerImpl(context)

        // Act
        // StateTransition: ExistingWorkPolicy.KEEP prevents duplicate delete work
        scheduler.scheduleDelete()
        scheduler.scheduleDelete()
        val actual = workManager.getWorkInfosForUniqueWork(DeleteMediaWorker.WORK_NAME).get().size

        // Assert
        assertEquals(1, actual)
    }

    @Suppress("RestrictedApi")
    private fun workSpecSnapshot(uniqueWorkName: String): WorkSpecSnapshot {
        val workInfos = workManager.getWorkInfosForUniqueWork(uniqueWorkName).get()
        val workInfo = workInfos.single()
        val workSpec = WorkManagerImpl.getInstance(context)
            .workDatabase
            .workSpecDao()
            .getWorkSpec(workInfo.id.toString())
            ?: error("WorkSpec was not found for ${workInfo.id}")

        return WorkSpecSnapshot(
            workCount = workInfos.size,
            state = workInfo.state,
            workerClassName = workSpec.workerClassName,
            requiredNetworkType = workSpec.constraints.requiredNetworkType,
            hasInputData = workSpec.input.size() > 0
        )
    }

    private data class WorkSpecSnapshot(
        val workCount: Int,
        val state: WorkInfo.State,
        val workerClassName: String,
        val requiredNetworkType: NetworkType,
        val hasInputData: Boolean
    )
}
