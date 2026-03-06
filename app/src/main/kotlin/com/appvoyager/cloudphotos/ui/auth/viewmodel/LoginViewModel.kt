package com.appvoyager.cloudphotos.ui.auth.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.appvoyager.cloudphotos.R
import com.appvoyager.cloudphotos.domain.auth.model.AuthError
import com.appvoyager.cloudphotos.domain.auth.model.AuthResult
import com.appvoyager.cloudphotos.domain.auth.model.SignInState
import com.appvoyager.cloudphotos.domain.auth.request.SignInRequest
import com.appvoyager.cloudphotos.domain.auth.request.SignUpRequest
import com.appvoyager.cloudphotos.domain.auth.usecase.SignInUseCase
import com.appvoyager.cloudphotos.domain.auth.usecase.SignUpUseCase
import com.appvoyager.cloudphotos.domain.auth.valueobject.Email
import com.appvoyager.cloudphotos.domain.auth.valueobject.Password
import com.appvoyager.cloudphotos.ui.auth.effect.LoginEffect
import com.appvoyager.cloudphotos.ui.auth.uistate.LoginUiState
import com.appvoyager.cloudphotos.ui.util.ValidationUtils
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
class LoginViewModel @Inject constructor(
    private val signInUseCase: SignInUseCase,
    private val signUpUseCase: SignUpUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(LoginUiState())
    val uiState: StateFlow<LoginUiState> = _uiState.asStateFlow()

    private val _effect = MutableSharedFlow<LoginEffect>()
    val effect: SharedFlow<LoginEffect> = _effect.asSharedFlow()

    val isFormValid: Boolean
        get() = with(_uiState.value) {
            email.isNotBlank() && ValidationUtils.isValidEmailFormat(email) && password.length >= 8
        }

    fun onEmailChanged(value: String) {
        _uiState.update { it.copy(email = value, emailError = null) }
    }

    fun onPasswordChanged(value: String) {
        _uiState.update { it.copy(password = value, passwordError = null) }
    }

    fun onTogglePasswordVisibility() {
        _uiState.update { it.copy(isPasswordVisible = !it.isPasswordVisible) }
    }

    fun onClearEmail() {
        _uiState.update { it.copy(email = "", emailError = null) }
    }

    fun onClearPassword() {
        _uiState.update { it.copy(password = "", passwordError = null) }
    }

    fun onForgotPassword() {
        viewModelScope.launch {
            _effect.emit(LoginEffect.NavigateToForgotPassword)
        }
    }

    fun onSignIn() {
        if (_uiState.value.isLoading || !validateForm()) return
        _uiState.update { it.copy(isLoading = true) }

        viewModelScope.launch {
            try {
                val state = _uiState.value
                val email = Email.of(state.email)
                val password = Password.of(state.password)
                val result = signInUseCase(SignInRequest(email, password))
                handleSignInResult(result, email)
            } catch (_: IllegalArgumentException) {
                _uiState.update { it.copy(emailError = R.string.error_check_input) }
            } finally {
                _uiState.update { it.copy(isLoading = false) }
            }
        }
    }

    fun onSignUp() {
        if (_uiState.value.isLoading || !validateForm()) return
        _uiState.update { it.copy(isLoading = true) }

        viewModelScope.launch {
            try {
                val state = _uiState.value
                val email = Email.of(state.email)
                val password = Password.of(state.password)
                val result = signUpUseCase(SignUpRequest(email, password))
                handleSignUpResult(result, email)
            } catch (_: IllegalArgumentException) {
                _uiState.update { it.copy(emailError = R.string.error_check_input) }
            } finally {
                _uiState.update { it.copy(isLoading = false) }
            }
        }
    }

    private suspend fun handleSignInResult(result: AuthResult<SignInState>, requestedEmail: Email) {
        when (result) {
            is AuthResult.Success -> {
                when (result.value) {
                    is SignInState.SignedIn -> _effect.emit(LoginEffect.NavigateToHome)
                    else -> _effect.emit(LoginEffect.ShowSnackbar(R.string.error_additional_auth_required))
                }
            }

            is AuthResult.Error -> handleAuthError(result.error, requestedEmail)
        }
    }

    private suspend fun handleSignUpResult(result: AuthResult<Unit>, requestedEmail: Email) {
        when (result) {
            is AuthResult.Success -> {
                _effect.emit(LoginEffect.NavigateToVerification(requestedEmail))
            }

            is AuthResult.Error -> handleAuthError(result.error, requestedEmail)
        }
    }

    private suspend fun handleAuthError(error: AuthError, requestedEmail: Email) {
        when (error) {
            is AuthError.InvalidCredentials -> {
                _uiState.update { it.copy(passwordError = R.string.error_invalid_credentials) }
            }

            is AuthError.InvalidPassword -> {
                _uiState.update { it.copy(passwordError = R.string.error_invalid_password) }
            }

            is AuthError.UserNotConfirmed -> {
                _effect.emit(LoginEffect.NavigateToVerification(requestedEmail))
            }

            is AuthError.UsernameAlreadyExists -> {
                _uiState.update { it.copy(emailError = R.string.error_email_already_registered) }
            }

            is AuthError.Network -> {
                _effect.emit(LoginEffect.ShowSnackbar(R.string.error_network))
            }

            is AuthError.TooManyRequests -> {
                _effect.emit(LoginEffect.ShowSnackbar(R.string.error_too_many_requests))
            }

            is AuthError.CodeExpired,
            is AuthError.CodeMismatch,
            is AuthError.Unknown -> {
                _effect.emit(LoginEffect.ShowSnackbar(R.string.error_unknown))
            }
        }
    }

    private fun validateForm(): Boolean {
        var valid = true
        val state = _uiState.value
        if (state.email.isBlank() || !ValidationUtils.isValidEmailFormat(state.email)) {
            _uiState.update { it.copy(emailError = R.string.error_invalid_email) }
            valid = false
        }
        if (state.password.length < 8) {
            _uiState.update { it.copy(passwordError = R.string.error_password_too_short) }
            valid = false
        }
        return valid
    }
}
