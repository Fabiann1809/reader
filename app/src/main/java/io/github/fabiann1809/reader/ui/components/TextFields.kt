package io.github.fabiann1809.reader.ui.components

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.TextFieldColors
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape

// Text field of the redesign: filled with the chip color, 16 dp corners, no border until it
// gets the accent ring on focus. Pass these to every OutlinedTextField.

val readerTextFieldShape: Shape
    @Composable get() = MaterialTheme.shapes.small

@Composable
fun readerTextFieldColors(): TextFieldColors {
    val container = MaterialTheme.colorScheme.surfaceContainerHigh
    return OutlinedTextFieldDefaults.colors(
        focusedContainerColor = container,
        unfocusedContainerColor = container,
        disabledContainerColor = container,
        errorContainerColor = container,
        unfocusedBorderColor = Color.Transparent,
        disabledBorderColor = Color.Transparent,
        focusedBorderColor = MaterialTheme.colorScheme.primary,
    )
}
