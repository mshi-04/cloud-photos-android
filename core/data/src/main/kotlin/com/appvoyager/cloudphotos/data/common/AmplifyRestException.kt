package com.appvoyager.cloudphotos.data.common

import com.amplifyframework.api.rest.RestResponse

class AmplifyRestException(val code: RestResponse.Code) : Exception("Unexpected response code $code") {
    // True for any response that is neither successful (2xx) nor a server failure (5xx),
    // which includes 1xx, 3xx, and 4xx codes. Named isClientError because the primary
    // use case is detecting non-retryable 4xx responses; caller should be aware it also
    // covers informational and redirect codes.
    val isClientError: Boolean
        get() = !code.isSuccessful && !code.isServiceFailure
}
