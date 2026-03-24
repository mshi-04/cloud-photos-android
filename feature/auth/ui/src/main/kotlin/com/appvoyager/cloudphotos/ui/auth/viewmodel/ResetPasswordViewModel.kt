package com.appvoyager.cloudphotos.ui.auth.viewmodel

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.appvoyager.cloudphotos.domain.auth.model.AuthError
import com.appvoyager.cloudphotos.domain.auth.model.AuthResult
import com.appvoyager.cloudphotos.domain.auth.request.ConfirmResetPasswordRequest
import com.appvoyager.cloudphotos.domain.auth.request.ResetPasswordRequest
import com.appvoyager.cloudphotos.domain.auth.usecase.ConfirmResetPasswordUseCase
import com.appvoyager.cloudphotos.domain.auth.usecase.ResetPasswordUseCase
import com.appvoyager.cloudphotos.domain.auth.valueobject.ConfirmationCode
import com.appvoyager.cloudphotos.domain.auth.valueobject.Email
import com.appvoyager.cloudphotos.domain.auth.valueobject.Password
import com.appvoyager.cloudphotos.ui.auth.effect.AuthSnackbarMessage
import com.appvoyager.cloudphotos.ui.auth.effect.ResetPasswordEffect
import com.appvoyager.cloudphotos.ui.auth.uistate.AuthFieldError
import com.appvoyager.cloudphotos.ui.auth.uistate.ResetPasswordUiState
import com.appvoyager.cloudphotos.ui.util.ResendTimer
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
class ResetPasswordViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val confirmResetPasswordUseCase: ConfirmResetPasswordUseCase,
    private val resetPasswordUseCase: ResetPasswordUseCase
) : ViewModel() {

    val email: String = savedStateHandle.get<String>(ARG_EMAIL)
        ?: error("Missing required nav argument: $ARG_EMAIL")

    private val _uiState = MutableStateFlow(ResetPasswordUiState())
    val uiState: StateFlow<ResetPasswordUiState> = _uiState.asStateFlow()

    private var isTimerStarted = false
    private val resendTimer = ResendTimer(
        durationSeconds = ResetPasswordUiState.DEFAULT_RESEND_COOLDOWN_SECONDS,
        scope = viewModelScope
    ) { seconds -> _uiState.update { it.copy(resendTimerSeconds = seconds) } }

    private val _effect = MutableSharedFlow<ResetPasswordEffect>(extraBufferCapacity = 1)
    val effect: SharedFlow<ResetPasswordEffect> = _effect.asSharedFlow()

    fun startTimerIfNeeded() {
        if (!isTimerStarted) {
            isTimerStarted = true
            resendTimer.start()
        }
    }

    fun onCodeChanged(index: Int, value: String) {
        val codeLength = _uiState.value.codes.size
        if (index !in 0 until codeLength) return
        _uiState.update { it.copy(codeError = null) }

        val digits = value.filter { it.isDigit() }
        if (digits.length > 1) {
            val currentCodes = _uiState.value.codes.toMutableList()
            digits.take(codeLength - index).forEachIndexed { i, ch ->
                currentCodes[index + i] = ch.toString()
            }
            _uiState.update { it.copy(codes = currentCodes) }
            return
        }

        val currentCodes = _uiState.value.codes.toMutableList()
        currentCodes[index] = digits.take(1)
        _uiState.update { it.copy(codes = currentCodes) }
    }

    fun onNewPasswordChanged(value: String) =
        _uiState.update { it.copy(newPassword = value, passwordError = null) }

    fun onToggleNewPasswordVisibility() =
        _uiState.update { it.copy(isNewPasswordVisible = !it.isNewPasswordVisible) }

    fun onConfirm() {
        if (_uiState.value.isLoading || !validateForm()) return
        _uiState.update { it.copy(isLoading = true) }
        val fullCode = _uiState.value.codes.joinToString("")

        viewModelScope.launch {
            try {
                val emailVO = Email.of(email)
                val code = ConfirmationCode.of(fullCode)
                val password = Password.of(_uiState.value.newPassword)
                val result = confirmResetPasswordUseCase(
                    ConfirmResetPasswordRequest(emailVO, code, password)
                )

                when (result) {
                    is AuthResult.Success -> {
                        _effect.emit(ResetPasswordEffect.NavigateBackToLogin(AuthSnackbarMessage.PasswordReset))
                    }

                    is AuthResult.Error -> handleConfirmError(result.error)
                }
            } catch (_: IllegalArgumentException) {
                _uiState.update { it.copy(codeError = AuthFieldError.CheckInput) }
            } finally {
                _uiState.update { it.copy(isLoading = false) }
            }
        }
    }

    fun onResend() {
        if (_uiState.value.isLoading || !_uiState.value.isResendEnabled) return
        _uiState.update { it.copy(isLoading = true) }

        viewModelScope.launch {
            try {
                val emailVO = Email.of(email)
                when (val result = resetPasswordUseCase(ResetPasswordRequest(emailVO))) {
                    is AuthResult.Success -> {
                        _effect.emit(ResetPasswordEffect.ShowSnackbar(AuthSnackbarMessage.CodeResent))
                        resendTimer.start()
                    }

                    is AuthResult.Error -> handleResendError(result.error)
                }
            } catch (_: IllegalArgumentException) {
                _effect.emit(ResetPasswordEffect.ShowSnackbar(AuthSnackbarMessage.Unknown))
            } finally {
                _uiState.update { it.copy(isLoading = false) }
            }
        }
    }

    private fun validateForm(): Boolean {
        var valid = true
        if (!_uiState.value.isCodeComplete) {
            _uiState.update { it.copy(codeError = AuthFieldError.EnterCode) }
            valid = false
        }
        if (_uiState.value.newPassword.length < MIN_PASSWORD_LENGTH) {
            _uiState.update { it.copy(passwordError = AuthFieldError.PasswordTooShort) }
            valid = false
        }
        return valid
    }

    private suspend fun handleConfirmError(error: AuthError) = when (error) {
        is AuthError.CodeMismatch -> {
            _uiState.update { it.copy(codeError = AuthFieldError.CodeMismatch) }
        }

        is AuthError.CodeExpired -> {
            _uiState.update { it.copy(codeError = AuthFieldError.CodeExpired) }
        }

        is AuthError.Network -> {
            _effect.emit(ResetPasswordEffect.ShowSnackbar(AuthSnackbarMessage.Network))
        }

        is AuthError.TooManyRequests -> {
            _effect.emit(ResetPasswordEffect.ShowSnackbar(AuthSnackbarMessage.TooManyRequests))
        }

        is AuthError.InvalidCredentials,
        is AuthError.InvalidPassword,
        is AuthError.Unknown,
        is AuthError.UserNotConfirmed,
        is AuthError.UsernameAlreadyExists -> {
            _effect.emit(ResetPasswordEffect.ShowSnackbar(AuthSnackbarMessage.Unknown))
        }
    }

    private suspend fun handleResendError(error: AuthError) = when (error) {
        is AuthError.Network -> {
            _effect.emit(ResetPasswordEffect.ShowSnackbar(AuthSnackbarMessage.Network))
        }

        is AuthError.TooManyRequests -> {
            _effect.emit(ResetPasswordEffect.ShowSnackbar(AuthSnackbarMessage.TooManyRequests))
        }

        is AuthError.CodeExpired,
        is AuthError.CodeMismatch,
        is AuthError.InvalidCredentials,
        is AuthError.InvalidPassword,
        is AuthError.Unknown,
        is AuthError.UserNotConfirmed,
        is AuthError.UsernameAlreadyExists -> {
            _effect.emit(ResetPasswordEffect.ShowSnackbar(AuthSnackbarMessage.ResendFailed))
        }
    }

    companion object {
        private const val ARG_EMAIL = "email"
        private const val MIN_PASSWORD_LENGTH = 8
    }

}
