package io.github.fabiann1809.reader.ui.onboarding

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import io.github.fabiann1809.reader.R
import io.github.fabiann1809.reader.ui.AppViewModelProvider
import io.github.fabiann1809.reader.ui.components.PrimaryButton
import io.github.fabiann1809.reader.ui.theme.ReaderTheme
import kotlinx.coroutines.launch

/** The three onboarding steps from the design (03 §9). */
private enum class OnboardingPage(@DrawableRes val icon: Int, @StringRes val title: Int, @StringRes val text: Int) {
    SHELVES(R.drawable.ic_books_duotone, R.string.onboarding_shelves_title, R.string.onboarding_shelves_text),
    UNDERSTAND(R.drawable.ic_sparkle, R.string.onboarding_understand_title, R.string.onboarding_understand_text),
    REMEMBER(R.drawable.ic_cards_duotone, R.string.onboarding_remember_title, R.string.onboarding_remember_text),
}

@Composable
fun OnboardingScreen(
    onFinished: () -> Unit,
    viewModel: OnboardingViewModel = viewModel(factory = AppViewModelProvider.Factory),
) {
    val isFinished by viewModel.isFinished.collectAsStateWithLifecycle()
    LaunchedEffect(isFinished) {
        if (isFinished) onFinished()
    }
    OnboardingContent(onFinish = viewModel::finish)
}

/** Skippable steps; skipping and finishing both count as "seen" so the onboarding never comes back. */
@Composable
fun OnboardingContent(onFinish: () -> Unit, modifier: Modifier = Modifier) {
    val pages = OnboardingPage.entries
    val pagerState = rememberPagerState { pages.size }
    val scope = rememberCoroutineScope()
    val isLastPage = pagerState.currentPage == pages.lastIndex

    Surface(modifier = modifier.fillMaxSize(), color = MaterialTheme.colorScheme.surface) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .safeDrawingPadding()
                .padding(16.dp),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
                contentAlignment = Alignment.CenterEnd,
            ) {
                if (!isLastPage) {
                    TextButton(onClick = onFinish) { Text(stringResource(R.string.onboarding_skip)) }
                }
            }
            HorizontalPager(state = pagerState, modifier = Modifier.weight(1f)) { index ->
                Page(pages[index])
            }
            PageIndicator(count = pages.size, current = pagerState.currentPage)
            Spacer(Modifier.height(24.dp))
            PrimaryButton(
                text = stringResource(if (isLastPage) R.string.onboarding_start else R.string.onboarding_next),
                onClick = {
                    if (isLastPage) onFinish() else scope.launch { pagerState.animateScrollToPage(pagerState.currentPage + 1) }
                },
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

@Composable
private fun Page(page: OnboardingPage) {
    // The AI step uses the lavender accent, like every AI feature in the app.
    val isAi = page == OnboardingPage.UNDERSTAND
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Box(
            modifier = Modifier
                .size(160.dp)
                .background(
                    if (isAi) ReaderTheme.colors.aiSoft else MaterialTheme.colorScheme.secondaryContainer,
                    CircleShape,
                ),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                painter = painterResource(page.icon),
                contentDescription = null,
                tint = if (isAi) ReaderTheme.colors.ai else MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(80.dp),
            )
        }
        Spacer(Modifier.height(32.dp))
        Text(
            text = stringResource(page.title),
            style = MaterialTheme.typography.headlineMedium,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(12.dp))
        Text(
            text = stringResource(page.text),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
private fun PageIndicator(count: Int, current: Int) {
    val description = stringResource(R.string.onboarding_page, current + 1, count)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .semantics { contentDescription = description },
        horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
    ) {
        repeat(count) { index ->
            val active = index == current
            Box(
                Modifier
                    .size(width = if (active) 24.dp else 8.dp, height = 8.dp)
                    .background(
                        if (active) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline,
                        CircleShape,
                    ),
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun OnboardingPreview() {
    ReaderTheme {
        OnboardingContent(onFinish = {})
    }
}
