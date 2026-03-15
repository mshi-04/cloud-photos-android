package com.appvoyager.cloudphotos.data.media.datasource

import com.amplifyframework.api.rest.RestOptions
import com.amplifyframework.core.Amplify
import com.amplifyframework.storage.StoragePath
import com.appvoyager.cloudphotos.data.media.util.RemoteUploadRecordMapper
import com.appvoyager.cloudphotos.domain.media.model.SyncStatus
import com.appvoyager.cloudphotos.domain.media.model.UploadRecord
import com.appvoyager.cloudphotos.domain.media.request.CreateUploadRecordRequest
import com.appvoyager.cloudphotos.domain.media.valueobject.CloudStoragePath
import com.appvoyager.cloudphotos.domain.media.valueobject.IsDeleted
import com.appvoyager.cloudphotos.domain.media.valueobject.MediaId
import com.appvoyager.cloudphotos.domain.media.valueobject.MediaUploadedAt
import kotlinx.coroutines.suspendCancellableCoroutine
import org.json.JSONObject
import javax.inject.Inject
import kotlin.coroutines.resumeWithException

private const val API_NAME = "CloudPhotosAPI"

class UploadRecordRemoteDataSourceImpl @Inject constructor() : UploadRecordRemoteDataSource {

    override suspend fun fetchUploadRecords(): List<UploadRecord> {
        val request = RestOptions.builder()
            .addPath("/media/uploads")
            .build()

        val response = suspendCancellableCoroutine { coroutine ->
            val operation = Amplify.API.get(
                API_NAME,
                request,
                { coroutine.resume(it) { _, _, _ -> } },
                { coroutine.resumeWithException(it) }
            )
            coroutine.invokeOnCancellation { operation?.cancel() }
        }

        return RemoteUploadRecordMapper.fromFetchResponse(response.data.asString())
    }

    override suspend fun createUploadRecord(request: CreateUploadRecordRequest): UploadRecord {
        val body = JSONObject().apply {
            put("mediaId", request.mediaId.value)
            put("cloudStoragePath", request.cloudStoragePath.value)
            put("contentType", request.contentType.value)
            put("mediaType", request.mediaType.name)
        }.toString()

        val restOptions = RestOptions.builder()
            .addPath("/media/uploads")
            .addHeaders(mapOf("Content-Type" to "application/json"))
            .addBody(body.toByteArray())
            .build()

        suspendCancellableCoroutine { coroutine ->
            val operation = Amplify.API.post(
                API_NAME,
                restOptions,
                { coroutine.resume(it) { _, _, _ -> } },
                { coroutine.resumeWithException(it) }
            )
            coroutine.invokeOnCancellation { operation?.cancel() }
        }

        return UploadRecord(
            mediaId = request.mediaId,
            cloudStoragePath = request.cloudStoragePath,
            isDeleted = IsDeleted.of(false),
            syncStatus = SyncStatus.SYNCED,
            mediaUploadedAt = MediaUploadedAt.of(System.currentTimeMillis())
        )
    }

    override suspend fun deleteUploadRecord(mediaId: MediaId) {
        val request = RestOptions.builder()
            .addPath("/media/uploads/${mediaId.value}")
            .build()

        suspendCancellableCoroutine { coroutine ->
            val operation = Amplify.API.delete(
                API_NAME,
                request,
                { coroutine.resume(it) { _, _, _ -> } },
                { coroutine.resumeWithException(it) }
            )
            coroutine.invokeOnCancellation { operation?.cancel() }
        }
    }

    override suspend fun deleteStorageFile(cloudStoragePath: CloudStoragePath) {
        val storagePath = StoragePath.fromString(cloudStoragePath.value)

        suspendCancellableCoroutine { coroutine ->
            Amplify.Storage.remove(
                storagePath,
                { coroutine.resume(it) { _, _, _ -> } },
                { coroutine.resumeWithException(it) }
            )
        }
    }

}
