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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.appvoyager.cloudphotos.ui.auth.effect.VerificationEffect
import com.appvoyager.cloudphotos.ui.auth.viewmodel.VerificationCodeViewModel
import com.appvoyager.cloudphotos.ui.theme.CloudPhotosTheme

@Composable
fun VerificationCodeScreen(
    viewModel: VerificationCodeViewModel = hiltViewModel(),
    onNavigateToHome: () -> Unit
) {
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(Unit) {
        viewModel.startTimer()
        viewModel.effect.collect { effect ->
            when (effect) {
                is VerificationEffect.NavigateToHome -> onNavigateToHome()
                is VerificationEffect.ShowSnackbar -> {
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
            VerificationContent(
                email = viewModel.email,
                codes = viewModel.codes,
                codeError = viewModel.codeError,
                isCodeComplete = viewModel.isCodeComplete,
                isLoading = viewModel.isLoading,
                resendTimerSeconds = viewModel.resendTimerSeconds,
                isResendEnabled = viewModel.isResendEnabled,
                onCodeChanged = { index, value -> viewModel.onCodeChanged(index, value) },
                onVerify = { viewModel.onVerify() },
                onResend = { viewModel.onResend() }
            )

            if (viewModel.isLoading) {
                LoadingOverlay()
            }
        }
    }
}

@Composable
private fun VerificationContent(
    email: String,
    codes: List<String>,
    codeError: String?,
    isCodeComplete: Boolean,
    isLoading: Boolean,
    resendTimerSeconds: Int,
    isResendEnabled: Boolean,
    onCodeChanged: (Int, String) -> Unit,
    onVerify: () -> Unit,
    onResend: () -> Unit
) {
    val maskedEmail = maskEmail(email)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .imePadding()
            .padding(horizontal = 24.dp),
    ) {
        Spacer(modifier = Modifier.height(80.dp))

        Text(
            text = "登録メールアドレス宛てにパスコードを送信しました。\nご確認いただきご入力ください。",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurface
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = maskedEmail,
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

        Spacer(modifier = Modifier.height(32.dp))


        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterHorizontally),
            verticalAlignment = Alignment.CenterVertically
        ) {
            TextButton(
                onClick = onResend,
                enabled = isResendEnabled,
            ) {
                Text(
                    text = if (resendTimerSeconds > 0) {
                        "再送信（$resendTimerSeconds）"
                    } else {
                        "再送信"
                    },
                )
            }

            Button(
                onClick = onVerify,
                enabled = isCodeComplete && !isLoading,
                shape = RoundedCornerShape(24.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary
                ),
            ) {
                Text("認証")
            }
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
                        textAlign = TextAlign.Center,
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
private fun VerificationContentPreview() {
    CloudPhotosTheme {
        VerificationContent(
            email = "exsample@email.com",
            codes = listOf("", "", "", "", "", ""),
            codeError = null,
            isCodeComplete = false,
            isLoading = false,
            resendTimerSeconds = 45,
            isResendEnabled = false,
            onCodeChanged = { _, _ -> },
            onVerify = {},
            onResend = {}
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun VerificationContentWithCodePreview() {
    CloudPhotosTheme {
        VerificationContent(
            email = "exsample@email.com",
            codes = listOf("1", "2", "3", "4", "5", ""),
            codeError = null,
            isCodeComplete = false,
            isLoading = false,
            resendTimerSeconds = 0,
            isResendEnabled = true,
            onCodeChanged = { _, _ -> },
            onVerify = {},
            onResend = {}
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun VerificationContentWithErrorPreview() {
    CloudPhotosTheme {
        VerificationContent(
            email = "exsample@email.com",
            codes = listOf("1", "2", "3", "4", "5", "6"),
            codeError = "確認コードが正しくありません",
            isCodeComplete = true,
            isLoading = false,
            resendTimerSeconds = 30,
            isResendEnabled = false,
            onCodeChanged = { _, _ -> },
            onVerify = {},
            onResend = {}
        )
    }
}
