package io.github.fabiann1809.reader.ui.auth

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import io.github.fabiann1809.reader.R
import io.github.fabiann1809.reader.data.auth.PasswordRule
import io.github.fabiann1809.reader.ui.components.readerTextFieldColors
import io.github.fabiann1809.reader.ui.components.readerTextFieldShape

@Composable
fun EmailField(value: String, onChange: (String) -> Unit, error: FieldError?, modifier: Modifier = Modifier) {
    OutlinedTextField(
        value = value,
        onValueChange = onChange,
        label = { Text(stringResource(R.string.auth_email)) },
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email, imeAction = ImeAction.Next),
        isError = error != null,
        supportingText = errorText(error),
        shape = readerTextFieldShape,
        colors = readerTextFieldColors(),
        modifier = modifier.fillMaxWidth(),
    )
}

/** A password field with a "Mostrar/Ocultar" button. [onDone] runs from the keyboard's last key. */
@Composable
fun PasswordField(
    value: String,
    onChange: (String) -> Unit,
    @StringRes label: Int,
    error: FieldError?,
    imeAction: ImeAction,
    onDone: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var visible by remember { mutableStateOf(false) }
    val toggleText = stringResource(if (visible) R.string.auth_hide_password else R.string.auth_show_password)
    OutlinedTextField(
        value = value,
        onValueChange = onChange,
        label = { Text(stringResource(label)) },
        singleLine = true,
        visualTransformation = if (visible) VisualTransformation.None else PasswordVisualTransformation(),
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = imeAction),
        keyboardActions = KeyboardActions(onDone = { onDone() }),
        isError = error != null,
        supportingText = errorText(error),
        trailingIcon = {
            IconButton(onClick = { visible = !visible }) {
                Icon(
                    painter = painterResource(if (visible) R.drawable.ic_eye_slash else R.drawable.ic_eye),
                    contentDescription = toggleText,
                )
            }
        },
        shape = readerTextFieldShape,
        colors = readerTextFieldColors(),
        modifier = modifier.fillMaxWidth(),
    )
}

/** What a new password needs, each line ticked as it is met. */
@Composable
fun PasswordChecklist(checks: List<PasswordCheck>, modifier: Modifier = Modifier) {
    Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(
            text = stringResource(R.string.auth_requirements_title),
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        checks.forEach { check ->
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = if (check.passed) "✓" else "○",
                    color = if (check.passed) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    text = stringResource(check.rule.labelRes()),
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (check.passed) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@StringRes
private fun PasswordRule.labelRes(): Int = when (this) {
    PasswordRule.MIN_LENGTH -> R.string.auth_rule_min_length
    PasswordRule.HAS_UPPERCASE -> R.string.auth_rule_uppercase
    PasswordRule.HAS_LOWERCASE -> R.string.auth_rule_lowercase
    PasswordRule.HAS_DIGIT -> R.string.auth_rule_digit
    PasswordRule.HAS_SYMBOL -> R.string.auth_rule_symbol
}

/** The red text under a field, or nothing. */
private fun errorText(error: FieldError?): (@Composable () -> Unit)? =
    error?.let { { Text(stringResource(fieldErrorMessageRes(it))) } }
