package io.github.fabiann1809.reader.ui.privacy

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLinkStyles
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withLink
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import io.github.fabiann1809.reader.R
import io.github.fabiann1809.reader.ai.MAX_TEXT_LENGTH
import io.github.fabiann1809.reader.ui.components.ReaderTopAppBar
import io.github.fabiann1809.reader.ui.theme.ReaderTheme

private const val GEMINI_TERMS_URL = "https://ai.google.dev/gemini-api/terms"

/** Static explanation of what data leaves the phone. No ViewModel: there is no state to hold. */
@Composable
fun PrivacyScreen(onNavigateUp: () -> Unit, modifier: Modifier = Modifier) {
    Scaffold(
        modifier = modifier,
        topBar = {
            ReaderTopAppBar(title = stringResource(R.string.privacy_title), onNavigateUp = onNavigateUp)
        },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp),
        ) {
            Text(stringResource(R.string.privacy_intro), style = MaterialTheme.typography.bodyLarge)
            Section(R.string.privacy_sent_title) {
                Paragraph(stringResource(R.string.privacy_sent_body, MAX_TEXT_LENGTH))
            }
            Section(R.string.privacy_provider_title) {
                Paragraph(stringResource(R.string.privacy_provider_body))
                TermsLink()
            }
            Section(R.string.privacy_local_title) {
                Paragraph(stringResource(R.string.privacy_local_body))
            }
            Section(R.string.privacy_key_title) {
                Paragraph(stringResource(R.string.privacy_key_body))
            }
        }
    }
}

@Composable
private fun Section(@StringRes title: Int, content: @Composable () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(stringResource(title), style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
        content()
    }
}

@Composable
private fun Paragraph(text: String) {
    Text(text = text, style = MaterialTheme.typography.bodyMedium)
}

@Composable
private fun TermsLink() {
    val linkColor = MaterialTheme.colorScheme.primary
    Text(
        text = buildAnnotatedString {
            withLink(
                LinkAnnotation.Url(
                    url = GEMINI_TERMS_URL,
                    styles = TextLinkStyles(SpanStyle(color = linkColor, textDecoration = TextDecoration.Underline)),
                ),
            ) {
                append(stringResource(R.string.privacy_provider_terms))
            }
        },
        style = MaterialTheme.typography.bodyMedium,
    )
}

@Preview(showBackground = true)
@Composable
private fun PrivacyPreview() {
    ReaderTheme {
        PrivacyScreen(onNavigateUp = {})
    }
}
