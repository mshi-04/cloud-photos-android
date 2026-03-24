package com.appvoyager.cloudphotos.data.media.datasource

import com.amplifyframework.api.ApiException
import com.amplifyframework.api.rest.RestOptions
import com.amplifyframework.api.rest.RestResponse
import com.amplifyframework.core.Amplify
import com.amplifyframework.core.Consumer
import com.amplifyframework.core.async.Cancelable
import com.appvoyager.cloudphotos.data.common.AMPLIFY_API_NAME
import com.appvoyager.cloudphotos.data.media.util.RemoteUploadRecordMapper
import com.appvoyager.cloudphotos.domain.common.Clock
import com.appvoyager.cloudphotos.domain.media.model.UploadRecord
import com.appvoyager.cloudphotos.domain.media.request.CreateUploadRecordRequest
import com.appvoyager.cloudphotos.domain.media.valueobject.MediaId
import com.appvoyager.cloudphotos.domain.media.valueobject.UploadSuccessCount
import kotlinx.coroutines.suspendCancellableCoroutine
import org.json.JSONObject
import javax.inject.Inject
import kotlin.coroutines.resumeWithException

class UploadRecordRemoteDataSourceImpl @Inject constructor(
    private val clock: Clock
) : UploadRecordRemoteDataSource {

    override suspend fun fetchUploadRecords(): List<UploadRecord> {
        val options = RestOptions.builder()
            .addPath("/media/uploads")
            .build()

        val response = awaitAmplifyResponse(options) { name, opts, onResp, onErr ->
            Amplify.API.get(name, opts, onResp, onErr)
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

        val options = RestOptions.builder()
            .addPath("/media/uploads")
            .addHeaders(mapOf("Content-Type" to "application/json"))
            .addBody(body.toByteArray())
            .build()

        val response = awaitAmplifyResponse(options) { name, opts, onResp, onErr ->
            Amplify.API.post(name, opts, onResp, onErr)
        }

        val json = JSONObject(response.data.asString())
        val uploadedAt: Long? = if (json.has("uploadedAt")) json.getLong("uploadedAt") else null
        return RemoteUploadRecordMapper.fromCreateResponse(
            uploadedAt = uploadedAt,
            request = request,
            fallbackUploadedAt = clock.getCurrentTime()
        )
    }

    override suspend fun completeUpload(successCount: UploadSuccessCount) {
        val body = JSONObject().apply {
            put("successCount", successCount.value)
        }.toString()

        val options = RestOptions.builder()
            .addPath("/media/uploads/complete")
            .addHeaders(mapOf("Content-Type" to "application/json"))
            .addBody(body.toByteArray())
            .build()

        awaitAmplifyResponse(options) { name, opts, onResp, onErr ->
            Amplify.API.post(name, opts, onResp, onErr)
        }
    }

    override suspend fun deleteUploadRecord(mediaId: MediaId) {
        val options = RestOptions.builder()
            .addPath("/media/uploads/${mediaId.value}")
            .build()

        awaitAmplifyResponse(options) { name, opts, onResp, onErr ->
            Amplify.API.delete(name, opts, onResp, onErr)
        }
    }

    private suspend fun awaitAmplifyResponse(
        options: RestOptions,
        apiCall: (String, RestOptions, Consumer<RestResponse>, Consumer<ApiException>) -> Cancelable?
    ): RestResponse = suspendCancellableCoroutine { coroutine ->
        val operation = apiCall(
            AMPLIFY_API_NAME,
            options,
            Consumer { apiResponse ->
                if (apiResponse.code.isSuccessful) {
                    coroutine.resume(apiResponse) { _, _, _ -> }
                } else {
                    coroutine.resumeWithException(
                        Exception("Unexpected response code ${apiResponse.code}: ${apiResponse.data.asString()}")
                    )
                }
            },
            Consumer { coroutine.resumeWithException(it) }
        )
        coroutine.invokeOnCancellation { operation?.cancel() }
    }

}
