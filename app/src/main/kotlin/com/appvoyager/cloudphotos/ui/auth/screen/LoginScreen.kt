package com.appvoyager.cloudphotos.ui.auth.screen

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.appvoyager.cloudphotos.ui.auth.component.LoadingOverlay
import com.appvoyager.cloudphotos.ui.auth.effect.LoginEffect
import com.appvoyager.cloudphotos.ui.auth.viewmodel.LoginViewModel
import com.appvoyager.cloudphotos.ui.theme.CloudPhotosTheme

@Composable
fun LoginScreen(
    viewModel: LoginViewModel = hiltViewModel(),
    onNavigateToVerification: (String) -> Unit,
    onNavigateToHome: () -> Unit,
    onNavigateToForgotPassword: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val latestResources = rememberUpdatedState(LocalResources.current)

    val latestOnNavigateToVerification = rememberUpdatedState(onNavigateToVerification)
    val latestOnNavigateToHome = rememberUpdatedState(onNavigateToHome)
    val latestOnNavigateToForgotPassword = rememberUpdatedState(onNavigateToForgotPassword)

    LaunchedEffect(Unit) {
        viewModel.effect.collect { effect ->
            when (effect) {
                is LoginEffect.NavigateToVerification -> {
                    latestOnNavigateToVerification.value(effect.email.value)
                }

                is LoginEffect.NavigateToHome -> {
                    latestOnNavigateToHome.value()
                }

                is LoginEffect.NavigateToForgotPassword -> {
                    latestOnNavigateToForgotPassword.value()
                }

                is LoginEffect.ShowSnackbar -> {
                    snackbarHostState.showSnackbar(latestResources.value.getString(effect.messageResId))
                }
            }
        }
    }

    BackHandler(enabled = uiState.isLoading) { }

    Scaffold(
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            LoginContent(
                email = uiState.email,
                password = uiState.password,
                isPasswordVisible = uiState.isPasswordVisible,
                emailError = uiState.emailError?.let { stringResource(it) },
                passwordError = uiState.passwordError?.let { stringResource(it) },
                isFormValid = viewModel.isFormValid,
                isLoading = uiState.isLoading,
                onEmailChanged = { viewModel.onEmailChanged(it) },
                onPasswordChanged = { viewModel.onPasswordChanged(it) },
                onTogglePasswordVisibility = { viewModel.onTogglePasswordVisibility() },
                onClearEmail = { viewModel.onClearEmail() },
                onClearPassword = { viewModel.onClearPassword() },
                onSignIn = { viewModel.onSignIn() },
                onSignUp = { viewModel.onSignUp() },
                onForgotPassword = { viewModel.onForgotPassword() }
            )


            if (uiState.isLoading) {
                LoadingOverlay()
            }
        }
    }
}

