package com.appvoyager.cloudphotos.data.media.datasource

import android.content.ContentValues
import android.content.Context
import android.provider.MediaStore
import com.appvoyager.cloudphotos.domain.media.model.SavePhotoResult
import com.appvoyager.cloudphotos.domain.media.repository.CapturedPhotoWriter
import com.appvoyager.cloudphotos.domain.media.valueobject.MediaUrl
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.Locale
import javax.inject.Inject
import kotlin.coroutines.cancellation.CancellationException

class MediaStoreCapturedPhotoWriter @Inject constructor(
    @param:ApplicationContext private val context: Context
) : CapturedPhotoWriter {

    override suspend fun write(jpegData: ByteArray): SavePhotoResult =
        withContext(Dispatchers.IO) {
            val contentValues = createContentValues()
            val uri = context.contentResolver.insert(
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                contentValues
            ) ?: return@withContext SavePhotoResult.Error(SavePhotoResult.ErrorType.SAVE_FAILED)

            try {
                context.contentResolver.openOutputStream(uri)?.use { output ->
                    output.write(jpegData)
                } ?: throw IOException("Failed to open output stream")

                val pending = ContentValues().apply {
                    put(MediaStore.Images.Media.IS_PENDING, 0)
                }
                val rowCount = context.contentResolver.update(uri, pending, null, null)
                if (rowCount > 0) {
                    SavePhotoResult.Success(MediaUrl.of(uri.toString()))
                } else {
                    runCatching { context.contentResolver.delete(uri, null, null) }
                        .onFailure { if (it is CancellationException) throw it }
                    SavePhotoResult.Error(SavePhotoResult.ErrorType.SAVE_FAILED)
                }
            } catch (e: Exception) {
                if (e is CancellationException) throw e
                runCatching { context.contentResolver.delete(uri, null, null) }
                    .onFailure { if (it is CancellationException) throw it }
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
        val name = SimpleDateFormat(FILENAME_FORMAT, Locale.US)
            .format(System.currentTimeMillis())

        return ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, name)
            put(MediaStore.MediaColumns.MIME_TYPE, "image/jpeg")
            put(MediaStore.Images.Media.RELATIVE_PATH, RELATIVE_PATH)
            put(MediaStore.Images.Media.IS_PENDING, 1)
        }
    }

    companion object {
        private const val FILENAME_FORMAT = "yyyy-MM-dd-HH-mm-ss-SSS"
        private const val RELATIVE_PATH = "DCIM/Camera"
    }
}
