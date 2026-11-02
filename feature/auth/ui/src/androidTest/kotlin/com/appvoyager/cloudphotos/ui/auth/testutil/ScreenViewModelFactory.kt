package com.appvoyager.cloudphotos.ui.auth.testutil

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlin.coroutines.CoroutineContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Delay
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.InternalCoroutinesApi
import kotlinx.coroutines.MainCoroutineDispatcher
import kotlinx.coroutines.cancel
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestCoroutineScheduler
import kotlinx.coroutines.test.TestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain

@OptIn(ExperimentalCoroutinesApi::class)
internal class ScreenViewModelFactory(scheduler: TestCoroutineScheduler) {
    private val dispatcher = ViewModelMainDispatcher(StandardTestDispatcher(scheduler))
    private val scopes = mutableListOf<CoroutineScope>()

    fun <T : ViewModel> create(factory: () -> T): T {
        Dispatchers.setMain(dispatcher)
        return try {
            factory().also { scopes += it.viewModelScope }
        } finally {
            Dispatchers.resetMain()
        }
    }

    fun close() {
        scopes.forEach { it.cancel() }
        scopes.clear()
    }
}

// Capture the immediate dispatcher in ViewModel scopes before restoring the global Main dispatcher.
@OptIn(InternalCoroutinesApi::class)
private class ViewModelMainDispatcher(private val delegate: TestDispatcher) :
    MainCoroutineDispatcher(),
    Delay by delegate {
    override val immediate: MainCoroutineDispatcher get() = this

    override fun dispatch(context: CoroutineContext, block: Runnable) = delegate.dispatch(context, block)

    override fun isDispatchNeeded(context: CoroutineContext): Boolean = delegate.isDispatchNeeded(context)
}
