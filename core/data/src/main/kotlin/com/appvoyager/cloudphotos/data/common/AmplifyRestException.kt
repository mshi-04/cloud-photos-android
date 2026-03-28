package com.appvoyager.cloudphotos.data.common

import com.amplifyframework.api.rest.RestResponse

class AmplifyRestException(val code: RestResponse.Code) : Exception("Unexpected response code $code") {
    // True for any response that is neither successful (2xx) nor a server failure (5xx).
    // Covers 1xx, 3xx, and 4xx codes. Primary use case is detecting non-retryable
    // responses (e.g. 4xx), but informational and redirect codes also satisfy this condition.
    val isNonServerError: Boolean
        get() = !code.isSuccessful && !code.isServiceFailure
}