@Composable
private fun LoginContent(
    email: String,
    password: String,
    isPasswordVisible: Boolean,
    emailError: String?,
    passwordError: String?,
    isFormValid: Boolean,
    isLoading: Boolean,
    onEmailChanged: (String) -> Unit,
    onPasswordChanged: (String) -> Unit,
    onTogglePasswordVisibility: () -> Unit,
    onClearEmail: () -> Unit,
    onClearPassword: () -> Unit,
    onSignIn: () -> Unit,
    onSignUp: () -> Unit,
    onForgotPassword: () -> Unit
) {
    val focusManager = LocalFocusManager.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(rememberScrollState())
            .imePadding()
            .padding(horizontal = 24.dp),
        verticalArrangement = Arrangement.Center
    ) {
        Spacer(modifier = Modifier.height(80.dp))


        OutlinedTextField(
            value = email,
            onValueChange = onEmailChanged,
            label = { Text("メールアドレス") },
            singleLine = true,
            isError = emailError != null,
            supportingText = emailError?.let { error -> { Text(error) } },
            trailingIcon = {
                if (email.isNotEmpty()) {
                    IconButton(onClick = onClearEmail) {
                        Icon(Icons.Default.Clear, contentDescription = "クリア")
                    }
                }
            },
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Email,
                imeAction = ImeAction.Next
            ),
            keyboardActions = KeyboardActions(
                onNext = { focusManager.moveFocus(FocusDirection.Down) }
            ),
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(16.dp))


        OutlinedTextField(
            value = password,
            onValueChange = onPasswordChanged,
            label = { Text("パスワード") },
            singleLine = true,
            isError = passwordError != null,
            supportingText = passwordError?.let { error -> { Text(error) } },
            visualTransformation = if (isPasswordVisible) {
                VisualTransformation.None
            } else {
                PasswordVisualTransformation()
            },
            trailingIcon = {
                Row {
                    if (password.isNotEmpty()) {
                        IconButton(onClick = onClearPassword) {
                            Icon(Icons.Default.Clear, contentDescription = "クリア")
                        }
                    }
                    IconButton(onClick = onTogglePasswordVisibility) {
                        Icon(
                            imageVector = if (isPasswordVisible) {
                                Icons.Default.VisibilityOff
                            } else {
                                Icons.Default.Visibility
                            },
                            contentDescription = if (isPasswordVisible) "パスワードを隠す" else "パスワードを表示"
                        )
                    }
                }
            },
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Password,
                imeAction = ImeAction.Done
            ),
            keyboardActions = KeyboardActions(
                onDone = {
                    focusManager.clearFocus()
                    if (isFormValid && !isLoading) onSignIn()
                }
            ),
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(4.dp))


        TextButton(
            onClick = onForgotPassword
        ) {
            Text(
                text = "パスワードを忘れた場合",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.primary
            )
        }

        Spacer(modifier = Modifier.height(24.dp))


        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterHorizontally),
            verticalAlignment = Alignment.CenterVertically
        ) {
            TextButton(
                onClick = onSignUp,
                enabled = isFormValid && !isLoading
            ) {
                Text("アカウントを作成")
            }

            Button(
                onClick = onSignIn,
                enabled = isFormValid && !isLoading,
                shape = RoundedCornerShape(24.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary
                )
            ) {
                Text("ログイン")
            }
        }

        Spacer(modifier = Modifier.height(80.dp))
    }
}

@Preview(showBackground = true)
@Composable
private fun LoginContentPreview() {
    CloudPhotosTheme {
        LoginContent(
            email = "example@email.com",
            password = "password",
            isPasswordVisible = false,
            emailError = null,
            passwordError = null,
            isFormValid = true,
            isLoading = false,
            onEmailChanged = {},
            onPasswordChanged = {},
            onTogglePasswordVisibility = {},
            onClearEmail = {},
            onClearPassword = {},
            onSignIn = {},
            onSignUp = {},
            onForgotPassword = {}
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun LoginContentWithErrorPreview() {
    CloudPhotosTheme {
        LoginContent(
            email = "test@example.com",
            password = "pass",
            isPasswordVisible = false,
            emailError = "このメールアドレスは既に登録されています",
            passwordError = "パスワードは8文字以上で入力してください",
            isFormValid = false,
            isLoading = false,
            onEmailChanged = {},
            onPasswordChanged = {},
            onTogglePasswordVisibility = {},
            onClearEmail = {},
            onClearPassword = {},
            onSignIn = {},
            onSignUp = {},
            onForgotPassword = {}
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun LoadingOverlayPreview() {
    CloudPhotosTheme {
        Box(modifier = Modifier.fillMaxSize()) {
            LoginContent(
                email = "test@example.com",
                password = "password",
                isPasswordVisible = false,
                emailError = null,
                passwordError = null,
                isFormValid = true,
                isLoading = true,
                onEmailChanged = {},
                onPasswordChanged = {},
                onTogglePasswordVisibility = {},
                onClearEmail = {},
                onClearPassword = {},
                onSignIn = {},
                onSignUp = {},
                onForgotPassword = {}
            )
            LoadingOverlay()
        }
    }
}
