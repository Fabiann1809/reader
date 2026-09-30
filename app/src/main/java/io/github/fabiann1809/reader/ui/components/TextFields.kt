package io.github.fabiann1809.reader.ui.components

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.TextFieldColors
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Shape

// Text field style from the design (7.19): filled surface-container, soft border, 12 dp corners,
// primary border on focus. Pass these to every OutlinedTextField.

val readerTextFieldShape: Shape
    @Composable get() = MaterialTheme.shapes.medium

@Composable
fun readerTextFieldColors(): TextFieldColors {
    val container = MaterialTheme.colorScheme.surfaceContainer
    return OutlinedTextFieldDefaults.colors(
        focusedContainerColor = container,
        unfocusedContainerColor = container,
        disabledContainerColor = container,
        errorContainerColor = container,
        unfocusedBorderColor = MaterialTheme.colorScheme.outline,
        focusedBorderColor = MaterialTheme.colorScheme.primary,
    )
}
