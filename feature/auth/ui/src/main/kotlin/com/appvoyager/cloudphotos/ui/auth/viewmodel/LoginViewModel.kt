package com.appvoyager.cloudphotos.ui.auth.viewmodel

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.appvoyager.cloudphotos.domain.auth.model.AuthError
import com.appvoyager.cloudphotos.domain.auth.model.AuthResult
import com.appvoyager.cloudphotos.domain.auth.model.SignInState
import com.appvoyager.cloudphotos.domain.auth.request.SignInRequest
import com.appvoyager.cloudphotos.domain.auth.request.SignUpRequest
import com.appvoyager.cloudphotos.domain.auth.usecase.SignInUseCase
import com.appvoyager.cloudphotos.domain.auth.usecase.SignUpUseCase
import com.appvoyager.cloudphotos.domain.auth.valueobject.Email
import com.appvoyager.cloudphotos.domain.auth.valueobject.Password
import com.appvoyager.cloudphotos.ui.auth.effect.AuthSnackbarMessage
import com.appvoyager.cloudphotos.ui.auth.effect.LoginEffect
import com.appvoyager.cloudphotos.ui.auth.uistate.AuthFieldError
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
    private val savedStateHandle: SavedStateHandle,
    private val signInUseCase: SignInUseCase,
    private val signUpUseCase: SignUpUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(LoginUiState())
    val uiState: StateFlow<LoginUiState> = _uiState.asStateFlow()

    private val _effect = MutableSharedFlow<LoginEffect>(extraBufferCapacity = 1)
    val effect: SharedFlow<LoginEffect> = _effect.asSharedFlow()

    init {
        savedStateHandle.get<String?>("message")?.let { key ->
            AuthSnackbarMessage.fromKey(key)?.let { message ->
                viewModelScope.launch {
                    _effect.emit(LoginEffect.ShowSnackbar(message))
                }
            }
            savedStateHandle.remove<String>("message")
        }
    }

    val isFormValid: Boolean
        get() = with(_uiState.value) {
            email.isNotBlank() && ValidationUtils.isValidEmailFormat(email) && password.trim().length >= MIN_PASSWORD_LENGTH
        }

    fun onEmailChanged(value: String) =
        _uiState.update { it.copy(email = value, emailError = null) }

    fun onPasswordChanged(value: String) =
        _uiState.update { it.copy(password = value, passwordError = null) }

    fun onTogglePasswordVisibility() =
        _uiState.update { it.copy(isPasswordVisible = !it.isPasswordVisible) }

    fun onClearEmail() =
        _uiState.update { it.copy(email = "", emailError = null) }

    fun onClearPassword() =
        _uiState.update { it.copy(password = "", passwordError = null) }

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
                _uiState.update { it.copy(emailError = AuthFieldError.CheckInput) }
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
                _uiState.update { it.copy(emailError = AuthFieldError.CheckInput) }
            } finally {
                _uiState.update { it.copy(isLoading = false) }
            }
        }
    }

    private suspend fun handleSignInResult(
        result: AuthResult<SignInState>,
        requestedEmail: Email
    ) = when (result) {
        is AuthResult.Success -> {
            when (result.value) {
                is SignInState.SignedIn -> _effect.emit(LoginEffect.NavigateToHome)
                is SignInState.MFARequired,
                is SignInState.NewPasswordRequired,
                is SignInState.AdditionalStepRequired ->
                    _effect.emit(
                        LoginEffect.ShowSnackbar(AuthSnackbarMessage.AdditionalAuthRequired)
                    )
            }
        }

        is AuthResult.Error -> handleAuthError(result.error, requestedEmail)
    }

    private suspend fun handleSignUpResult(
        result: AuthResult<Unit>,
        requestedEmail: Email
    ) = when (result) {
        is AuthResult.Success -> {
            _effect.emit(LoginEffect.NavigateToVerification(requestedEmail))
        }

        is AuthResult.Error -> handleAuthError(result.error, requestedEmail)
    }

    private suspend fun handleAuthError(
        error: AuthError,
        requestedEmail: Email
    ) = when (error) {
        is AuthError.InvalidCredentials -> {
            _uiState.update { it.copy(passwordError = AuthFieldError.InvalidCredentials) }
        }

        is AuthError.InvalidPassword -> {
            _uiState.update { it.copy(passwordError = AuthFieldError.InvalidPassword) }
        }

        is AuthError.UserNotConfirmed -> {
            _effect.emit(LoginEffect.NavigateToVerification(requestedEmail))
        }

        is AuthError.UsernameAlreadyExists -> {
            _uiState.update { it.copy(emailError = AuthFieldError.EmailAlreadyRegistered) }
        }

        is AuthError.Network -> {
            _effect.emit(LoginEffect.ShowSnackbar(AuthSnackbarMessage.Network))
        }

        is AuthError.TooManyRequests -> {
            _effect.emit(LoginEffect.ShowSnackbar(AuthSnackbarMessage.TooManyRequests))
        }

        is AuthError.CodeExpired,
        is AuthError.CodeMismatch,
        is AuthError.Unknown -> {
            _effect.emit(LoginEffect.ShowSnackbar(AuthSnackbarMessage.Unknown))
        }
    }

    private fun validateForm(): Boolean {
        var valid = true
        val state = _uiState.value
        if (state.email.isBlank() || !ValidationUtils.isValidEmailFormat(state.email)) {
            _uiState.update { it.copy(emailError = AuthFieldError.InvalidEmail) }
            valid = false
        }
        if (state.password.trim().length < MIN_PASSWORD_LENGTH) {
            _uiState.update { it.copy(passwordError = AuthFieldError.PasswordTooShort) }
            valid = false
        }
        return valid
    }

    companion object {
        private const val MIN_PASSWORD_LENGTH = 8
    }

}
