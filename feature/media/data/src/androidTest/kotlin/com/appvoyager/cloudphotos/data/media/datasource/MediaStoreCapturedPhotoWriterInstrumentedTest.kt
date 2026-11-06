package com.appvoyager.cloudphotos.data.media.datasource

import android.content.ContentProvider
import android.content.ContentResolver
import android.content.ContentValues
import android.content.Context
import android.content.pm.ProviderInfo
import android.content.res.AssetFileDescriptor
import android.database.Cursor
import android.net.Uri
import android.os.ParcelFileDescriptor
import android.provider.MediaStore
import android.system.ErrnoException
import android.system.OsConstants
import androidx.test.core.app.ApplicationProvider
import com.appvoyager.cloudphotos.domain.common.Clock
import com.appvoyager.cloudphotos.domain.media.model.SavePhotoResult
import com.appvoyager.cloudphotos.domain.media.valueobject.MediaUrl
import java.io.File
import java.io.FileDescriptor
import java.io.FileOutputStream
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.Locale
import kotlin.coroutines.cancellation.CancellationException
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Before
import org.junit.Test

class MediaStoreCapturedPhotoWriterInstrumentedTest {

    private lateinit var provider: PhotoProvider
    private lateinit var resolver: ContentResolver
    private val clock = object : Clock {
        override fun getCurrentTime(): Long = FIXED_TIME
    }

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        provider = PhotoProvider(File.createTempFile("captured-photo-", ".jpg", context.cacheDir))
        provider.attachInfo(context, ProviderInfo().apply { authority = "media" })
        resolver = ContentResolver.wrap(provider)
    }

    @After
    fun tearDown() {
        provider.close()
    }

    @Test
    fun write_returnsSuccess_whenImageIsWrittenAndPublished() = runTest {
        // Arrange
        val writer = MediaStoreCapturedPhotoWriter(resolver, clock, StandardTestDispatcher(testScheduler))
        val bytes = byteArrayOf(1, 2, 3, 4)

        // Act
        // Normal: pending publication follows writing all JPEG bytes to the inserted record
        val actual = writer.write(bytes)

        // Assert
        assertEquals(SavePhotoResult.Success(MediaUrl.of(provider.insertedUri.toString())), actual)
        assertEquals(listOf("insert", "open", "update"), provider.events)
        assertEquals(1, provider.insertValues?.getAsInteger(MediaStore.Images.Media.IS_PENDING))
        assertEquals(0, provider.updateValues?.getAsInteger(MediaStore.Images.Media.IS_PENDING))
        assertEquals("image/jpeg", provider.insertValues?.getAsString(MediaStore.MediaColumns.MIME_TYPE))
        assertEquals("DCIM/Camera", provider.insertValues?.getAsString(MediaStore.Images.Media.RELATIVE_PATH))
        assertEquals(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, provider.insertCollection)
        assertEquals(listOf(provider.insertedUri), provider.openedUris)
        assertEquals(listOf(provider.insertedUri), provider.updatedUris)
        assertArrayEquals(bytes, provider.bytesAtPublication)
        assertArrayEquals(bytes, provider.file.readBytes())
        assertEquals(1, provider.closedDescriptorCount)
        assertTrue(provider.deletedUris.isEmpty())
    }

    @Test
    fun write_setsClockBasedDisplayName_whenImageIsInserted() = runTest {
        // Arrange
        val writer = MediaStoreCapturedPhotoWriter(resolver, clock, StandardTestDispatcher(testScheduler))
        val expectedName = SimpleDateFormat("yyyy-MM-dd-HH-mm-ss-SSS", Locale.US).format(FIXED_TIME)

        // Act
        // Interaction: the injected clock supplies the filename time in the device timezone
        writer.write(byteArrayOf(1))

        // Assert
        assertEquals(expectedName, provider.insertValues?.getAsString(MediaStore.MediaColumns.DISPLAY_NAME))
    }

    @Test
    fun write_returnsSaveFailed_whenContentValuesCannotBeCreated() = runTest {
        // Arrange
        val failingClock = object : Clock {
            override fun getCurrentTime(): Long = throw IllegalStateException("Clock unavailable")
        }
        val writer = MediaStoreCapturedPhotoWriter(resolver, failingClock, StandardTestDispatcher(testScheduler))

        // Act
        // Error: failure before insertion cannot cause cleanup of an unrelated record
        val actual = writer.write(byteArrayOf(1))

        // Assert
        assertEquals(SavePhotoResult.Error(SavePhotoResult.ErrorType.SAVE_FAILED), actual)
        assertTrue(provider.events.isEmpty())
        assertTrue(provider.deletedUris.isEmpty())
    }

    @Test
    fun write_returnsSaveFailed_whenInsertThrows() = runTest {
        // Arrange
        provider.insertFailure = SecurityException("Insert denied")
        val writer = MediaStoreCapturedPhotoWriter(resolver, clock, StandardTestDispatcher(testScheduler))

        // Act
        // Error: an insert failure is converted without deleting a record that was never created
        val actual = writer.write(byteArrayOf(1))

        // Assert
        assertEquals(SavePhotoResult.Error(SavePhotoResult.ErrorType.SAVE_FAILED), actual)
        assertEquals(listOf("insert"), provider.events)
        assertTrue(provider.deletedUris.isEmpty())
    }

    @Test
    fun write_returnsStorageFull_whenInsertThrowsEnospc() = runTest {
        // Arrange
        provider.insertFailure = ErrnoException("insert", OsConstants.ENOSPC)
        val writer = MediaStoreCapturedPhotoWriter(resolver, clock, StandardTestDispatcher(testScheduler))

        // Act
        // Error: storage exhaustion during insert has the same result as exhaustion during writing
        val actual = writer.write(byteArrayOf(1))

        // Assert
        assertEquals(SavePhotoResult.Error(SavePhotoResult.ErrorType.STORAGE_FULL), actual)
        assertTrue(provider.deletedUris.isEmpty())
    }

    @Test
    fun write_returnsStorageFull_whenInsertFailureContainsEnospcCause() = runTest {
        // Arrange
        provider.insertFailure = IOException("Insert failed", ErrnoException("insert", OsConstants.ENOSPC))
        val writer = MediaStoreCapturedPhotoWriter(resolver, clock, StandardTestDispatcher(testScheduler))

        // Act
        // Error: wrapped storage exhaustion is classified through its cause
        val actual = writer.write(byteArrayOf(1))

        // Assert
        assertEquals(SavePhotoResult.Error(SavePhotoResult.ErrorType.STORAGE_FULL), actual)
        assertTrue(provider.deletedUris.isEmpty())
    }

    @Test
    fun write_returnsSaveFailed_whenInsertReturnsNull() = runTest {
        // Arrange
        provider.nullInsert = true
        val writer = MediaStoreCapturedPhotoWriter(resolver, clock, StandardTestDispatcher(testScheduler))

        // Act
        // Boundary: null insert cannot be opened, published, or cleaned up
        val actual = writer.write(byteArrayOf(1))

        // Assert
        assertEquals(SavePhotoResult.Error(SavePhotoResult.ErrorType.SAVE_FAILED), actual)
        assertEquals(listOf("insert"), provider.events)
        assertTrue(provider.deletedUris.isEmpty())
    }

    @Test
    fun write_returnsSaveFailed_whenOutputStreamIsNull() = runTest {
        // Arrange
        provider.nullStream = true
        val writer = MediaStoreCapturedPhotoWriter(resolver, clock, StandardTestDispatcher(testScheduler))

        // Act
        // Error: a missing output stream triggers cleanup of the inserted URI only
        val actual = writer.write(byteArrayOf(1))

        // Assert
        assertEquals(SavePhotoResult.Error(SavePhotoResult.ErrorType.SAVE_FAILED), actual)
        assertEquals(listOf("insert", "open", "delete"), provider.events)
        assertEquals(listOf(provider.insertedUri), provider.deletedUris)
        assertTrue(provider.updatedUris.isEmpty())
    }

    @Test
    fun write_returnsSaveFailed_whenWritingToDescriptorFails() = runTest {
        // Arrange
        provider.readOnlyStream = true
        val writer = MediaStoreCapturedPhotoWriter(resolver, clock, StandardTestDispatcher(testScheduler))

        // Act
        // Error: a real descriptor write error prevents publication and removes the inserted record
        val actual = writer.write(byteArrayOf(1))

        // Assert
        assertEquals(SavePhotoResult.Error(SavePhotoResult.ErrorType.SAVE_FAILED), actual)
        assertEquals(listOf("insert", "open", "delete"), provider.events)
        assertEquals(listOf(provider.insertedUri), provider.deletedUris)
        assertTrue(provider.updatedUris.isEmpty())
        assertEquals(1, provider.closedDescriptorCount)
    }

    @Test
    fun write_returnsStorageFull_whenOutputWriteThrowsEnospc() = runTest {
        // Arrange
        provider.storageFullOnWrite = true
        val writer = MediaStoreCapturedPhotoWriter(resolver, clock, StandardTestDispatcher(testScheduler))

        // Act
        // Error: ENOSPC raised by output.write is reported as storage exhaustion
        val actual = writer.write(byteArrayOf(1))

        // Assert
        assertEquals(SavePhotoResult.Error(SavePhotoResult.ErrorType.STORAGE_FULL), actual)
    }

    @Test
    fun write_returnsSaveFailed_whenPublicationThrows() = runTest {
        // Arrange
        provider.updateFailure = SecurityException("Update denied")
        val writer = MediaStoreCapturedPhotoWriter(resolver, clock, StandardTestDispatcher(testScheduler))

        // Act
        // Error: a publication exception cleans up the already written record
        val actual = writer.write(byteArrayOf(1))

        // Assert
        assertEquals(SavePhotoResult.Error(SavePhotoResult.ErrorType.SAVE_FAILED), actual)
        assertEquals(listOf("insert", "open", "update", "delete"), provider.events)
        assertEquals(listOf(provider.insertedUri), provider.deletedUris)
    }

    @Test
    fun write_returnsStorageFull_whenPublicationFailureContainsEnospc() = runTest {
        // Arrange
        provider.updateFailure = IOException("Update failed", ErrnoException("update", OsConstants.ENOSPC))
        val writer = MediaStoreCapturedPhotoWriter(resolver, clock, StandardTestDispatcher(testScheduler))

        // Act
        // Error: nested storage exhaustion after insertion retains its classification and removes the record
        val actual = writer.write(byteArrayOf(1))

        // Assert
        assertEquals(SavePhotoResult.Error(SavePhotoResult.ErrorType.STORAGE_FULL), actual)
        assertEquals(listOf(provider.insertedUri), provider.deletedUris)
    }

    @Test
    fun write_returnsSaveFailed_whenPublicationUpdatesNoRows() = runTest {
        // Arrange
        provider.updateCount = 0
        val writer = MediaStoreCapturedPhotoWriter(resolver, clock, StandardTestDispatcher(testScheduler))

        // Act
        // Boundary: zero affected rows cannot be reported as a successfully published photo
        val actual = writer.write(byteArrayOf(1))

        // Assert
        assertEquals(SavePhotoResult.Error(SavePhotoResult.ErrorType.SAVE_FAILED), actual)
        assertEquals(listOf(provider.insertedUri), provider.deletedUris)
        assertEquals(listOf("insert", "open", "update", "delete"), provider.events)
    }

    @Test
    fun write_returnsOriginalError_whenCleanupThrows() = runTest {
        // Arrange
        provider.updateFailure = IOException("Full", ErrnoException("update", OsConstants.ENOSPC))
        provider.deleteFailure = SecurityException("Delete denied")
        val writer = MediaStoreCapturedPhotoWriter(resolver, clock, StandardTestDispatcher(testScheduler))

        // Act
        // Error: failed best-effort cleanup does not replace the original storage-full result
        val actual = writer.write(byteArrayOf(1))

        // Assert
        assertEquals(SavePhotoResult.Error(SavePhotoResult.ErrorType.STORAGE_FULL), actual)
        assertEquals(listOf(provider.insertedUri), provider.deletedUris)
    }

    @Test
    fun write_rethrowsCancellation_whenInsertIsCancelled() = runTest {
        // Arrange
        val cancellation = CancellationException("Insert cancelled")
        provider.insertFailure = cancellation
        val writer = MediaStoreCapturedPhotoWriter(resolver, clock, StandardTestDispatcher(testScheduler))

        // Act & Assert
        // Coroutine: cancellation before insertion does not become a normal save failure
        try {
            writer.write(byteArrayOf(1))
            fail("Expected CancellationException")
        } catch (actual: CancellationException) {
            assertEquals(cancellation.message, actual.message)
        }
        assertEquals(listOf("insert"), provider.events)
        assertTrue(provider.deletedUris.isEmpty())
    }

    @Test
    fun write_rethrowsCancellation_whenOpeningInsertedRecordIsCancelled() = runTest {
        // Arrange
        val cancellation = CancellationException("Open cancelled")
        provider.openFailure = cancellation
        val writer = MediaStoreCapturedPhotoWriter(resolver, clock, StandardTestDispatcher(testScheduler))

        // Act & Assert
        // Coroutine: the existing cancellation contract leaves the inserted pending record untouched
        try {
            writer.write(byteArrayOf(1))
            fail("Expected CancellationException")
        } catch (actual: CancellationException) {
            assertEquals(cancellation.message, actual.message)
        }
        assertEquals(listOf("insert", "open"), provider.events)
        assertTrue(provider.deletedUris.isEmpty())
    }

    @Test
    fun write_rethrowsCancellation_whenPublicationIsCancelled() = runTest {
        // Arrange
        val cancellation = CancellationException("Update cancelled")
        provider.updateFailure = cancellation
        val writer = MediaStoreCapturedPhotoWriter(resolver, clock, StandardTestDispatcher(testScheduler))

        // Act & Assert
        // Coroutine: cancellation after writing is propagated without deleting the pending record
        try {
            writer.write(byteArrayOf(1))
            fail("Expected CancellationException")
        } catch (actual: CancellationException) {
            assertEquals(cancellation.message, actual.message)
        }
        assertEquals(listOf("insert", "open", "update"), provider.events)
        assertTrue(provider.deletedUris.isEmpty())
    }

    @Test
    fun write_rethrowsCancellation_whenCleanupIsCancelled() = runTest {
        // Arrange
        val cancellation = CancellationException("Delete cancelled")
        provider.nullStream = true
        provider.deleteFailure = cancellation
        val writer = MediaStoreCapturedPhotoWriter(resolver, clock, StandardTestDispatcher(testScheduler))

        // Act & Assert
        // Coroutine: best-effort cleanup still propagates cancellation rather than returning SAVE_FAILED
        try {
            writer.write(byteArrayOf(1))
            fail("Expected CancellationException")
        } catch (actual: CancellationException) {
            assertEquals(cancellation.message, actual.message)
        }
        assertEquals(listOf(provider.insertedUri), provider.deletedUris)
    }

    companion object {
        private const val FIXED_TIME = 1700000000123L
    }
}

