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
import kotlinx.coroutines.launch
import javax.inject.Inject
import kotlin.coroutines.cancellation.CancellationException

@HiltViewModel
class MainViewModel @Inject constructor(
    private val getSessionUseCase: GetSessionUseCase,
    private val signOutUseCase: SignOutUseCase
) : ViewModel() {

    var uiState by mutableStateOf<MainUiState>(MainUiState.Loading)
        private set

    private var isSessionChecked = false

    fun checkSession() {
        if (isSessionChecked) return
        isSessionChecked = true

        viewModelScope.launch {
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

                    is AuthResult.Error -> MainUiState.Unauthenticated
                }
            } catch (e: Exception) {
                if (e is CancellationException) throw e
                isSessionChecked = false
                uiState = MainUiState.Unauthenticated
            }
        }
    }

    fun signOut() {
        viewModelScope.launch {
            try {
                val result = signOutUseCase()
                uiState = when (result) {
                    is AuthResult.Success -> MainUiState.Unauthenticated
                    is AuthResult.Error -> MainUiState.Authenticated
                }
            } catch (e: Exception) {
                if (e is CancellationException) throw e
            }
        }
    }

}
