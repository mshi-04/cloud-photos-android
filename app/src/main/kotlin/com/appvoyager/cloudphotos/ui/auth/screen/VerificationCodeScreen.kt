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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.appvoyager.cloudphotos.domain.auth.valueobject.Email
import com.appvoyager.cloudphotos.ui.auth.component.CodeInputRow
import com.appvoyager.cloudphotos.ui.auth.component.LoadingOverlay
import com.appvoyager.cloudphotos.ui.auth.effect.VerificationEffect
import com.appvoyager.cloudphotos.ui.auth.viewmodel.VerificationCodeViewModel
import com.appvoyager.cloudphotos.ui.theme.CloudPhotosTheme
import com.appvoyager.cloudphotos.ui.util.StringUtils

@Composable
fun VerificationCodeScreen(
    viewModel: VerificationCodeViewModel = hiltViewModel(),
    onNavigateToHome: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val resources = LocalResources.current

    LaunchedEffect(Unit) {
        viewModel.startTimer()
        viewModel.effect.collect { effect ->
            when (effect) {
                is VerificationEffect.NavigateToHome -> onNavigateToHome()
                is VerificationEffect.ShowSnackbar -> {
                    snackbarHostState.showSnackbar(resources.getString(effect.messageResId))
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
            VerificationContent(
                email = viewModel.email,
                codes = uiState.codes,
                codeError = uiState.codeError?.let { stringResource(it) },
                isCodeComplete = viewModel.isCodeComplete,
                isLoading = uiState.isLoading,
                resendTimerSeconds = uiState.resendTimerSeconds,
                isResendEnabled = viewModel.isResendEnabled,
                onCodeChanged = { index, value -> viewModel.onCodeChanged(index, value) },
                onVerify = { viewModel.onVerify() },
                onResend = { viewModel.onResend() }
            )

            if (uiState.isLoading) {
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
    val maskedEmail = runCatching { StringUtils.maskEmail(Email.of(email)) }
        .getOrElse { "" }

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
                enabled = isResendEnabled
            ) {
                Text(
                    text = if (resendTimerSeconds > 0) {
                        "再送信（$resendTimerSeconds）"
                    } else {
                        "再送信"
                    }
                )
            }

            Button(
                onClick = onVerify,
                enabled = isCodeComplete && !isLoading,
                shape = RoundedCornerShape(24.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary
                )
            ) {
                Text("認証")
            }
        }

        Spacer(modifier = Modifier.height(80.dp))
    }
}

@Preview(showBackground = true)
@Composable
private fun VerificationContentPreview() {
    CloudPhotosTheme {
        VerificationContent(
            email = "example@email.com",
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
            email = "example@email.com",
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
            email = "example@email.com",
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
