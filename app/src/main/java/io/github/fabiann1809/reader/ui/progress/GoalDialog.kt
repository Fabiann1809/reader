package io.github.fabiann1809.reader.ui.progress

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import io.github.fabiann1809.reader.ui.components.ReaderFilterChip
import io.github.fabiann1809.reader.R
import io.github.fabiann1809.reader.data.prefs.GoalUnit
import io.github.fabiann1809.reader.data.prefs.ReadingGoal
import io.github.fabiann1809.reader.ui.components.readerTextFieldColors
import io.github.fabiann1809.reader.ui.components.readerTextFieldShape

/** "Meta diaria" (T16.3): minutes or pages a day, and how many. */
@Composable
fun GoalDialog(goal: ReadingGoal, onSave: (ReadingGoal) -> Unit, onDismiss: () -> Unit) {
    var unit by rememberSaveable { mutableStateOf(goal.unit) }
    var amount by rememberSaveable { mutableStateOf(goal.amount.toString()) }
    val value = amount.toIntOrNull()?.takeIf { it in ReadingGoal.AMOUNTS }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.goal_title)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    GoalUnit.entries.forEach { option ->
                        ReaderFilterChip(
                            selected = option == unit,
                            onClick = { unit = option },
                            label = { Text(stringResource(if (option == GoalUnit.MINUTES) R.string.goal_minutes else R.string.goal_pages)) },
                        )
                    }
                }
                OutlinedTextField(
                    shape = readerTextFieldShape,
                    colors = readerTextFieldColors(),
                    value = amount,
                    onValueChange = { text -> if (text.length <= 3 && text.all(Char::isDigit)) amount = text },
                    label = { Text(stringResource(if (unit == GoalUnit.MINUTES) R.string.goal_minutes_a_day else R.string.goal_pages_a_day)) },
                    isError = value == null,
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                )
            }
        },
        confirmButton = {
            TextButton(onClick = { value?.let { onSave(ReadingGoal(unit, it)) } }, enabled = value != null) {
                Text(stringResource(R.string.action_save))
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_cancel)) } },
    )
}
