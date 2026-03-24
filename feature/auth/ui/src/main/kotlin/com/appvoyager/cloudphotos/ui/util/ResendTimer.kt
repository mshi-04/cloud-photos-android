package com.appvoyager.cloudphotos.ui.util

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class ResendTimer(
    private val durationSeconds: Int,
    private val scope: CoroutineScope,
    private val onTick: (seconds: Int) -> Unit
) {
    private var job: Job? = null

    fun start() {
        job?.cancel()
        onTick(durationSeconds)
        job = scope.launch {
            var remaining = durationSeconds
            while (remaining > 0) {
                delay(1_000L)
                remaining--
                onTick(remaining)
            }
        }
    }
}
