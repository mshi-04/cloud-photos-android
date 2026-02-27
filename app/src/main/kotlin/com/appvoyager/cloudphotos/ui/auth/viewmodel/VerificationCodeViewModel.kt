package com.appvoyager.cloudphotos.ui.auth.viewmodel

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
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
import com.appvoyager.cloudphotos.ui.auth.effect.VerificationEffect
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class VerificationCodeViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val confirmSignUpUseCase: ConfirmSignUpUseCase,
    private val resendSignUpCodeUseCase: ResendSignUpCodeUseCase
) : ViewModel() {

    val email: String = savedStateHandle.get<String>(ARG_EMAIL)
        ?: error("Missing required nav argument: $ARG_EMAIL")

    var codes by mutableStateOf(List(6) { "" })
        private set

    var isLoading by mutableStateOf(false)
        private set

    var codeError by mutableStateOf<String?>(null)
        private set

    var resendTimerSeconds by mutableIntStateOf(RESEND_COOLDOWN_SECONDS)
        private set

    val isResendEnabled: Boolean
        get() = resendTimerSeconds <= 0 && !isLoading

    val isCodeComplete: Boolean
        get() = codes.all { it.length == 1 && it[0].isDigit() }

    private val _effect = MutableSharedFlow<VerificationEffect>()
    val effect: SharedFlow<VerificationEffect> = _effect.asSharedFlow()

    private var isTimerStarted = false

    init {
        if (email.isEmpty()) {
            viewModelScope.launch {
                _effect.emit(VerificationEffect.ShowSnackbar("エラーが発生しました"))
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
        codeError = null

        val digits = value.filter { it.isDigit() }
        if (digits.length > 1) {
            val newCodes = codes.toMutableList()
            digits.take(6 - index).forEachIndexed { i, ch ->
                newCodes[index + i] = ch.toString()
            }
            codes = newCodes
            if (isCodeComplete) {
                onVerify()
            }
            return
        }

        val newCodes = codes.toMutableList()
        newCodes[index] = digits.take(1)
        codes = newCodes

        if (isCodeComplete) {
            onVerify()
        }
    }

    fun onVerify() {
        if (!isCodeComplete || isLoading) return
        val fullCode = codes.joinToString("")

        viewModelScope.launch {
            isLoading = true
            try {
                val email = Email.of(email)
                val code = ConfirmationCode.of(fullCode)
                when (val confirmResult = confirmSignUpUseCase(ConfirmSignUpRequest(email, code))) {
                    is AuthResult.Success -> _effect.emit(VerificationEffect.NavigateToHome)
                    is AuthResult.Error -> handleConfirmError(confirmResult.error)
                }
            } catch (_: IllegalArgumentException) {
                codeError = "正しい確認コードを入力してください"
            } finally {
                isLoading = false
            }
        }
    }

    fun onResend() {
        if (!isResendEnabled) return

        viewModelScope.launch {
            isLoading = true
            try {
                val email = Email.of(email)
                when (val result = resendSignUpCodeUseCase(ResendSignUpCodeRequest(email))) {
                    is AuthResult.Success -> {
                        _effect.emit(VerificationEffect.ShowSnackbar("確認コードを再送信しました"))
                        startResendTimer()
                    }

                    is AuthResult.Error -> handleResendError(result.error)
                }
            } catch (_: IllegalArgumentException) {
                _effect.emit(VerificationEffect.ShowSnackbar("エラーが発生しました"))
            } finally {
                isLoading = false
            }
        }
    }

    private suspend fun handleConfirmError(error: AuthError) {
        when (error) {
            is AuthError.CodeMismatch -> {
                codeError = "確認コードが正しくありません"
            }

            is AuthError.CodeExpired -> {
                codeError = "確認コードの有効期限が切れました。再送信してください"
            }

            is AuthError.Network -> {
                _effect.emit(VerificationEffect.ShowSnackbar("ネットワークエラーが発生しました"))
            }

            is AuthError.TooManyRequests -> {
                _effect.emit(VerificationEffect.ShowSnackbar("リクエストが多すぎます。しばらくしてから再試行してください"))
            }

            else -> {
                _effect.emit(VerificationEffect.ShowSnackbar("エラーが発生しました"))
            }
        }
    }

    private suspend fun handleResendError(error: AuthError) {
        when (error) {
            is AuthError.Network -> {
                _effect.emit(VerificationEffect.ShowSnackbar("ネットワークエラーが発生しました"))
            }

            is AuthError.TooManyRequests -> {
                _effect.emit(VerificationEffect.ShowSnackbar("リクエストが多すぎます。しばらくしてから再試行してください"))
            }

            else -> {
                _effect.emit(VerificationEffect.ShowSnackbar("再送信に失敗しました"))
            }
        }
    }

    private fun startResendTimer() {
        resendTimerSeconds = RESEND_COOLDOWN_SECONDS
        viewModelScope.launch {
            while (resendTimerSeconds > 0) {
                delay(1_000L)
                resendTimerSeconds--
            }
        }
    }

    companion object {
        const val RESEND_COOLDOWN_SECONDS = 60

        private const val ARG_EMAIL = "email"
    }
}
