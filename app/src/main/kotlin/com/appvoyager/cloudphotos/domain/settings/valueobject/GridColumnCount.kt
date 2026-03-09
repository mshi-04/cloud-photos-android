package com.appvoyager.cloudphotos.domain.settings.valueobject

@JvmInline
value class GridColumnCount private constructor(val value: Int) {

    companion object {
        const val MIN = 2
        const val MAX = 6

        fun of(raw: Int): GridColumnCount =
            raw.also {
                require(it in MIN..MAX) { "GridColumnCount must be in $MIN..$MAX, but was $it." }
            }.let(::GridColumnCount)
    }

}
