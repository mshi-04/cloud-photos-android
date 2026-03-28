package com.appvoyager.cloudphotos.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.appvoyager.cloudphotos.R
import com.appvoyager.cloudphotos.ui.auth.component.LoadingOverlay
import com.appvoyager.cloudphotos.ui.theme.CloudPhotosTheme

@Composable
fun SessionCheckErrorScreen(isRetrying: Boolean, onRetry: () -> Unit) {
    SessionCheckErrorContent(isRetrying = isRetrying, onRetry = onRetry)
}

@Composable
private fun SessionCheckErrorContent(isRetrying: Boolean, onRetry: () -> Unit) {
    BackHandler(enabled = isRetrying) {}
    Scaffold { innerPadding ->
        Box(modifier = Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = stringResource(R.string.error_session_check),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.error,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(16.dp))
                Button(
                    onClick = onRetry,
                    enabled = !isRetrying
                ) {
                    Text(text = stringResource(R.string.retry))
                }
            }
            if (isRetrying) {
                LoadingOverlay()
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun SessionCheckErrorContentPreview() {
    CloudPhotosTheme {
        SessionCheckErrorContent(isRetrying = false, onRetry = {})
    }
}

@Preview(showBackground = true)
@Composable
private fun SessionCheckErrorContentRetryingPreview() {
    CloudPhotosTheme {
        SessionCheckErrorContent(isRetrying = true, onRetry = {})
    }
}