private class PhotoProvider(val file: File) : ContentProvider() {

    val insertedUri: Uri = Uri.parse("content://media/external/images/media/123")
    val events = mutableListOf<String>()
    val openedUris = mutableListOf<Uri>()
    val updatedUris = mutableListOf<Uri>()
    val deletedUris = mutableListOf<Uri>()
    var insertCollection: Uri? = null
    var insertValues: ContentValues? = null
    var updateValues: ContentValues? = null
    var bytesAtPublication: ByteArray? = null
    var insertFailure: Exception? = null
    var openFailure: Exception? = null
    var updateFailure: Exception? = null
    var deleteFailure: Exception? = null
    var nullInsert = false
    var nullStream = false
    var readOnlyStream = false
    var storageFullOnWrite = false
    var updateCount = 1
    private val descriptors = mutableListOf<ParcelFileDescriptor>()
    private val openedFileDescriptors = mutableListOf<FileDescriptor>()
    val closedDescriptorCount: Int
        get() = openedFileDescriptors.count { !it.valid() }

    override fun onCreate(): Boolean = true

    override fun insert(uri: Uri, values: ContentValues?): Uri? {
        events += "insert"
        insertCollection = uri
        insertValues = values?.let(::ContentValues)
        insertFailure?.let { throw it }
        return if (nullInsert) null else insertedUri
    }

