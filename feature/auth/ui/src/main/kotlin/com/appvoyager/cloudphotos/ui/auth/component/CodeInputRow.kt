package com.appvoyager.cloudphotos.ui.auth.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.unit.dp

private const val CODE_LENGTH = 6

@Composable
fun CodeInputRow(
    codes: List<String>,
    isError: Boolean,
    onCodeChanged: (Int, String) -> Unit
) {
    require(codes.size == CODE_LENGTH) { "codes must have exactly $CODE_LENGTH elements" }
    val focusRequesters = remember { List(CODE_LENGTH) { FocusRequester() } }
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