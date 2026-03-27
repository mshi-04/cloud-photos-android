package com.appvoyager.cloudphotos.data.common

import com.amplifyframework.api.ApiException
import com.amplifyframework.api.rest.RestOptions
import com.amplifyframework.api.rest.RestResponse
import com.amplifyframework.core.Consumer
import com.amplifyframework.core.async.Cancelable
import kotlin.coroutines.resumeWithException
import kotlinx.coroutines.suspendCancellableCoroutine

suspend fun awaitAmplifyRestCall(
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
                    AmplifyRestException(
                        code = apiResponse.code,
                        responseBody = apiResponse.data.asString()
                    )
                )
            }
        },
        Consumer { coroutine.resumeWithException(it) }
    )
    coroutine.invokeOnCancellation { operation?.cancel() }
}
