package com.appvoyager.cloudphotos.ui.auth.viewmodel

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
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
import com.appvoyager.cloudphotos.ui.auth.effect.LoginEffect
import com.appvoyager.cloudphotos.ui.util.ValidationUtils
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class LoginViewModel @Inject constructor(
    private val signInUseCase: SignInUseCase,
    private val signUpUseCase: SignUpUseCase
) : ViewModel() {

    var email by mutableStateOf("")
        private set

    var password by mutableStateOf("")
        private set

    var isPasswordVisible by mutableStateOf(false)
        private set

    var isLoading by mutableStateOf(false)
        private set

    var emailError by mutableStateOf<String?>(null)
        private set

    var passwordError by mutableStateOf<String?>(null)
        private set

    private val _effect = MutableSharedFlow<LoginEffect>()
    val effect: SharedFlow<LoginEffect> = _effect.asSharedFlow()

    val isFormValid: Boolean
        get() = email.isNotBlank() && ValidationUtils.isValidEmailFormat(email) && password.length >= 8

    fun onEmailChanged(value: String) {
        email = value
        emailError = null
    }

    fun onPasswordChanged(value: String) {
        password = value
        passwordError = null
    }

    fun onTogglePasswordVisibility() {
        isPasswordVisible = !isPasswordVisible
    }

    fun onClearEmail() {
        email = ""
        emailError = null
    }

    fun onClearPassword() {
        password = ""
        passwordError = null
    }

    fun onForgotPassword() {
        viewModelScope.launch {
            _effect.emit(LoginEffect.NavigateToForgotPassword)
        }
    }

    fun onSignIn() {
        if (!validateForm()) return

        viewModelScope.launch {
            isLoading = true
            try {
                val email = Email.of(email)
                val password = Password.of(password)
                val result = signInUseCase(SignInRequest(email, password))
                handleSignInResult(result)
            } catch (_: IllegalArgumentException) {
                emailError = "入力内容を確認してください"
            } finally {
                isLoading = false
            }
        }
    }

    fun onSignUp() {
        if (!validateForm()) return

        viewModelScope.launch {
            isLoading = true
            try {
                val email = Email.of(email)
                val password = Password.of(password)
                val result = signUpUseCase(SignUpRequest(email, password))
                handleSignUpResult(result)
            } catch (_: IllegalArgumentException) {
                emailError = "入力内容を確認してください"
            } finally {
                isLoading = false
            }
        }
    }

    private suspend fun handleSignInResult(result: AuthResult<SignInState>) {
        when (result) {
            is AuthResult.Success -> {
                when (result.value) {
                    is SignInState.SignedIn -> _effect.emit(LoginEffect.NavigateToHome)
                    else -> _effect.emit(LoginEffect.ShowSnackbar("追加の認証ステップが必要です"))
                }
            }

            is AuthResult.Error -> handleAuthError(result.error, isSignUp = false)
        }
    }

    private suspend fun handleSignUpResult(result: AuthResult<Unit>) {
        when (result) {
            is AuthResult.Success -> _effect.emit(LoginEffect.NavigateToVerification(Email.of(email)))
            is AuthResult.Error -> handleAuthError(result.error, isSignUp = true)
        }
    }

    private suspend fun handleAuthError(error: AuthError, isSignUp: Boolean) {
        when (error) {
            is AuthError.InvalidCredentials -> {
                passwordError = "メールアドレスまたはパスワードが正しくありません"
            }

            is AuthError.UserNotConfirmed -> {
                _effect.emit(LoginEffect.NavigateToVerification(Email.of(email)))
            }

            is AuthError.UsernameAlreadyExists -> {
                emailError = "このメールアドレスは既に登録されています"
            }

            is AuthError.Network -> {
                _effect.emit(LoginEffect.ShowSnackbar("ネットワークエラーが発生しました"))
            }

            is AuthError.TooManyRequests -> {
                _effect.emit(LoginEffect.ShowSnackbar("リクエストが多すぎます。しばらくしてから再試行してください"))
            }

            else -> {
                _effect.emit(LoginEffect.ShowSnackbar("エラーが発生しました"))
            }
        }
    }

    private fun validateForm(): Boolean {
        var valid = true
        if (email.isBlank() || !ValidationUtils.isValidEmailFormat(email)) {
            emailError = "有効なメールアドレスを入力してください"
            valid = false
        }
        if (password.length < 8) {
            passwordError = "パスワードは8文字以上で入力してください"
            valid = false
        }
        return valid
    }

}
