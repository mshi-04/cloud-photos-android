package com.appvoyager.cloudphotos.ui.main

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.appvoyager.cloudphotos.domain.auth.model.AuthResult
import com.appvoyager.cloudphotos.domain.auth.usecase.GetSessionUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class MainViewModel @Inject constructor(
    private val getSessionUseCase: GetSessionUseCase,
) : ViewModel() {

    var uiState by mutableStateOf<MainUiState>(MainUiState.Loading)
        private set

    private var isSessionChecked = false

    fun checkSession() {
        if (isSessionChecked) return
        isSessionChecked = true

        viewModelScope.launch {
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
        }
    }

}