    override fun openAssetFile(uri: Uri, mode: String): AssetFileDescriptor? {
        events += "open"
        openedUris += uri
        openFailure?.let { throw it }
        if (nullStream) return null
        val descriptorMode = if (readOnlyStream) {
            ParcelFileDescriptor.MODE_READ_ONLY
        } else {
            ParcelFileDescriptor.MODE_WRITE_ONLY or ParcelFileDescriptor.MODE_TRUNCATE
        }
        val descriptor = ParcelFileDescriptor.open(file, descriptorMode)
        descriptors += descriptor
        openedFileDescriptors += descriptor.fileDescriptor
        return object : AssetFileDescriptor(descriptor, 0, AssetFileDescriptor.UNKNOWN_LENGTH) {
            override fun createOutputStream(): FileOutputStream {
                if (!storageFullOnWrite) return super.createOutputStream()
                return object : AssetFileDescriptor.AutoCloseOutputStream(this) {
                    override fun write(buffer: ByteArray): Unit =
                        throw IOException("Write failed", ErrnoException("write", OsConstants.ENOSPC))
                }
            }
        }
    }

    override fun update(uri: Uri, values: ContentValues?, selection: String?, selectionArgs: Array<out String>?): Int {
        events += "update"
        updatedUris += uri
        updateValues = values?.let(::ContentValues)
        bytesAtPublication = file.readBytes()
        updateFailure?.let { throw it }
        return updateCount
    }

    override fun delete(uri: Uri, selection: String?, selectionArgs: Array<out String>?): Int {
        events += "delete"
        deletedUris += uri
        deleteFailure?.let { throw it }
        file.delete()
        return 1
    }

    override fun query(
        uri: Uri,
        projection: Array<out String>?,
        selection: String?,
        selectionArgs: Array<out String>?,
        sortOrder: String?
    ): Cursor? = null

    override fun getType(uri: Uri): String = "image/jpeg"

    fun close() {
        descriptors.forEach { it.close() }
        file.delete()
    }
}
