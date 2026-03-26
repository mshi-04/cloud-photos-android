package com.appvoyager.cloudphotos.data.common

import com.amplifyframework.api.rest.RestResponse

class AmplifyRestException(
    val code: RestResponse.Code,
    val responseBody: String
) : Exception("Unexpected response code $code") {

    val isClientError: Boolean
        get() = !code.isSuccessful && !code.isServiceFailure
}
