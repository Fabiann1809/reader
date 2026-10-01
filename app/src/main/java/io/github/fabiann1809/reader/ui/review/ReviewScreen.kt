package io.github.fabiann1809.reader.ui.review

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import io.github.fabiann1809.reader.R
import io.github.fabiann1809.reader.ui.components.ReaderTopAppBar
import io.github.fabiann1809.reader.ui.components.StatusMessage

/** "Repasar" tab. Flashcards arrive in phase 14; until then it shows the design's empty state. */
@Composable
fun ReviewScreen(modifier: Modifier = Modifier) {
    Scaffold(
        modifier = modifier,
        topBar = { ReaderTopAppBar(title = stringResource(R.string.tab_review)) },
    ) { innerPadding ->
        Box(
            Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentAlignment = Alignment.Center,
        ) {
            StatusMessage(
                icon = R.drawable.ic_cards,
                title = stringResource(R.string.review_empty_title),
                message = stringResource(R.string.review_empty_message),
            )
        }
    }
}
