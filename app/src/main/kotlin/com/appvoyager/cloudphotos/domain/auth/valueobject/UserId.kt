package com.appvoyager.cloudphotos.domain.auth.valueobject

@JvmInline
value class UserId(val value: String) {

    init {
        require(value.isNotBlank()) { "UserId must not be blank." }
    }

}
