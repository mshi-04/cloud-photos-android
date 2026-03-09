package com.appvoyager.cloudphotos.data.media.datasource

import android.content.ContentResolver
import android.content.ContentUris
import android.content.Context
import android.database.Cursor
import android.net.Uri
import android.provider.MediaStore
import com.appvoyager.cloudphotos.domain.media.model.MediaType
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.unmockkAll
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

class LocalMediaDataSourceImplTest {

    private lateinit var mockContext: Context
    private lateinit var mockContentResolver: ContentResolver
    private lateinit var mockCursor: Cursor
    private lateinit var dataSource: LocalMediaDataSourceImpl

    @BeforeEach
    fun setup() {
        mockContext = mockk()
        mockContentResolver = mockk()
        mockCursor = mockk(relaxed = true)

        every { mockContext.contentResolver } returns mockContentResolver

        mockkStatic(Uri::class)
        mockkStatic(ContentUris::class)
        mockkStatic(MediaStore::class)
        mockkStatic(MediaStore.Files::class)

        val mockVolumeUri = mockk<Uri>(relaxed = true)
        every { MediaStore.Files.getContentUri(any<String>()) } returns mockVolumeUri
        every { MediaStore.getExternalVolumeNames(any()) } returns setOf("external_primary")

        val mockImageUri = mockk<Uri>(relaxed = true)
        every { mockImageUri.toString() } returns "content://media/external_primary/file/123"

        val mockVideoUri = mockk<Uri>(relaxed = true)
        every { mockVideoUri.toString() } returns "content://media/external_primary/file/456"

        every { ContentUris.withAppendedId(any(), 123L) } returns mockImageUri
        every { ContentUris.withAppendedId(any(), 456L) } returns mockVideoUri

        dataSource = LocalMediaDataSourceImpl(mockContext)
    }

    @AfterEach
    fun teardown() {
        unmockkAll()
    }

    @Test
    fun `getLocalMediaList successfully maps cursor data to Media list`() = runTest {
        // Arrange
        every {
            mockContentResolver.query(any(), any(), any(), any(), any())
        } returns mockCursor

        every { mockCursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns._ID) } returns 0
        every { mockCursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns.MEDIA_TYPE) } returns 1
        every { mockCursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns.DATE_ADDED) } returns 2

        // Simulate 1 image row
        every { mockCursor.moveToNext() } returnsMany listOf(true, false)
        every { mockCursor.getLong(0) } returns 123L
        every { mockCursor.getInt(1) } returns MediaStore.Files.FileColumns.MEDIA_TYPE_IMAGE
        every { mockCursor.getLong(2) } returns 1600000000L

        // Act
        val result = dataSource.getLocalMediaList()

        // Assert
        assertEquals(1, result.size)
        val media = result.first()
        assertEquals("external_primary_123", media.id.value)
        assertEquals(MediaType.IMAGE, media.type)
        assertEquals("content://media/external_primary/file/123", media.url.value)
        assertEquals(1600000000000L, media.createdAt.value)
    }

    @Test
    fun `getLocalMediaList successfully maps cursor data to Media list for video`() = runTest {
        // Arrange
        every {
            mockContentResolver.query(any(), any(), any(), any(), any())
        } returns mockCursor

        every { mockCursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns._ID) } returns 0
        every { mockCursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns.MEDIA_TYPE) } returns 1
        every { mockCursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns.DATE_ADDED) } returns 2

        // Simulate 1 video row
        every { mockCursor.moveToNext() } returnsMany listOf(true, false)
        every { mockCursor.getLong(0) } returns 456L
        every { mockCursor.getInt(1) } returns MediaStore.Files.FileColumns.MEDIA_TYPE_VIDEO
        every { mockCursor.getLong(2) } returns 1700000000L

        // Act
        val result = dataSource.getLocalMediaList()

        // Assert
        assertEquals(1, result.size)
        val media = result.first()
        assertEquals("external_primary_456", media.id.value)
        assertEquals(MediaType.VIDEO, media.type)
        assertEquals("content://media/external_primary/file/456", media.url.value)
        assertEquals(1700000000000L, media.createdAt.value)
    }

    @Test
    fun `getLocalMediaList returns empty list when cursor is null`() = runTest {
        // Arrange
        every {
            mockContentResolver.query(any(), any(), any(), any(), any())
        } returns null

        // Act
        val result = dataSource.getLocalMediaList()

        // Assert
        assertEquals(0, result.size)
    }

    @Test
    fun `getLocalMediaList throws SecurityException when permission denied`() = runTest {
        // Arrange
        every {
            mockContentResolver.query(any(), any(), any(), any(), any())
        } throws SecurityException("Permission denied")

        // Act & Assert
        assertThrows<SecurityException> {
            runBlocking { dataSource.getLocalMediaList() }
        }
    }
}