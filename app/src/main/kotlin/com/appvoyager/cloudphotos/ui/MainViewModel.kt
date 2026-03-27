package com.appvoyager.cloudphotos.ui

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.appvoyager.cloudphotos.domain.auth.model.AuthResult
import com.appvoyager.cloudphotos.domain.auth.usecase.GetSessionUseCase
import com.appvoyager.cloudphotos.domain.auth.usecase.SignOutUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlin.coroutines.cancellation.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch

@HiltViewModel
class MainViewModel @Inject constructor(
    private val getSessionUseCase: GetSessionUseCase,
    private val signOutUseCase: SignOutUseCase
) : ViewModel() {

    var uiState by mutableStateOf<MainUiState>(MainUiState.None)
        private set

    var isCheckingSession by mutableStateOf(true)
        private set

    var isRetrying by mutableStateOf(false)
        private set

    private val _uiEvent = Channel<MainUiEvent>(Channel.BUFFERED)
    val uiEvent = _uiEvent.receiveAsFlow()

    private var checkSessionJob: Job? = null
    private var signOutJob: Job? = null

    fun checkSession() {
        if (checkSessionJob?.isActive == true) return
        val retrying = uiState is MainUiState.SessionCheckError
        checkSessionJob = viewModelScope.launch {
            if (retrying) isRetrying = true
            try {
                val result = getSessionUseCase()
                uiState = when (result) {
                    is AuthResult.Success -> {
                        if (result.value.isSignedIn) {
                            MainUiState.Authenticated
                        } else {
                            MainUiState.Unauthenticated
                        }
                    }

                    is AuthResult.Error -> MainUiState.SessionCheckError
                }
            } catch (e: Exception) {
                if (e is CancellationException) throw e
                uiState = MainUiState.SessionCheckError
            } finally {
                isCheckingSession = false
                isRetrying = false
            }
        }
    }

    fun signOut() {
        if (signOutJob?.isActive == true) return
        signOutJob = viewModelScope.launch {
            try {
                val result = signOutUseCase()
                uiState = when (result) {
                    is AuthResult.Success -> MainUiState.Unauthenticated
                    is AuthResult.Error -> {
                        _uiEvent.trySend(MainUiEvent.SignOutFailed)
                        MainUiState.Authenticated
                    }
                }
            } catch (e: Exception) {
                if (e is CancellationException) throw e
                uiState = MainUiState.Authenticated
                _uiEvent.trySend(MainUiEvent.SignOutFailed)
            }
        }
    }
}
