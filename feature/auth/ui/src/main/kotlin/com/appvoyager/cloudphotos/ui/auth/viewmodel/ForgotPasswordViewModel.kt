package com.appvoyager.cloudphotos.ui.auth.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.appvoyager.cloudphotos.domain.auth.model.AuthError
import com.appvoyager.cloudphotos.domain.auth.model.AuthResult
import com.appvoyager.cloudphotos.domain.auth.request.ResetPasswordRequest
import com.appvoyager.cloudphotos.domain.auth.usecase.ResetPasswordUseCase
import com.appvoyager.cloudphotos.domain.auth.valueobject.Email
import com.appvoyager.cloudphotos.ui.auth.effect.AuthSnackbarMessage
import com.appvoyager.cloudphotos.ui.auth.effect.ForgotPasswordEffect
import com.appvoyager.cloudphotos.ui.auth.uistate.AuthFieldError
import com.appvoyager.cloudphotos.ui.auth.uistate.ForgotPasswordUiState
import com.appvoyager.cloudphotos.ui.util.ValidationUtils
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@HiltViewModel
class ForgotPasswordViewModel @Inject constructor(private val resetPasswordUseCase: ResetPasswordUseCase) :
    ViewModel() {

    private val _uiState = MutableStateFlow(ForgotPasswordUiState())
    val uiState: StateFlow<ForgotPasswordUiState> = _uiState.asStateFlow()

    private val _effect = Channel<ForgotPasswordEffect>(Channel.BUFFERED)
    val effect: Flow<ForgotPasswordEffect> = _effect.receiveAsFlow()

    val isFormValid: Boolean
        get() = isFormValid(_uiState.value)

    internal fun isFormValid(state: ForgotPasswordUiState): Boolean =
        state.email.isNotBlank() && ValidationUtils.isValidEmailFormat(state.email)

    fun onEmailChanged(value: String) = _uiState.update { it.copy(email = value, emailError = null) }

    fun onClearEmail() = _uiState.update { it.copy(email = "", emailError = null) }

    fun onSubmit() {
        if (_uiState.value.isLoading || !validateForm()) return
        _uiState.update { it.copy(isLoading = true) }

        viewModelScope.launch {
            try {
                val email = Email.of(_uiState.value.email)
                when (val result = resetPasswordUseCase(ResetPasswordRequest(email))) {
                    is AuthResult.Success -> {
                        _effect.send(ForgotPasswordEffect.NavigateToResetPassword(email))
                    }

                    is AuthResult.Error -> handleError(result.error, email)
                }
            } catch (_: IllegalArgumentException) {
                _uiState.update { it.copy(emailError = AuthFieldError.InvalidEmail) }
            } finally {
                _uiState.update { it.copy(isLoading = false) }
            }
        }
    }

    private fun validateForm(): Boolean {
        if (!isFormValid) {
            _uiState.update { it.copy(emailError = AuthFieldError.InvalidEmail) }
            return false
        }
        return true
    }

    private suspend fun handleError(error: AuthError, email: Email) = when (error) {
        is AuthError.Network -> {
            _effect.send(ForgotPasswordEffect.ShowSnackbar(AuthSnackbarMessage.Network))
        }

        is AuthError.TooManyRequests -> {
            _effect.send(ForgotPasswordEffect.ShowSnackbar(AuthSnackbarMessage.TooManyRequests))
        }

        is AuthError.UserNotConfirmed -> {
            _effect.send(ForgotPasswordEffect.NavigateToVerification(email))
        }

        is AuthError.InvalidPassword -> {
            _effect.send(ForgotPasswordEffect.ShowSnackbar(AuthSnackbarMessage.InvalidPassword))
        }

        is AuthError.InvalidCredentials -> {
            _effect.send(ForgotPasswordEffect.ShowSnackbar(AuthSnackbarMessage.InvalidCredentials))
        }

        is AuthError.CodeExpired -> {
            _effect.send(ForgotPasswordEffect.ShowSnackbar(AuthSnackbarMessage.CodeExpired))
        }

        is AuthError.CodeMismatch -> {
            _effect.send(ForgotPasswordEffect.ShowSnackbar(AuthSnackbarMessage.CodeMismatch))
        }

        is AuthError.Unknown,
        is AuthError.UsernameAlreadyExists -> {
            _effect.send(ForgotPasswordEffect.ShowSnackbar(AuthSnackbarMessage.Unknown))
        }
    }
}
