package com.appvoyager.cloudphotos.ui.auth.screen

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.material.icons.filled.Clear
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
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.appvoyager.cloudphotos.ui.auth.component.LoadingOverlay
import com.appvoyager.cloudphotos.ui.auth.effect.ForgotPasswordEffect
import com.appvoyager.cloudphotos.ui.auth.viewmodel.ForgotPasswordViewModel
import com.appvoyager.cloudphotos.ui.theme.CloudPhotosTheme

@Composable
fun ForgotPasswordScreen(
    viewModel: ForgotPasswordViewModel = hiltViewModel(),
    onNavigateToResetCode: (String) -> Unit,
    onNavigateBack: () -> Unit
) {
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(Unit) {
        viewModel.effect.collect { effect ->
            when (effect) {
                is ForgotPasswordEffect.NavigateToResetCode -> {
                    onNavigateToResetCode(effect.email.value)
                }

                is ForgotPasswordEffect.NavigateBackToLogin -> {
                    onNavigateBack()
                }

                is ForgotPasswordEffect.ShowSnackbar -> {
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
            ForgotPasswordContent(
                email = viewModel.email,
                emailError = viewModel.emailError,
                isFormValid = viewModel.isFormValid,
                isLoading = viewModel.isLoading,
                onEmailChanged = { viewModel.onEmailChanged(it) },
                onClearEmail = { viewModel.onClearEmail() },
                onSubmit = { viewModel.onSubmit() },
                onBack = onNavigateBack
            )

            if (viewModel.isLoading) {
                LoadingOverlay()
            }
        }
    }
}

@Composable
private fun ForgotPasswordContent(
    email: String,
    emailError: String?,
    isFormValid: Boolean,
    isLoading: Boolean,
    onEmailChanged: (String) -> Unit,
    onClearEmail: () -> Unit,
    onSubmit: () -> Unit,
    onBack: () -> Unit
) {
    val focusManager = LocalFocusManager.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(rememberScrollState())
            .imePadding()
            .padding(horizontal = 24.dp),
        verticalArrangement = Arrangement.Center,
    ) {
        Spacer(modifier = Modifier.height(80.dp))

        Text(
            text = "パスワードのリセット",
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.onSurface
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "登録済みのメールアドレスを入力してください。\nパスワードリセット用の確認コードを送信します。",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(32.dp))

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
                imeAction = ImeAction.Done,
            ),
            keyboardActions = KeyboardActions(
                onDone = {
                    focusManager.clearFocus()
                    if (isFormValid && !isLoading) onSubmit()
                }
            ),
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(24.dp))

        Button(
            onClick = onSubmit,
            enabled = isFormValid && !isLoading,
            shape = RoundedCornerShape(24.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primary,
            ),
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("確認コードを送信")
        }

        Spacer(modifier = Modifier.height(16.dp))

        TextButton(
            onClick = onBack,
            modifier = Modifier.align(Alignment.CenterHorizontally),
        ) {
            Text("ログイン画面に戻る")
        }

        Spacer(modifier = Modifier.height(80.dp))
    }
}

@Preview(showBackground = true)
@Composable
private fun ForgotPasswordContentPreview() {
    CloudPhotosTheme {
        ForgotPasswordContent(
            email = "",
            emailError = null,
            isFormValid = false,
            isLoading = false,
            onEmailChanged = {},
            onClearEmail = {},
            onSubmit = {},
            onBack = {}
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun ForgotPasswordContentWithEmailPreview() {
    CloudPhotosTheme {
        ForgotPasswordContent(
            email = "test@example.com",
            emailError = null,
            isFormValid = true,
            isLoading = false,
            onEmailChanged = {},
            onClearEmail = {},
            onSubmit = {},
            onBack = {}
        )
    }
}
