package com.appvoyager.cloudphotos.ui.media.component

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.appvoyager.cloudphotos.R
import com.appvoyager.cloudphotos.domain.settings.valueobject.GridColumnCount
import com.appvoyager.cloudphotos.ui.theme.CloudPhotosTheme
import kotlin.math.roundToInt

@Composable
fun GridColumnSettingsDialog(
    currentColumnCount: Int,
    onColumnCountChanged: (Int) -> Unit,
    onDismiss: () -> Unit
) {

    var sliderValue by remember(currentColumnCount) { mutableIntStateOf(currentColumnCount) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(text = stringResource(R.string.settings_grid_column_title))
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = stringResource(R.string.settings_grid_column_value, sliderValue),
                    style = MaterialTheme.typography.headlineMedium
                )

                Spacer(modifier = Modifier.height(16.dp))

                Slider(
                    value = sliderValue.toFloat(),
                    onValueChange = { sliderValue = it.roundToInt() },
                    valueRange = GridColumnCount.MIN.toFloat()..GridColumnCount.MAX.toFloat(),
                    steps = GridColumnCount.MAX - GridColumnCount.MIN - 1
                )
            }
        },
        confirmButton = {
            TextButton(onClick = {
                onColumnCountChanged(sliderValue)
                onDismiss()
            }) {
                Text(stringResource(R.string.settings_confirm))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.settings_cancel))
            }
        }
    )
}

@Preview(showBackground = true)
@Composable
private fun GridColumnSettingsDialogPreview() {
    CloudPhotosTheme {
        GridColumnSettingsDialog(
            currentColumnCount = 3,
            onColumnCountChanged = {},
            onDismiss = {}
        )
    }
}