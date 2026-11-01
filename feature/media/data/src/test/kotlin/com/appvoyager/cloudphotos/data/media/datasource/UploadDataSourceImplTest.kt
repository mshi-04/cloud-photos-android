package com.appvoyager.cloudphotos.data.media.datasource

import android.content.ContentResolver
import android.content.Context
import android.net.Uri
import com.amplifyframework.core.Amplify
import com.amplifyframework.core.Consumer
import com.amplifyframework.storage.StorageException
import com.amplifyframework.storage.StoragePath
import com.amplifyframework.storage.operation.StorageUploadInputStreamOperation
import com.amplifyframework.storage.options.StorageUploadInputStreamOptions
import com.amplifyframework.storage.result.StorageUploadInputStreamResult
import com.appvoyager.cloudphotos.data.media.testutil.uploadMediaRequestFixture
import com.appvoyager.cloudphotos.domain.media.model.UploadError
import com.appvoyager.cloudphotos.domain.media.model.UploadResult
import com.appvoyager.cloudphotos.domain.media.valueobject.CloudStoragePath
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.mockkStatic
import io.mockk.slot
import io.mockk.unmockkAll
import io.mockk.verify
import java.io.ByteArrayInputStream
import java.io.FileNotFoundException
import java.io.IOException
import kotlin.coroutines.CoroutineContext
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

@OptIn(ExperimentalCoroutinesApi::class)
class UploadDataSourceImplTest {

    private lateinit var context: Context
    private lateinit var resolver: ContentResolver
    private lateinit var uri: Uri
    private lateinit var stream: CloseTrackingInputStream
    private lateinit var operation: StorageUploadInputStreamOperation<*>

    @BeforeEach
    fun setUp() {
        context = mockk()
        resolver = mockk()
        uri = mockk()
        stream = CloseTrackingInputStream()
        operation = mockk(relaxed = true)
        every { context.contentResolver } returns resolver
        mockkStatic(Uri::class)
        every { Uri.parse(any()) } returns uri
        every { resolver.openInputStream(uri) } returns stream
        mockkObject(Amplify.Storage)
    }

    @AfterEach
    fun tearDown() {
        unmockkAll()
    }

    @Test
    fun `uploadMedia returns cloud path when upload succeeds`() = runTest {
        // Arrange
        val options = slot<StorageUploadInputStreamOptions>()
        val ioDispatcher = TrackingDispatcher(StandardTestDispatcher(testScheduler))
        val execution = mutableListOf<Pair<String, Boolean>>()
        stream = CloseTrackingInputStream { execution += "close" to ioDispatcher.isRunning }
        every { resolver.openInputStream(uri) } answers {
            execution += "open" to ioDispatcher.isRunning
            stream
        }
        every {
            Amplify.Storage.uploadInputStream(any<StoragePath>(), stream, capture(options), any(), any())
        } answers {
            execution += "upload" to ioDispatcher.isRunning
            arg<Consumer<StorageUploadInputStreamResult>>(3).accept(
                createUploadResult()
            )
            operation
        }
        val dataSource = UploadDataSourceImpl(context, ioDispatcher)

        // Act
        // Normal/Coroutine: Own opening, uploading and closing the stream inside the injected IO dispatcher.
        val result = dataSource.uploadMedia(uploadMediaRequestFixture())

        // Assert
        assertEquals(UploadResult.Success(CloudStoragePath.of("private/identity/photo.jpeg")), result)
        assertEquals("image/jpeg", options.captured.contentType)
        assertEquals(1, stream.closeCount)
        assertEquals(listOf("open" to true, "upload" to true, "close" to true), execution)
        verify(exactly = 1) { resolver.openInputStream(uri) }
        verify(exactly = 0) { operation.cancel() }
    }

    @Test
    fun `uploadMedia returns network error when SDK reports network failure`() = runTest {
        // Arrange
        val error = StorageException("Upload failed", IOException("offline"), "Retry")
        every {
            Amplify.Storage.uploadInputStream(any<StoragePath>(), stream, any(), any(), any())
        } answers {
            arg<Consumer<StorageException>>(4).accept(error)
            operation
        }
        val dataSource = UploadDataSourceImpl(context, StandardTestDispatcher(testScheduler))

        // Act
        // Error: Preserve SDK error classification and close the failed upload stream.
        val result = dataSource.uploadMedia(uploadMediaRequestFixture())

        // Assert
        assertEquals(UploadResult.Error(UploadError.Network(error.message)), result)
        assertEquals(1, stream.closeCount)
        verify(exactly = 0) { operation.cancel() }
    }

