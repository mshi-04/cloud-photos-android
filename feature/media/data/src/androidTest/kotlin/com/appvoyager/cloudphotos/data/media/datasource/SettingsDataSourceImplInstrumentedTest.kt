package com.appvoyager.cloudphotos.data.media.datasource

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStoreFile
import androidx.test.core.app.ApplicationProvider
import java.io.File
import java.util.UUID
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

class SettingsDataSourceImplInstrumentedTest {

    private lateinit var dataStoreFile: File
    private lateinit var dataStoreScope: CoroutineScope
    private lateinit var dataSource: SettingsDataSourceImpl

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        dataStoreFile = context.preferencesDataStoreFile("settings-test-${UUID.randomUUID()}")
        dataStoreScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
        val dataStore: DataStore<Preferences> = PreferenceDataStoreFactory.create(
            scope = dataStoreScope,
            produceFile = { dataStoreFile }
        )
        dataSource = SettingsDataSourceImpl(dataStore)
    }

    @After
    fun tearDown() {
        dataStoreScope.cancel()
        dataStoreFile.delete()
    }

    @Test
    fun gridColumnCount_returnsDefaultCount_whenPreferenceIsUnset() = runTest {
        // Arrange

        // Act
        // Boundary: an unset preference falls back to the app default
        val actual = dataSource.gridColumnCount.first()

        // Assert
        assertEquals(3, actual)
    }

    @Test
    fun setGridColumnCount_setsStoredCount_whenCountIsPositive() = runTest {
        // Arrange

        // Act
        // Normal: a positive count is persisted through the real Preferences DataStore
        dataSource.setGridColumnCount(5)
        val actual = dataSource.gridColumnCount.first()

        // Assert
        assertEquals(5, actual)
    }

    @Test
    fun setGridColumnCount_returnsPreviousCount_whenCountIsZero() = runTest {
        // Arrange
        dataSource.setGridColumnCount(4)

        // Act
        // Boundary: zero is ignored and does not overwrite the previous valid count
        dataSource.setGridColumnCount(0)
        val actual = dataSource.gridColumnCount.first()

        // Assert
        assertEquals(4, actual)
    }

    @Test
    fun setGridColumnCount_returnsPreviousCount_whenCountIsNegative() = runTest {
        // Arrange
        dataSource.setGridColumnCount(4)

        // Act
        // Boundary: negative count is ignored and does not overwrite the previous valid count
        dataSource.setGridColumnCount(-1)
        val actual = dataSource.gridColumnCount.first()

        // Assert
        assertEquals(4, actual)
    }

    @Test
    fun setGridColumnCount_returnsLatestCount_whenCalledMultipleTimes() = runTest {
        // Arrange
        dataSource.setGridColumnCount(2)

        // Act
        // StateTransition: repeated valid updates keep the latest persisted count
        dataSource.setGridColumnCount(6)
        val actual = dataSource.gridColumnCount.first()

        // Assert
        assertEquals(6, actual)
    }
}
