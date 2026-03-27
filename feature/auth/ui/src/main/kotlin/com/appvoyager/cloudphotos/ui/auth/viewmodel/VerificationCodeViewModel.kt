package com.appvoyager.cloudphotos.ui.auth.viewmodel

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.appvoyager.cloudphotos.domain.auth.model.AuthError
import com.appvoyager.cloudphotos.domain.auth.model.AuthResult
import com.appvoyager.cloudphotos.domain.auth.request.ConfirmSignUpRequest
import com.appvoyager.cloudphotos.domain.auth.request.ResendSignUpCodeRequest
import com.appvoyager.cloudphotos.domain.auth.usecase.ConfirmSignUpUseCase
import com.appvoyager.cloudphotos.domain.auth.usecase.ResendSignUpCodeUseCase
import com.appvoyager.cloudphotos.domain.auth.valueobject.ConfirmationCode
import com.appvoyager.cloudphotos.domain.auth.valueobject.Email
import com.appvoyager.cloudphotos.ui.auth.effect.AuthSnackbarMessage
import com.appvoyager.cloudphotos.ui.auth.effect.VerificationEffect
import com.appvoyager.cloudphotos.ui.auth.uistate.AuthFieldError
import com.appvoyager.cloudphotos.ui.auth.uistate.VerificationCodeUiState
import com.appvoyager.cloudphotos.ui.util.ResendTimer
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@HiltViewModel
class VerificationCodeViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val confirmSignUpUseCase: ConfirmSignUpUseCase,
    private val resendSignUpCodeUseCase: ResendSignUpCodeUseCase
) : ViewModel() {

    val email: Email? = savedStateHandle.get<String>(ARG_EMAIL)
        ?.let { runCatching { Email.of(it) }.getOrNull() }

    private val _uiState = MutableStateFlow(VerificationCodeUiState(isLoading = false))
    val uiState: StateFlow<VerificationCodeUiState> = _uiState.asStateFlow()

    private val _effect = MutableSharedFlow<VerificationEffect>(extraBufferCapacity = 1)
    val effect: SharedFlow<VerificationEffect> = _effect.asSharedFlow()

    private var isTimerStarted = false
    private val resendTimer = ResendTimer(
        durationSeconds = VerificationCodeUiState.DEFAULT_RESEND_COOLDOWN_SECONDS,
        scope = viewModelScope
    ) { seconds -> _uiState.update { it.copy(resendTimerSeconds = seconds) } }

    init {
        if (email == null) {
            viewModelScope.launch {
                _effect.emit(VerificationEffect.NavigateBack)
            }
        }
    }

    fun startTimer() {
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
            if (_uiState.value.isCodeComplete) {
                onVerify()
            }
            return
        }

        val currentCodes = _uiState.value.codes.toMutableList()
        currentCodes[index] = digits.take(1)
        _uiState.update { it.copy(codes = currentCodes) }

        if (_uiState.value.isCodeComplete) {
            onVerify()
        }
    }

    fun onVerify() {
        val emailValue = email ?: return
        if (!_uiState.value.isCodeComplete || _uiState.value.isLoading) return
        _uiState.update { it.copy(isLoading = true) }
        val fullCode = _uiState.value.codes.joinToString("")

        viewModelScope.launch {
            try {
                val codeValue = ConfirmationCode.of(fullCode)
                when (
                    val confirmResult =
                        confirmSignUpUseCase(ConfirmSignUpRequest(emailValue, codeValue))
                ) {
                    is AuthResult.Success -> _effect.emit(VerificationEffect.NavigateToHome)
                    is AuthResult.Error -> handleConfirmError(confirmResult.error)
                }
            } catch (_: IllegalArgumentException) {
                _uiState.update { it.copy(codeError = AuthFieldError.ConfirmCode) }
            } finally {
                _uiState.update { it.copy(isLoading = false) }
            }
        }
    }

    fun onResend() {
        val emailValue = email ?: return
        if (!_uiState.value.isResendEnabled || _uiState.value.isLoading) return
        _uiState.update { it.copy(isLoading = true) }

        viewModelScope.launch {
            try {
                when (val result = resendSignUpCodeUseCase(ResendSignUpCodeRequest(emailValue))) {
                    is AuthResult.Success -> {
                        _effect.emit(VerificationEffect.ShowSnackbar(AuthSnackbarMessage.CodeResent))
                        resendTimer.start()
                    }

                    is AuthResult.Error -> handleResendError(result.error)
                }
            } catch (_: IllegalArgumentException) {
                _effect.emit(VerificationEffect.ShowSnackbar(AuthSnackbarMessage.Unknown))
            } finally {
                _uiState.update { it.copy(isLoading = false) }
            }
        }
    }

    private suspend fun handleConfirmError(error: AuthError) = when (error) {
        is AuthError.CodeMismatch -> {
            _uiState.update { it.copy(codeError = AuthFieldError.CodeMismatch) }
        }

        is AuthError.CodeExpired -> {
            _uiState.update { it.copy(codeError = AuthFieldError.CodeExpired) }
        }

        is AuthError.Network -> {
            _effect.emit(VerificationEffect.ShowSnackbar(AuthSnackbarMessage.Network))
        }

        is AuthError.TooManyRequests -> {
            _effect.emit(VerificationEffect.ShowSnackbar(AuthSnackbarMessage.TooManyRequests))
        }

        is AuthError.InvalidCredentials,
        is AuthError.InvalidPassword,
        is AuthError.Unknown,
        is AuthError.UserNotConfirmed,
        is AuthError.UsernameAlreadyExists -> {
            _effect.emit(VerificationEffect.ShowSnackbar(AuthSnackbarMessage.Unknown))
        }
    }

    private suspend fun handleResendError(error: AuthError) = when (error) {
        is AuthError.Network -> {
            _effect.emit(VerificationEffect.ShowSnackbar(AuthSnackbarMessage.Network))
        }

        is AuthError.TooManyRequests -> {
            _effect.emit(VerificationEffect.ShowSnackbar(AuthSnackbarMessage.TooManyRequests))
        }

        is AuthError.CodeExpired,
        is AuthError.CodeMismatch,
        is AuthError.InvalidCredentials,
        is AuthError.InvalidPassword,
        is AuthError.Unknown,
        is AuthError.UserNotConfirmed,
        is AuthError.UsernameAlreadyExists -> {
            _effect.emit(VerificationEffect.ShowSnackbar(AuthSnackbarMessage.ResendFailed))
        }
    }

    companion object {
        private const val ARG_EMAIL = "email"
    }
}
