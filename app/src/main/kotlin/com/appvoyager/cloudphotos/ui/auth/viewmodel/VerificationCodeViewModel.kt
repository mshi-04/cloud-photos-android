package com.appvoyager.cloudphotos.ui.auth.viewmodel

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.appvoyager.cloudphotos.R
import com.appvoyager.cloudphotos.domain.auth.model.AuthError
import com.appvoyager.cloudphotos.domain.auth.model.AuthResult
import com.appvoyager.cloudphotos.domain.auth.request.ConfirmSignUpRequest
import com.appvoyager.cloudphotos.domain.auth.request.ResendSignUpCodeRequest
import com.appvoyager.cloudphotos.domain.auth.usecase.ConfirmSignUpUseCase
import com.appvoyager.cloudphotos.domain.auth.usecase.ResendSignUpCodeUseCase
import com.appvoyager.cloudphotos.domain.auth.valueobject.ConfirmationCode
import com.appvoyager.cloudphotos.domain.auth.valueobject.Email
import com.appvoyager.cloudphotos.ui.auth.effect.VerificationEffect
import com.appvoyager.cloudphotos.ui.auth.uistate.VerificationCodeUiState
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
class VerificationCodeViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val confirmSignUpUseCase: ConfirmSignUpUseCase,
    private val resendSignUpCodeUseCase: ResendSignUpCodeUseCase
) : ViewModel() {

    val email: String = savedStateHandle.get<String>(ARG_EMAIL).orEmpty()

    private val _uiState = MutableStateFlow(VerificationCodeUiState())
    val uiState: StateFlow<VerificationCodeUiState> = _uiState.asStateFlow()

    private val _effect = MutableSharedFlow<VerificationEffect>(extraBufferCapacity = 1)
    val effect: SharedFlow<VerificationEffect> = _effect.asSharedFlow()

    private var isTimerStarted = false

    val isResendEnabled: Boolean
        get() = _uiState.value.resendTimerSeconds <= 0 && !_uiState.value.isLoading

    val isCodeComplete: Boolean
        get() = _uiState.value.codes.all { it.length == 1 && it[0].isDigit() }

    init {
        if (email.isBlank()) {
            viewModelScope.launch {
                _effect.emit(VerificationEffect.ShowSnackbar(R.string.error_unknown))
            }
        }
    }

    fun startTimer() {
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
            if (isCodeComplete) {
                onVerify()
            }
            return
        }

        val currentCodes = _uiState.value.codes.toMutableList()
        currentCodes[index] = digits.take(1)
        _uiState.update { it.copy(codes = currentCodes) }

        if (isCodeComplete) {
            onVerify()
        }
    }

    fun onVerify() {
        if (!isCodeComplete || _uiState.value.isLoading) return
        _uiState.update { it.copy(isLoading = true) }
        val fullCode = _uiState.value.codes.joinToString("")

        viewModelScope.launch {
            try {
                val email = Email.of(email)
                val code = ConfirmationCode.of(fullCode)
                when (val confirmResult = confirmSignUpUseCase(ConfirmSignUpRequest(email, code))) {
                    is AuthResult.Success -> _effect.emit(VerificationEffect.NavigateToHome)
                    is AuthResult.Error -> handleConfirmError(confirmResult.error)
                }
            } catch (_: IllegalArgumentException) {
                _uiState.update { it.copy(codeError = R.string.error_confirm_code) }
            } finally {
                _uiState.update { it.copy(isLoading = false) }
            }
        }
    }

    fun onResend() {
        if (!isResendEnabled || _uiState.value.isLoading) return
        _uiState.update { it.copy(isLoading = true) }

        viewModelScope.launch {
            try {
                val email = Email.of(email)
                when (val result = resendSignUpCodeUseCase(ResendSignUpCodeRequest(email))) {
                    is AuthResult.Success -> {
                        _effect.emit(VerificationEffect.ShowSnackbar(R.string.message_code_resent))
                        startResendTimer()
                    }

                    is AuthResult.Error -> handleResendError(result.error)
                }
            } catch (_: IllegalArgumentException) {
                _effect.emit(VerificationEffect.ShowSnackbar(R.string.error_unknown))
            } finally {
                _uiState.update { it.copy(isLoading = false) }
            }
        }
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
                _effect.emit(VerificationEffect.ShowSnackbar(R.string.error_network))
            }

            is AuthError.TooManyRequests -> {
                _effect.emit(VerificationEffect.ShowSnackbar(R.string.error_too_many_requests))
            }

            else -> {
                _effect.emit(VerificationEffect.ShowSnackbar(R.string.error_unknown))
            }
        }
    }

    private suspend fun handleResendError(error: AuthError) {
        when (error) {
            is AuthError.Network -> {
                _effect.emit(VerificationEffect.ShowSnackbar(R.string.error_network))
            }

            is AuthError.TooManyRequests -> {
                _effect.emit(VerificationEffect.ShowSnackbar(R.string.error_too_many_requests))
            }

            else -> {
                _effect.emit(VerificationEffect.ShowSnackbar(R.string.error_resend_failed))
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
