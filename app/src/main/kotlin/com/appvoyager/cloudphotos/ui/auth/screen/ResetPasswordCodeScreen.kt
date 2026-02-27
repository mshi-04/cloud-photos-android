package com.appvoyager.cloudphotos.ui.auth.screen

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
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
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.appvoyager.cloudphotos.ui.auth.component.CodeInputRow
import com.appvoyager.cloudphotos.ui.auth.component.LoadingOverlay
import com.appvoyager.cloudphotos.ui.auth.effect.ResetPasswordCodeEffect
import com.appvoyager.cloudphotos.ui.auth.viewmodel.ResetPasswordCodeViewModel
import com.appvoyager.cloudphotos.ui.theme.CloudPhotosTheme
import com.appvoyager.cloudphotos.ui.util.StringUtils

@Composable
fun ResetPasswordCodeScreen(
    viewModel: ResetPasswordCodeViewModel = hiltViewModel(),
    onNavigateBackToLogin: () -> Unit
) {
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(Unit) {
        viewModel.startTimerIfNeeded()
        viewModel.effect.collect { effect ->
            when (effect) {
                is ResetPasswordCodeEffect.NavigateBackToLogin -> {
                    onNavigateBackToLogin()
                }

                is ResetPasswordCodeEffect.ShowSnackbar -> {
                    snackbarHostState.showSnackbar(effect.message)
                }
            }
        }
    }

    BackHandler(enabled = viewModel.isLoading) { }

    Scaffold(
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            ResetPasswordCodeContent(
                email = viewModel.email,
                codes = viewModel.codes,
                newPassword = viewModel.newPassword,
                isNewPasswordVisible = viewModel.isNewPasswordVisible,
                codeError = viewModel.codeError,
                passwordError = viewModel.passwordError,
                isFormValid = viewModel.isFormValid,
                isLoading = viewModel.isLoading,
                resendTimerSeconds = viewModel.resendTimerSeconds,
                isResendEnabled = viewModel.isResendEnabled,
                onCodeChanged = { index, value -> viewModel.onCodeChanged(index, value) },
                onNewPasswordChanged = { viewModel.onNewPasswordChanged(it) },
                onTogglePasswordVisibility = { viewModel.onToggleNewPasswordVisibility() },
                onConfirm = { viewModel.onConfirm() },
                onResend = { viewModel.onResend() }
            )

            if (viewModel.isLoading) {
                LoadingOverlay()
            }
        }
    }
}

@Composable
private fun ResetPasswordCodeContent(
    email: String,
    codes: List<String>,
    newPassword: String,
    isNewPasswordVisible: Boolean,
    codeError: String?,
    passwordError: String?,
    isFormValid: Boolean,
    isLoading: Boolean,
    resendTimerSeconds: Int,
    isResendEnabled: Boolean,
    onCodeChanged: (Int, String) -> Unit,
    onNewPasswordChanged: (String) -> Unit,
    onTogglePasswordVisibility: () -> Unit,
    onConfirm: () -> Unit,
    onResend: () -> Unit
) {
    val maskedEmail = StringUtils.maskEmail(email)
    val focusManager = LocalFocusManager.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(rememberScrollState())
            .imePadding()
            .padding(horizontal = 24.dp)
    ) {
        Spacer(modifier = Modifier.height(80.dp))

        Text(
            text = "パスワードのリセット",
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.onSurface
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "確認コードを $maskedEmail に送信しました。\nコードと新しいパスワードを入力してください。",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(32.dp))

        CodeInputRow(
            codes = codes,
            isError = codeError != null,
            onCodeChanged = onCodeChanged
        )

        if (codeError != null) {
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = codeError,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.padding(start = 4.dp)
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        OutlinedTextField(
            value = newPassword,
            onValueChange = onNewPasswordChanged,
            label = { Text("新しいパスワード") },
            singleLine = true,
            isError = passwordError != null,
            supportingText = passwordError?.let { error -> { Text(error) } },
            visualTransformation = if (isNewPasswordVisible) {
                VisualTransformation.None
            } else {
                PasswordVisualTransformation()
            },
            trailingIcon = {
                IconButton(onClick = onTogglePasswordVisibility) {
                    Icon(
                        imageVector = if (isNewPasswordVisible) {
                            Icons.Default.VisibilityOff
                        } else {
                            Icons.Default.Visibility
                        },
                        contentDescription = if (isNewPasswordVisible) "パスワードを隠す" else "パスワードを表示"
                    )
                }
            },
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Password,
                imeAction = ImeAction.Done
            ),
            keyboardActions = KeyboardActions(
                onDone = {
                    focusManager.clearFocus()
                    if (isFormValid && !isLoading) onConfirm()
                }
            ),
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(24.dp))

        Button(
            onClick = onConfirm,
            enabled = isFormValid && !isLoading,
            shape = RoundedCornerShape(24.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primary
            ),
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("パスワードをリセット")
        }

        Spacer(modifier = Modifier.height(16.dp))

        TextButton(
            onClick = onResend,
            enabled = isResendEnabled,
            modifier = Modifier.align(Alignment.CenterHorizontally)
        ) {
            Text(
                text = if (resendTimerSeconds > 0) {
                    "コードを再送信（$resendTimerSeconds）"
                } else {
                    "コードを再送信"
                }
            )
        }

        Spacer(modifier = Modifier.height(80.dp))
    }
}

@Preview(showBackground = true)
@Composable
private fun ResetPasswordCodeContentPreview() {
    CloudPhotosTheme {
        ResetPasswordCodeContent(
            email = "test@example.com",
            codes = listOf("", "", "", "", "", ""),
            newPassword = "",
            isNewPasswordVisible = false,
            codeError = null,
            passwordError = null,
            isFormValid = false,
            isLoading = false,
            resendTimerSeconds = 45,
            isResendEnabled = false,
            onCodeChanged = { _, _ -> },
            onNewPasswordChanged = {},
            onTogglePasswordVisibility = {},
            onConfirm = {},
            onResend = {}
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun ResetPasswordCodeContentWithErrorPreview() {
    CloudPhotosTheme {
        ResetPasswordCodeContent(
            email = "test@example.com",
            codes = listOf("1", "2", "3", "4", "5", "6"),
            newPassword = "pass",
            isNewPasswordVisible = false,
            codeError = "確認コードが正しくありません",
            passwordError = "パスワードは8文字以上で入力してください",
            isFormValid = false,
            isLoading = false,
            resendTimerSeconds = 0,
            isResendEnabled = true,
            onCodeChanged = { _, _ -> },
            onNewPasswordChanged = {},
            onTogglePasswordVisibility = {},
            onConfirm = {},
            onResend = {}
        )
    }
}
