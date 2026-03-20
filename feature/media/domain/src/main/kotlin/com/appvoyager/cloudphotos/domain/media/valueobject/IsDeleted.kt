package com.appvoyager.cloudphotos.domain.media.valueobject

@JvmInline
value class IsDeleted private constructor(val value: Boolean) {

    companion object {
        fun of(raw: Boolean): IsDeleted = IsDeleted(raw)
    }

}
