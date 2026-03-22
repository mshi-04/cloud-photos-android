package com.appvoyager.cloudphotos.data.fcm

import com.amplifyframework.api.rest.RestOptions
import com.amplifyframework.core.Amplify
import com.appvoyager.cloudphotos.data.common.AMPLIFY_API_NAME
import kotlinx.coroutines.suspendCancellableCoroutine
import org.json.JSONObject
import javax.inject.Inject
import kotlin.coroutines.resumeWithException

class DeviceTokenDataSource @Inject constructor() {

    suspend fun register(token: String) {
        val body = JSONObject().apply {
            put("deviceToken", token)
            put("platform", "android")
        }.toString()

        val restOptions = RestOptions.builder()
            .addPath("/devices/token")
            .addHeaders(mapOf("Content-Type" to "application/json"))
            .addBody(body.toByteArray())
            .build()

        suspendCancellableCoroutine { coroutine ->
            val operation = Amplify.API.put(
                AMPLIFY_API_NAME,
                restOptions,
                { apiResponse ->
                    if (apiResponse.code.isSuccessful) {
                        coroutine.resume(Unit) { _, _, _ -> }
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

    suspend fun unregister(token: String) {
        val body = JSONObject().apply {
            put("deviceToken", token)
        }.toString()

        val restOptions = RestOptions.builder()
            .addPath("/devices/token")
            .addHeaders(mapOf("Content-Type" to "application/json"))
            .addBody(body.toByteArray())
            .build()

        suspendCancellableCoroutine { coroutine ->
            val operation = Amplify.API.delete(
                AMPLIFY_API_NAME,
                restOptions,
                { apiResponse ->
                    if (apiResponse.code.isSuccessful) {
                        coroutine.resume(Unit) { _, _, _ -> }
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

}