    @Test
    fun `uploadMedia returns network error when SDK invocation throws IOException`() = runTest {
        // Arrange
        every {
            Amplify.Storage.uploadInputStream(any<StoragePath>(), stream, any(), any(), any())
        } throws IOException("Cannot start upload")
        val dataSource = UploadDataSourceImpl(context, StandardTestDispatcher(testScheduler))

        // Act
        // Error: Close the stream even if SDK invocation fails before returning an operation.
        val result = dataSource.uploadMedia(uploadMediaRequestFixture())

        // Assert
        assertEquals(UploadResult.Error(UploadError.Network("Cannot start upload")), result)
        assertEquals(1, stream.closeCount)
    }

    @Test
    fun `uploadMedia returns file not found when resolver returns null stream`() = runTest {
        // Arrange
        val request = uploadMediaRequestFixture()
        every { resolver.openInputStream(uri) } returns null
        val dataSource = UploadDataSourceImpl(context, StandardTestDispatcher(testScheduler))

        // Act
        // Error: Classify a missing stream without invoking Storage.
        val result = dataSource.uploadMedia(request)

        // Assert
        assertEquals(
            UploadResult.Error(UploadError.FileNotFound("Cannot open input stream for: ${request.localUri.value}")),
            result
        )
        verify(exactly = 0) {
            Amplify.Storage.uploadInputStream(any<StoragePath>(), any(), any(), any(), any())
        }
    }

    @Test
    fun `uploadMedia returns file not found when resolver throws FileNotFoundException`() = runTest {
        // Arrange
        every { resolver.openInputStream(uri) } throws FileNotFoundException("Missing photo")
        val dataSource = UploadDataSourceImpl(context, StandardTestDispatcher(testScheduler))

        // Act
        // Error: Keep the existing classification for exceptions opening local media.
        val result = dataSource.uploadMedia(uploadMediaRequestFixture())

        // Assert
        assertEquals(UploadResult.Error(UploadError.FileNotFound("Missing photo")), result)
        verify(exactly = 0) {
            Amplify.Storage.uploadInputStream(any<StoragePath>(), any(), any(), any(), any())
        }
    }

    @Test
    fun `uploadMedia rethrows cancellation when resolver throws CancellationException`() = runTest {
        // Arrange
        val cancellation = CancellationException("Cancelled opening photo")
        every { resolver.openInputStream(uri) } throws cancellation
        val dataSource = UploadDataSourceImpl(context, StandardTestDispatcher(testScheduler))

        // Act & Assert
        // Coroutine: Never translate cancellation into an ordinary upload error.
        val thrown = assertThrows<CancellationException> {
            dataSource.uploadMedia(uploadMediaRequestFixture())
        }
        assertEquals(cancellation.message, thrown.message)
    }

    @Test
    fun `uploadMedia rethrows cancellation when caller cancels pending SDK operation`() = runTest {
        // Arrange
        every {
            Amplify.Storage.uploadInputStream(any<StoragePath>(), stream, any(), any(), any())
        } returns operation
        val dataSource = UploadDataSourceImpl(context, StandardTestDispatcher(testScheduler))
        val upload = async { dataSource.uploadMedia(uploadMediaRequestFixture()) }
        runCurrent()
        assertEquals(0, stream.closeCount)

        // Act
        // Coroutine/Interaction: Cancel the actual suspended operation and release its stream.
        upload.cancel()
        assertThrows<CancellationException> { upload.await() }

        // Assert
        assertEquals(1, stream.closeCount)
        verify(exactly = 1) { operation.cancel() }
    }

    @Test
    fun `uploadMedia rethrows cancellation when caller cancels before success resumes`() = runTest {
        // Arrange
        val success = slot<Consumer<StorageUploadInputStreamResult>>()
        every {
            Amplify.Storage.uploadInputStream(any<StoragePath>(), stream, any(), capture(success), any())
        } returns operation
        val dataSource = UploadDataSourceImpl(context, StandardTestDispatcher(testScheduler))
        val upload = async { dataSource.uploadMedia(uploadMediaRequestFixture()) }
        runCurrent()

        // Act
        // Coroutine: Close the stream when cancellation wins the SDK callback resume race.
        success.captured.accept(createUploadResult())
        upload.cancel()
        assertThrows<CancellationException> { upload.await() }

        // Assert
        assertEquals(1, stream.closeCount)
        verify(exactly = 1) { operation.cancel() }
    }

    private fun createUploadResult(): StorageUploadInputStreamResult = mockk {
        every { path } returns "private/identity/photo.jpeg"
    }

    private class TrackingDispatcher(private val delegate: CoroutineDispatcher) : CoroutineDispatcher() {
        var isRunning = false
            private set

        override fun dispatch(context: CoroutineContext, block: Runnable) {
            delegate.dispatch(context) {
                val previouslyRunning = isRunning
                isRunning = true
                try {
                    block.run()
                } finally {
                    isRunning = previouslyRunning
                }
            }
        }
    }

    private class CloseTrackingInputStream(private val onClose: () -> Unit = {}) :
        ByteArrayInputStream(byteArrayOf(1, 2, 3)) {
        var closeCount = 0
            private set

        override fun close() {
            onClose()
            closeCount++
            super.close()
        }
    }
}
