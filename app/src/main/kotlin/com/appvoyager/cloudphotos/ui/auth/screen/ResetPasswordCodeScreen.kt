package com.appvoyager.cloudphotos.ui.auth.screen

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
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
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.appvoyager.cloudphotos.ui.auth.effect.ResetPasswordCodeEffect
import com.appvoyager.cloudphotos.ui.auth.viewmodel.ResetPasswordCodeViewModel
import com.appvoyager.cloudphotos.ui.theme.CloudPhotosTheme

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
    val maskedEmail = maskEmail(email)
    val focusManager = LocalFocusManager.current

    Column(
        modifier = Modifier
            .fillMaxSize()
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

@Composable
private fun CodeInputRow(
    codes: List<String>,
    isError: Boolean,
    onCodeChanged: (Int, String) -> Unit
) {
    val focusRequesters = remember { List(6) { FocusRequester() } }
    val keyboardController = LocalSoftwareKeyboardController.current

    LaunchedEffect(Unit) {
        focusRequesters[0].requestFocus()
    }

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally)
    ) {
        codes.forEachIndexed { index, code ->
            CodeInputBox(
                value = code,
                isError = isError,
                focusRequester = focusRequesters[index],
                onValueChange = { newValue ->
                    val digits = newValue.filter { it.isDigit() }
                    if (digits.length > 1) {
                        onCodeChanged(index, digits)
                        val targetIndex = (index + digits.length).coerceAtMost(5)
                        focusRequesters[targetIndex].requestFocus()
                        if (targetIndex == 5) {
                            keyboardController?.hide()
                        }
                        return@CodeInputBox
                    }

                    onCodeChanged(index, digits.take(1))
                    if (digits.isNotEmpty() && index < 5) {
                        focusRequesters[index + 1].requestFocus()
                    }
                    if (digits.isNotEmpty() && index == 5) {
                        keyboardController?.hide()
                    }
                },
                onBackspace = {
                    if (code.isEmpty() && index > 0) {
                        onCodeChanged(index - 1, "")
                        focusRequesters[index - 1].requestFocus()
                    } else {
                        onCodeChanged(index, "")
                    }
                }
            )
        }
    }
}

@Composable
private fun CodeInputBox(
    value: String,
    isError: Boolean,
    focusRequester: FocusRequester,
    onValueChange: (String) -> Unit,
    onBackspace: () -> Unit
) {
    val borderColor = when {
        isError -> MaterialTheme.colorScheme.error
        value.isNotEmpty() -> MaterialTheme.colorScheme.primary
        else -> MaterialTheme.colorScheme.outline
    }

    BasicTextField(
        value = value,
        onValueChange = onValueChange,
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
        modifier = Modifier
            .size(48.dp)
            .focusRequester(focusRequester)
            .onKeyEvent { event ->
                if (event.key == Key.Backspace) {
                    onBackspace()
                    true
                } else {
                    false
                }
            },
        decorationBox = { innerTextField ->
            Box(
                modifier = Modifier
                    .border(
                        width = if (value.isNotEmpty() || isError) 2.dp else 1.dp,
                        color = borderColor,
                        shape = RoundedCornerShape(8.dp),
                    )
                    .background(Color.White, RoundedCornerShape(8.dp))
                    .padding(4.dp),
                contentAlignment = Alignment.Center
            ) {
                if (value.isEmpty()) {
                    Text(
                        text = "",
                        style = MaterialTheme.typography.headlineSmall,
                        textAlign = TextAlign.Center
                    )
                }
                Box(contentAlignment = Alignment.Center) {
                    innerTextField()
                }
            }
        },
        textStyle = MaterialTheme.typography.headlineSmall.copy(
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurface
        )
    )
}

@Composable
private fun LoadingOverlay() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.4f)),
        contentAlignment = Alignment.Center
    ) {
        CircularProgressIndicator(
            color = MaterialTheme.colorScheme.primary
        )
    }
}

private fun maskEmail(email: String): String {
    val parts = email.split("@")
    if (parts.size != 2) return email
    val local = parts[0]
    val domain = parts[1]
    val maskedLocal = if (local.length <= 2) {
        local.first() + "***"
    } else {
        local.take(2) + "*".repeat((local.length - 2).coerceAtMost(5))
    }
    return "$maskedLocal@$domain"
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
