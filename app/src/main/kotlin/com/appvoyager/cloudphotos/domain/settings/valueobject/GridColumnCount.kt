package com.appvoyager.cloudphotos.domain.settings.valueobject

@JvmInline
value class GridColumnCount private constructor(val value: Int) {

    companion object {
        fun of(raw: Int): GridColumnCount =
            raw.also {
                require(it > 0) { "GridColumnCount must be greater than 0." }
            }.let(::GridColumnCount)
    }

}
