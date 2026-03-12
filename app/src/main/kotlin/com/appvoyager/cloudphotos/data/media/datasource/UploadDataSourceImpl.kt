package com.appvoyager.cloudphotos.data.media.datasource

import android.content.Context
import androidx.core.net.toUri
import com.amplifyframework.core.Amplify
import com.amplifyframework.storage.StoragePath
import com.amplifyframework.storage.options.StorageUploadInputStreamOptions
import com.appvoyager.cloudphotos.data.media.util.UploadErrorMapper
import com.appvoyager.cloudphotos.domain.media.model.UploadResult
import com.appvoyager.cloudphotos.domain.media.request.UploadMediaRequest
import com.appvoyager.cloudphotos.domain.media.valueobject.CloudStoragePath
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.suspendCancellableCoroutine
import java.io.FileNotFoundException
import java.util.UUID
import javax.inject.Inject
import kotlin.coroutines.cancellation.CancellationException
import kotlin.coroutines.resumeWithException

class UploadDataSourceImpl @Inject constructor(
    @param:ApplicationContext private val context: Context
) : UploadDataSource {

    override suspend fun uploadMedia(request: UploadMediaRequest): UploadResult<CloudStoragePath> =
        runCatching {
            val contentUri = request.localUri.value.toUri()
            val extension = resolveExtension(request.contentType.value)
            val remotePath = StoragePath.fromIdentityId { identityId ->
                "media/$identityId/${UUID.randomUUID()}$extension"
            }

            val inputStream = context.contentResolver.openInputStream(contentUri)
                ?: throw FileNotFoundException("Cannot open input stream for: ${request.localUri.value}")

            val options = StorageUploadInputStreamOptions.builder()
                .contentType(request.contentType.value)
                .build()

            val result = inputStream.use { stream ->
                suspendCancellableCoroutine { coroutine ->
                    val operation = Amplify.Storage.uploadInputStream(
                        remotePath,
                        stream,
                        options,
                        { coroutine.resume(it) { _, _, _ -> } },
                        { coroutine.resumeWithException(it) }
                    )
                    coroutine.invokeOnCancellation { operation.cancel() }
                }
            }

            CloudStoragePath.of(result.path)
        }.fold(
            onSuccess = { UploadResult.Success(it) },
            onFailure = { throwable ->
                if (throwable is CancellationException) throw throwable
                UploadResult.Error(UploadErrorMapper.map(throwable))
            }
        )

    private fun resolveExtension(contentType: String): String =
        when (contentType) {
            "image/jpeg" -> ".jpg"
            "image/png" -> ".png"
            "image/webp" -> ".webp"
            "image/gif" -> ".gif"
            "image/heic" -> ".heic"
            "image/heif" -> ".heif"
            "video/mp4" -> ".mp4"
            "video/quicktime" -> ".mov"
            "video/x-matroska" -> ".mkv"
            "video/webm" -> ".webm"
            else -> ""
        }

}
