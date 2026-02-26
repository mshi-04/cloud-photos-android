package com.appvoyager.cloudphotos.ui.auth.viewmodel

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.appvoyager.cloudphotos.domain.auth.model.AuthError
import com.appvoyager.cloudphotos.domain.auth.model.AuthResult
import com.appvoyager.cloudphotos.domain.auth.request.ResetPasswordRequest
import com.appvoyager.cloudphotos.domain.auth.usecase.ResetPasswordUseCase
import com.appvoyager.cloudphotos.domain.auth.valueobject.Email
import com.appvoyager.cloudphotos.ui.auth.effect.ForgotPasswordEffect
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ForgotPasswordViewModel @Inject constructor(
    private val resetPasswordUseCase: ResetPasswordUseCase
) : ViewModel() {

    var email by mutableStateOf("")
        private set

    var isLoading by mutableStateOf(false)
        private set

    var emailError by mutableStateOf<String?>(null)
        private set

    private val _effect = MutableSharedFlow<ForgotPasswordEffect>()
    val effect: SharedFlow<ForgotPasswordEffect> = _effect.asSharedFlow()

    val isFormValid: Boolean
        get() = email.isNotBlank() && isValidEmailFormat(email)

    fun onEmailChanged(value: String) {
        email = value
        emailError = null
    }

    fun onClearEmail() {
        email = ""
        emailError = null
    }

    fun onSubmit() {
        if (!isFormValid || isLoading) return

        viewModelScope.launch {
            isLoading = true
            try {
                val emailVo = Email.of(email)
                val result = resetPasswordUseCase(ResetPasswordRequest(emailVo))

                when (result) {
                    is AuthResult.Success -> {
                        _effect.emit(ForgotPasswordEffect.NavigateToResetCode(email))
                    }

                    is AuthResult.Error -> handleError(result.error)
                }
            } catch (_: IllegalArgumentException) {
                emailError = "有効なメールアドレスを入力してください"
            } finally {
                isLoading = false
            }
        }
    }

    private suspend fun handleError(error: AuthError) {
        when (error) {
            is AuthError.Network -> {
                _effect.emit(ForgotPasswordEffect.ShowSnackbar("ネットワークエラーが発生しました"))
            }

            is AuthError.TooManyRequests -> {
                _effect.emit(ForgotPasswordEffect.ShowSnackbar("リクエストが多すぎます。しばらくしてから再試行してください"))
            }

            else -> {
                _effect.emit(ForgotPasswordEffect.ShowSnackbar("エラーが発生しました"))
            }
        }
    }

    private fun isValidEmailFormat(email: String): Boolean =
        Regex("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$").matches(email.trim())
}
