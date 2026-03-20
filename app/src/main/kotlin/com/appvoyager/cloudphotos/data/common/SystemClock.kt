package com.appvoyager.cloudphotos.data.common

import com.appvoyager.cloudphotos.domain.common.Clock
import javax.inject.Inject

class SystemClock @Inject constructor() : Clock {

    override fun getCurrentTime(): Long = System.currentTimeMillis()

}
