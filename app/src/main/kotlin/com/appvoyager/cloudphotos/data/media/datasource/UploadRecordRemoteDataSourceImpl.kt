package com.appvoyager.cloudphotos.data.media.datasource

import com.amplifyframework.api.rest.RestOptions
import com.amplifyframework.core.Amplify
import com.appvoyager.cloudphotos.data.media.util.RemoteUploadRecordMapper
import com.appvoyager.cloudphotos.domain.common.Clock
import com.appvoyager.cloudphotos.domain.media.model.UploadRecord
import com.appvoyager.cloudphotos.domain.media.request.CreateUploadRecordRequest
import com.appvoyager.cloudphotos.domain.media.valueobject.MediaId
import kotlinx.coroutines.suspendCancellableCoroutine
import org.json.JSONObject
import javax.inject.Inject
import kotlin.coroutines.resumeWithException

class UploadRecordRemoteDataSourceImpl @Inject constructor(
    private val clock: Clock
) : UploadRecordRemoteDataSource {

    override suspend fun fetchUploadRecords(): List<UploadRecord> {
        val request = RestOptions.builder()
            .addPath("/media/uploads")
            .build()

        val response = suspendCancellableCoroutine { coroutine ->
            val operation = Amplify.API.get(
                API_NAME,
                request,
                { apiResponse ->
                    if (apiResponse.code.isSuccessful) {
                        coroutine.resume(apiResponse) { _, _, _ -> }
                    } else {
                        coroutine.resumeWithException(
                            Exception("Unexpected response code ${apiResponse.code}: ${apiResponse.data.asString()}")
                        )
                    }
                },
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

        val response = suspendCancellableCoroutine { coroutine ->
            val operation = Amplify.API.post(
                API_NAME,
                restOptions,
                { apiResponse ->
                    if (apiResponse.code.isSuccessful) {
                        coroutine.resume(apiResponse) { _, _, _ -> }
                    } else {
                        coroutine.resumeWithException(
                            Exception("Unexpected response code ${apiResponse.code}: ${apiResponse.data.asString()}")
                        )
                    }
                },
                { coroutine.resumeWithException(it) }
            )
            coroutine.invokeOnCancellation { operation?.cancel() }
        }

        val uploadedAt = runCatching {
            JSONObject(response.data.asString()).getLong("uploadedAt")
        }.getOrNull()
        return RemoteUploadRecordMapper.fromCreateResponse(
            uploadedAt = uploadedAt,
            request = request,
            fallbackUploadedAt = clock.getCurrentTimes()
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
                { apiResponse ->
                    if (apiResponse.code.isSuccessful) {
                        coroutine.resume(apiResponse) { _, _, _ -> }
                    } else {
                        coroutine.resumeWithException(
                            Exception("Unexpected response code ${apiResponse.code}: ${apiResponse.data.asString()}")
                        )
                    }
                },
                { coroutine.resumeWithException(it) }
            )
            coroutine.invokeOnCancellation { operation?.cancel() }
        }
    }

    companion object {
        private const val API_NAME = "CloudPhotosAPI"
    }

}
