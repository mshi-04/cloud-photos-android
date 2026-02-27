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
import com.appvoyager.cloudphotos.domain.auth.request.ConfirmResetPasswordRequest
import com.appvoyager.cloudphotos.domain.auth.request.ResetPasswordRequest
import com.appvoyager.cloudphotos.domain.auth.usecase.ConfirmResetPasswordUseCase
import com.appvoyager.cloudphotos.domain.auth.usecase.ResetPasswordUseCase
import com.appvoyager.cloudphotos.domain.auth.valueobject.ConfirmationCode
import com.appvoyager.cloudphotos.domain.auth.valueobject.Email
import com.appvoyager.cloudphotos.domain.auth.valueobject.Password
import com.appvoyager.cloudphotos.ui.auth.effect.ResetPasswordCodeEffect
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
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

    var codes by mutableStateOf(List(6) { "" })
        private set

    var newPassword by mutableStateOf("")
        private set

    var isNewPasswordVisible by mutableStateOf(false)
        private set

    var isLoading by mutableStateOf(false)
        private set

    var codeError by mutableStateOf<String?>(null)
        private set

    var passwordError by mutableStateOf<String?>(null)
        private set

    var resendTimerSeconds by mutableIntStateOf(RESEND_COOLDOWN_SECONDS)
        private set

    private var isTimerStarted = false

    private val _effect = MutableSharedFlow<ResetPasswordCodeEffect>(extraBufferCapacity = 1)
    val effect: SharedFlow<ResetPasswordCodeEffect> = _effect.asSharedFlow()

    val isResendEnabled: Boolean
        get() = resendTimerSeconds <= 0 && !isLoading

    val isCodeComplete: Boolean
        get() = codes.all { it.length == 1 && it[0].isDigit() }

    val isFormValid: Boolean
        get() = isCodeComplete && newPassword.length >= 8

    fun startTimerIfNeeded() {
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
            return
        }

        val newCodes = codes.toMutableList()
        newCodes[index] = digits.take(1)
        codes = newCodes
    }

    fun onNewPasswordChanged(value: String) {
        newPassword = value
        passwordError = null
    }

    fun onToggleNewPasswordVisibility() {
        isNewPasswordVisible = !isNewPasswordVisible
    }

    fun onClearCodes() {
        codes = List(6) { "" }
        codeError = null
    }

    fun onConfirm() {
        if (!validateForm() || isLoading) return
        val fullCode = codes.joinToString("")

        viewModelScope.launch {
            isLoading = true
            try {
                val email = Email.of(email)
                val code = ConfirmationCode.of(fullCode)
                val password = Password.of(newPassword)
                val result = confirmResetPasswordUseCase(
                    ConfirmResetPasswordRequest(email, code, password)
                )

                when (result) {
                    is AuthResult.Success -> {
                        _effect.emit(ResetPasswordCodeEffect.ShowSnackbar("パスワードをリセットしました"))
                        _effect.emit(ResetPasswordCodeEffect.NavigateBackToLogin)
                    }

                    is AuthResult.Error -> handleConfirmError(result.error)
                }
            } catch (_: IllegalArgumentException) {
                codeError = "入力内容を確認してください"
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
                val result = resetPasswordUseCase(ResetPasswordRequest(email))

                when (result) {
                    is AuthResult.Success -> {
                        _effect.emit(ResetPasswordCodeEffect.ShowSnackbar("確認コードを再送信しました"))
                        startResendTimer()
                    }

                    is AuthResult.Error -> handleResendError(result.error)
                }
            } catch (_: IllegalArgumentException) {
                _effect.emit(ResetPasswordCodeEffect.ShowSnackbar("エラーが発生しました"))
            } finally {
                isLoading = false
            }
        }
    }

    private fun validateForm(): Boolean {
        var valid = true
        if (!isCodeComplete) {
            codeError = "確認コードを入力してください"
            valid = false
        }
        if (newPassword.length < 8) {
            passwordError = "パスワードは8文字以上で入力してください"
            valid = false
        }
        return valid
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
                _effect.emit(ResetPasswordCodeEffect.ShowSnackbar("ネットワークエラーが発生しました"))
            }

            is AuthError.TooManyRequests -> {
                _effect.emit(ResetPasswordCodeEffect.ShowSnackbar("リクエストが多すぎます。しばらくしてから再試行してください"))
            }

            else -> {
                _effect.emit(ResetPasswordCodeEffect.ShowSnackbar("エラーが発生しました"))
            }
        }
    }

    private suspend fun handleResendError(error: AuthError) {
        when (error) {
            is AuthError.Network -> {
                _effect.emit(ResetPasswordCodeEffect.ShowSnackbar("ネットワークエラーが発生しました"))
            }

            is AuthError.TooManyRequests -> {
                _effect.emit(ResetPasswordCodeEffect.ShowSnackbar("リクエストが多すぎます。しばらくしてから再試行してください"))
            }

            else -> {
                _effect.emit(ResetPasswordCodeEffect.ShowSnackbar("再送信に失敗しました"))
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
