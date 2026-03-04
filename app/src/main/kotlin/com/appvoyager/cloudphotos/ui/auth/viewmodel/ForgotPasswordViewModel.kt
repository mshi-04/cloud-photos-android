package com.appvoyager.cloudphotos.ui.auth.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.appvoyager.cloudphotos.R
import com.appvoyager.cloudphotos.domain.auth.model.AuthError
import com.appvoyager.cloudphotos.domain.auth.model.AuthResult
import com.appvoyager.cloudphotos.domain.auth.request.ResetPasswordRequest
import com.appvoyager.cloudphotos.domain.auth.usecase.ResetPasswordUseCase
import com.appvoyager.cloudphotos.domain.auth.valueobject.Email
import com.appvoyager.cloudphotos.ui.auth.effect.ForgotPasswordEffect
import com.appvoyager.cloudphotos.ui.auth.uistate.ForgotPasswordUiState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ForgotPasswordViewModel @Inject constructor(
    private val resetPasswordUseCase: ResetPasswordUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(ForgotPasswordUiState())
    val uiState: StateFlow<ForgotPasswordUiState> = _uiState.asStateFlow()

    private val _effect = MutableSharedFlow<ForgotPasswordEffect>()
    val effect: SharedFlow<ForgotPasswordEffect> = _effect.asSharedFlow()

    fun onEmailChanged(value: String) {
        _uiState.update { it.copy(email = value, emailError = null) }
    }

    fun onClearEmail() {
        _uiState.update { it.copy(email = "", emailError = null) }
    }

    fun onSubmit() {
        if (!_uiState.value.isFormValid || _uiState.value.isLoading) return

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            val email = try {
                Email.of(_uiState.value.email)
            } catch (_: IllegalArgumentException) {
                _uiState.update {
                    it.copy(
                        emailError = R.string.error_invalid_email,
                        isLoading = false
                    )
                }
                return@launch
            }
            try {
                when (val result = resetPasswordUseCase(ResetPasswordRequest(email))) {
                    is AuthResult.Success -> {
                        _effect.emit(ForgotPasswordEffect.NavigateToResetCode(email))
                    }

                    is AuthResult.Error -> handleError(result.error)
                }
            } finally {
                _uiState.update { it.copy(isLoading = false) }
            }
        }
    }

    private suspend fun handleError(error: AuthError) {
        when (error) {
            is AuthError.Network -> {
                _effect.emit(ForgotPasswordEffect.ShowSnackbar(R.string.error_network))
            }

            is AuthError.TooManyRequests -> {
                _effect.emit(ForgotPasswordEffect.ShowSnackbar(R.string.error_too_many_requests))
            }

            is AuthError.CodeExpired,
            is AuthError.CodeMismatch,
            is AuthError.InvalidCredentials,
            is AuthError.InvalidPassword,
            is AuthError.Unknown,
            is AuthError.UserNotConfirmed,
            is AuthError.UsernameAlreadyExists -> {
                android.util.Log.e("ForgotPasswordViewModel", "unexpected auth error", Exception(error.toString()))
                _effect.emit(ForgotPasswordEffect.ShowSnackbar(R.string.error_unknown))
            }
        }
    }
}
