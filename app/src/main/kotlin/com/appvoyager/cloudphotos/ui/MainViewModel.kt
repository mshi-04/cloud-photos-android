package com.appvoyager.cloudphotos.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.appvoyager.cloudphotos.domain.auth.model.AuthResult
import com.appvoyager.cloudphotos.domain.auth.usecase.GetSessionUseCase
import com.appvoyager.cloudphotos.fcm.FcmTokenRegistrar
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlin.coroutines.cancellation.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@HiltViewModel
class MainViewModel @Inject constructor(
    private val getSessionUseCase: GetSessionUseCase,
    private val fcmTokenRegistrar: FcmTokenRegistrar
) : ViewModel() {

    private val _uiState = MutableStateFlow<MainUiState>(MainUiState.None)
    val uiState: StateFlow<MainUiState> = _uiState.asStateFlow()

    private val _isCheckingSession = MutableStateFlow(true)
    val isCheckingSession: StateFlow<Boolean> = _isCheckingSession.asStateFlow()

    private val _isRetrying = MutableStateFlow(false)
    val isRetrying: StateFlow<Boolean> = _isRetrying.asStateFlow()

    private var checkSessionJob: Job? = null

    fun checkSession() {
        if (checkSessionJob?.isActive == true) return
        val retrying = _uiState.value is MainUiState.SessionCheckError
        checkSessionJob = viewModelScope.launch {
            if (retrying) _isRetrying.update { true }
            try {
                val result = getSessionUseCase()
                val state = when (result) {
                    is AuthResult.Success -> {
                        if (result.value.isSignedIn) {
                            MainUiState.Authenticated
                        } else {
                            MainUiState.Unauthenticated
                        }
                    }

                    is AuthResult.Error -> MainUiState.SessionCheckError
                }
                _uiState.update { state }
            } catch (e: Exception) {
                if (e is CancellationException) throw e
                _uiState.update { MainUiState.SessionCheckError }
            } finally {
                _isCheckingSession.update { false }
                _isRetrying.update { false }
            }
        }
    }

    fun registerFcmToken() = fcmTokenRegistrar.register()
}
