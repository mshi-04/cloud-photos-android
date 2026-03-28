package com.appvoyager.cloudphotos.ui

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.appvoyager.cloudphotos.domain.auth.model.AuthResult
import com.appvoyager.cloudphotos.domain.auth.usecase.GetSessionUseCase
import com.appvoyager.cloudphotos.fcm.FcmTokenRegistrar
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlin.coroutines.cancellation.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

@HiltViewModel
class MainViewModel @Inject constructor(
    private val getSessionUseCase: GetSessionUseCase,
    private val fcmTokenRegistrar: FcmTokenRegistrar
) : ViewModel() {

    var uiState by mutableStateOf<MainUiState>(MainUiState.None)
        private set

    var isCheckingSession by mutableStateOf(true)
        private set

    var isRetrying by mutableStateOf(false)
        private set

    private var checkSessionJob: Job? = null

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

    fun registerFcmToken() = fcmTokenRegistrar.register()
}
