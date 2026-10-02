package io.github.fabiann1809.reader.ui.voice

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import io.github.fabiann1809.reader.R
import io.github.fabiann1809.reader.ui.components.aiErrorMessageRes
import io.github.fabiann1809.reader.ui.components.readerTextFieldColors
import io.github.fabiann1809.reader.ui.components.readerTextFieldShape

private const val TRANSCRIPT_MIN_LINES = 3

/** The recording written down (T13.2): a wait, the editable text, or why it failed with a retry. */
@Composable
fun TranscriptField(
    transcript: Transcript,
    onChange: (String) -> Unit,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
) {
    when (transcript) {
        Transcript.Loading -> Row(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            modifier = modifier.heightIn(min = 56.dp),
        ) {
            CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
            Text(stringResource(R.string.voice_transcribing), style = MaterialTheme.typography.bodyMedium)
        }
        is Transcript.Ready -> OutlinedTextField(
            shape = readerTextFieldShape,
            colors = readerTextFieldColors(),
            value = transcript.text,
            onValueChange = onChange,
            label = { Text(stringResource(R.string.voice_transcript_label)) },
            // Nothing was heard: the field stays, so the user can write the note by hand.
            placeholder = { Text(stringResource(R.string.voice_transcript_empty)) },
            minLines = TRANSCRIPT_MIN_LINES,
            keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
            modifier = modifier.fillMaxWidth(),
        )
        is Transcript.Failed -> Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = modifier.fillMaxWidth(),
        ) {
            Text(
                text = stringResource(aiErrorMessageRes(transcript.error)),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.weight(1f),
            )
            TextButton(onClick = onRetry) { Text(stringResource(R.string.voice_transcript_retry)) }
        }
    }
}
