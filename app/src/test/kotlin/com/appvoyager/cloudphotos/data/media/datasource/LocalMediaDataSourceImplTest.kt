package com.appvoyager.cloudphotos.data.media.datasource

import android.content.ContentResolver
import android.content.Context
import android.database.Cursor
import android.net.Uri
import android.provider.MediaStore
import com.appvoyager.cloudphotos.domain.media.model.MediaType
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.unmockkAll
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

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
        mockkStatic(MediaStore.Files::class)
        mockkStatic(MediaStore.Images.Media::class)
        mockkStatic(MediaStore.Video.Media::class)

        val mockUri = mockk<Uri>(relaxed = true)
        every { mockUri.toString() } returns "content://media/external/file/123"
        
        every { Uri.withAppendedPath(any(), any()) } returns mockUri
        every { MediaStore.Files.getContentUri("external") } returns mockUri

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
            mockContentResolver.query(
                any(),
                any(),
                any(),
                any(),
                any()
            )
        } returns mockCursor

        every { mockCursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns._ID) } returns 0
        every { mockCursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns.MEDIA_TYPE) } returns 1
        every { mockCursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns.DATE_ADDED) } returns 2
        every { mockCursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns.DURATION) } returns 3

        // Simulate 1 image row
        every { mockCursor.moveToNext() } returnsMany listOf(true, false)
        every { mockCursor.getLong(0) } returns 123L
        every { mockCursor.getInt(1) } returns MediaStore.Files.FileColumns.MEDIA_TYPE_IMAGE
        every { mockCursor.getLong(2) } returns 1600000000L
        every { mockCursor.getLong(3) } returns 0L

        // Act
        val result = dataSource.getLocalMediaList()

        // Assert
        assertEquals(1, result.size)
        val media = result.first()
        assertEquals("123", media.id.value)
        assertEquals(MediaType.IMAGE, media.type)
        assertEquals("content://media/external/file/123", media.url.value)
        assertEquals(1600000000000L, media.createdAt.value)
    }
    
    @Test
    fun `getLocalMediaList returns empty list when cursor is null`() = runTest {
        // Arrange
        every {
            mockContentResolver.query(
                any(),
                any(),
                any(),
                any(),
                any()
            )
        } returns null

        // Act
        val result = dataSource.getLocalMediaList()

        // Assert
        assertEquals(0, result.size)
    }
}
