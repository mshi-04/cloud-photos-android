package com.appvoyager.cloudphotos.data.media.datasource

import android.content.ContentResolver
import android.content.ContentValues
import android.content.Context
import android.net.Uri
import android.provider.MediaStore
import com.appvoyager.cloudphotos.domain.common.Clock
import com.appvoyager.cloudphotos.domain.media.model.SavePhotoResult
import com.appvoyager.cloudphotos.domain.media.repository.CapturedPhotoWriter
import com.appvoyager.cloudphotos.domain.media.valueobject.MediaUrl
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone
import javax.inject.Inject
import kotlin.coroutines.cancellation.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class MediaStoreCapturedPhotoWriter internal constructor(
    private val contentResolver: ContentResolver,
    private val clock: Clock,
    private val ioDispatcher: CoroutineDispatcher
) : CapturedPhotoWriter {

    @Inject
    constructor(@ApplicationContext context: Context, clock: Clock) : this(
        contentResolver = context.contentResolver,
        clock = clock,
        ioDispatcher = Dispatchers.IO
    )

    override suspend fun write(jpegData: ByteArray): SavePhotoResult = withContext(ioDispatcher) {
        var createdUri: Uri? = null
        try {
            val uri = contentResolver.insert(
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                createContentValues()
            ) ?: return@withContext SavePhotoResult.Error(SavePhotoResult.ErrorType.SAVE_FAILED)
            createdUri = uri

            contentResolver.openOutputStream(uri)?.use { output ->
                output.write(jpegData)
            } ?: throw IOException("Failed to open output stream")

            val pending = ContentValues().apply {
                put(MediaStore.Images.Media.IS_PENDING, 0)
            }
            val rowCount = contentResolver.update(uri, pending, null, null)
            if (rowCount <= 0) throw IOException("Failed to publish captured photo")
            SavePhotoResult.Success(MediaUrl.of(uri.toString()))
        } catch (e: Exception) {
            if (e is CancellationException) throw e
            createdUri?.let { uri ->
                runCatching { contentResolver.delete(uri, null, null) }
                    .onFailure { if (it is CancellationException) throw it }
            }
            val errorType = if (isStorageFull(e)) {
                SavePhotoResult.ErrorType.STORAGE_FULL
            } else {
                SavePhotoResult.ErrorType.SAVE_FAILED
            }
            SavePhotoResult.Error(errorType)
        }
    }

    private fun isStorageFull(exception: Throwable): Boolean {
        var cause: Throwable? = exception
        while (cause != null) {
            if (cause is android.system.ErrnoException && cause.errno == android.system.OsConstants.ENOSPC) {
                return true
            }
            cause = cause.cause
        }
        return false
    }

    private fun createContentValues(): ContentValues {
        val name = CapturedPhotoFileName.create(clock.getCurrentTime(), TimeZone.getDefault())

        return ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, name)
            put(MediaStore.MediaColumns.MIME_TYPE, "image/jpeg")
            put(MediaStore.Images.Media.RELATIVE_PATH, RELATIVE_PATH)
            put(MediaStore.Images.Media.IS_PENDING, 1)
        }
    }

    companion object {
        private const val RELATIVE_PATH = "DCIM/Camera"
    }
}

internal object CapturedPhotoFileName {
    fun create(epochMillis: Long, timeZone: TimeZone): String =
        SimpleDateFormat("yyyy-MM-dd-HH-mm-ss-SSS", Locale.US).apply {
            this.timeZone = timeZone
        }.format(epochMillis)
}
