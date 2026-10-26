package io.github.fabiann1809.reader.ui.settings

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.core.content.ContextCompat
import io.github.fabiann1809.reader.R
import io.github.fabiann1809.reader.data.reminder.MINUTES_PER_HOUR

/** "Recordatorio": a daily notification that says how many cards are waiting (T14.6). */
@Composable
fun ReminderSection(state: GeneralSettingsUiState, onEnable: (Boolean) -> Unit, onTime: (Int) -> Unit) {
    val context = LocalContext.current
    var permissionDenied by rememberSaveable { mutableStateOf(false) }
    var pickingTime by rememberSaveable { mutableStateOf(false) }
    val askPermission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        permissionDenied = !granted
        if (granted) onEnable(true)
    }
    val turnOn = {
        // Before Android 13 notifications need no permission.
        val needsAsking = Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        if (needsAsking) askPermission.launch(Manifest.permission.POST_NOTIFICATIONS) else onEnable(true)
    }
    SettingsSection(R.string.settings_reminder) {
        Toggle(
            R.string.settings_reminder_toggle,
            R.string.settings_reminder_hint,
            state.settings.reviewReminder,
        ) { on -> if (on) turnOn() else onEnable(false) }
        if (permissionDenied && !state.settings.reviewReminder) {
            Text(
                stringResource(R.string.settings_reminder_denied),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error,
            )
        }
        if (state.settings.reviewReminder) {
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(role = Role.Button) { pickingTime = true },
            ) {
                Text(stringResource(R.string.settings_reminder_time), style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold))
                Text(clockText(state.settings.reminderMinutes), style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
            }
        }
    }
    if (pickingTime) {
        ReminderTimeDialog(
            initialMinutes = state.settings.reminderMinutes,
            onConfirm = {
                pickingTime = false
                onTime(it)
            },
            onDismiss = { pickingTime = false },
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ReminderTimeDialog(initialMinutes: Int, onConfirm: (Int) -> Unit, onDismiss: () -> Unit) {
    val picker = rememberTimePickerState(initialMinutes / MINUTES_PER_HOUR, initialMinutes % MINUTES_PER_HOUR, is24Hour = true)
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.settings_reminder_time)) },
        text = { TimePicker(state = picker) },
        confirmButton = {
            TextButton(onClick = { onConfirm(picker.hour * MINUTES_PER_HOUR + picker.minute) }) {
                Text(stringResource(R.string.action_save))
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_cancel)) } },
    )
}

/** "20:00" */
private fun clockText(minutes: Int): String = "%02d:%02d".format(minutes / MINUTES_PER_HOUR, minutes % MINUTES_PER_HOUR)
