package com.appvoyager.cloudphotos.ui.auth.viewmodel

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.appvoyager.cloudphotos.R
import com.appvoyager.cloudphotos.domain.auth.model.AuthError
import com.appvoyager.cloudphotos.domain.auth.model.AuthResult
import com.appvoyager.cloudphotos.domain.auth.request.ConfirmResetPasswordRequest
import com.appvoyager.cloudphotos.domain.auth.request.ResetPasswordRequest
import com.appvoyager.cloudphotos.domain.auth.usecase.ConfirmResetPasswordUseCase
import com.appvoyager.cloudphotos.domain.auth.usecase.ResetPasswordUseCase
import com.appvoyager.cloudphotos.domain.auth.valueobject.ConfirmationCode
import com.appvoyager.cloudphotos.domain.auth.valueobject.Email
import com.appvoyager.cloudphotos.domain.auth.valueobject.Password
import com.appvoyager.cloudphotos.ui.auth.effect.ResetPasswordCodeEffect
import com.appvoyager.cloudphotos.ui.auth.uistate.ResetPasswordCodeUiState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
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
class ResetPasswordCodeViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val confirmResetPasswordUseCase: ConfirmResetPasswordUseCase,
    private val resetPasswordUseCase: ResetPasswordUseCase
) : ViewModel() {

    val email: String = savedStateHandle.get<String>(ARG_EMAIL)
        ?: error("Missing required nav argument: $ARG_EMAIL")

    private val _uiState = MutableStateFlow(ResetPasswordCodeUiState())
    val uiState: StateFlow<ResetPasswordCodeUiState> = _uiState.asStateFlow()

    private var isTimerStarted = false

    private val _effect = MutableSharedFlow<ResetPasswordCodeEffect>(extraBufferCapacity = 1)
    val effect: SharedFlow<ResetPasswordCodeEffect> = _effect.asSharedFlow()

    val isResendEnabled: Boolean
        get() = _uiState.value.resendTimerSeconds <= 0 && !_uiState.value.isLoading

    val isCodeComplete: Boolean
        get() = _uiState.value.codes.all { it.length == 1 && it[0].isDigit() }

    val isFormValid: Boolean
        get() = isCodeComplete && _uiState.value.newPassword.length >= 8

    fun startTimerIfNeeded() {
        if (!isTimerStarted) {
            isTimerStarted = true
            startResendTimer()
        }
    }

    fun onCodeChanged(index: Int, value: String) {
        if (index !in 0..5) return
        _uiState.update { it.copy(codeError = null) }

        val digits = value.filter { it.isDigit() }
        if (digits.length > 1) {
            val currentCodes = _uiState.value.codes.toMutableList()
            digits.take(6 - index).forEachIndexed { i, ch ->
                currentCodes[index + i] = ch.toString()
            }
            _uiState.update { it.copy(codes = currentCodes) }
            return
        }

        val currentCodes = _uiState.value.codes.toMutableList()
        currentCodes[index] = digits.take(1)
        _uiState.update { it.copy(codes = currentCodes) }
    }

    fun onNewPasswordChanged(value: String) {
        _uiState.update { it.copy(newPassword = value, passwordError = null) }
    }

    fun onToggleNewPasswordVisibility() {
        _uiState.update { it.copy(isNewPasswordVisible = !it.isNewPasswordVisible) }
    }

    fun onClearCodes() {
        _uiState.update { it.copy(codes = List(6) { "" }, codeError = null) }
    }

    fun onConfirm() {
        if (!validateForm() || _uiState.value.isLoading) return
        val fullCode = _uiState.value.codes.joinToString("")

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            try {
                val email = Email.of(email)
                val code = ConfirmationCode.of(fullCode)
                val password = Password.of(_uiState.value.newPassword)
                val result = confirmResetPasswordUseCase(
                    ConfirmResetPasswordRequest(email, code, password)
                )

                when (result) {
                    is AuthResult.Success -> {
                        _effect.emit(ResetPasswordCodeEffect.ShowSnackbar(R.string.message_password_reset))
                        _effect.emit(ResetPasswordCodeEffect.NavigateBackToLogin)
                    }

                    is AuthResult.Error -> handleConfirmError(result.error)
                }
            } catch (_: IllegalArgumentException) {
                _uiState.update { it.copy(codeError = R.string.error_check_input) }
            } finally {
                _uiState.update { it.copy(isLoading = false) }
            }
        }
    }

    fun onResend() {
        if (!isResendEnabled) return

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            try {
                val email = Email.of(email)
                val result = resetPasswordUseCase(ResetPasswordRequest(email))

                when (result) {
                    is AuthResult.Success -> {
                        _effect.emit(ResetPasswordCodeEffect.ShowSnackbar(R.string.message_code_resent))
                        startResendTimer()
                    }

                    is AuthResult.Error -> handleResendError(result.error)
                }
            } catch (_: IllegalArgumentException) {
                _effect.emit(ResetPasswordCodeEffect.ShowSnackbar(R.string.error_unknown))
            } finally {
                _uiState.update { it.copy(isLoading = false) }
            }
        }
    }

    private fun validateForm(): Boolean {
        var valid = true
        if (!isCodeComplete) {
            _uiState.update { it.copy(codeError = R.string.error_enter_code) }
            valid = false
        }
        if (_uiState.value.newPassword.length < 8) {
            _uiState.update { it.copy(passwordError = R.string.error_password_too_short) }
            valid = false
        }
        return valid
    }

    private suspend fun handleConfirmError(error: AuthError) {
        when (error) {
            is AuthError.CodeMismatch -> {
                _uiState.update { it.copy(codeError = R.string.error_code_mismatch) }
            }

            is AuthError.CodeExpired -> {
                _uiState.update { it.copy(codeError = R.string.error_code_expired) }
            }

            is AuthError.Network -> {
                _effect.emit(ResetPasswordCodeEffect.ShowSnackbar(R.string.error_network))
            }

            is AuthError.TooManyRequests -> {
                _effect.emit(ResetPasswordCodeEffect.ShowSnackbar(R.string.error_too_many_requests))
            }

            else -> {
                _effect.emit(ResetPasswordCodeEffect.ShowSnackbar(R.string.error_unknown))
            }
        }
    }

    private suspend fun handleResendError(error: AuthError) {
        when (error) {
            is AuthError.Network -> {
                _effect.emit(ResetPasswordCodeEffect.ShowSnackbar(R.string.error_network))
            }

            is AuthError.TooManyRequests -> {
                _effect.emit(ResetPasswordCodeEffect.ShowSnackbar(R.string.error_too_many_requests))
            }

            else -> {
                _effect.emit(ResetPasswordCodeEffect.ShowSnackbar(R.string.error_resend_failed))
            }
        }
    }

    private fun startResendTimer() {
        _uiState.update { it.copy(resendTimerSeconds = RESEND_COOLDOWN_SECONDS) }
        viewModelScope.launch {
            while (_uiState.value.resendTimerSeconds > 0) {
                delay(1_000L)
                _uiState.update { it.copy(resendTimerSeconds = it.resendTimerSeconds - 1) }
            }
        }
    }

    companion object {
        const val RESEND_COOLDOWN_SECONDS = 60
        private const val ARG_EMAIL = "email"
    }
}
