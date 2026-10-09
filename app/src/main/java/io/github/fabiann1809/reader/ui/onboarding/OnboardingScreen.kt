package io.github.fabiann1809.reader.ui.onboarding

import androidx.annotation.StringRes
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import io.github.fabiann1809.reader.R
import io.github.fabiann1809.reader.ui.AppViewModelProvider
import io.github.fabiann1809.reader.ui.components.rememberReduceMotion
import io.github.fabiann1809.reader.ui.theme.ReaderTheme
import kotlinx.coroutines.launch

/** The three onboarding steps of the design. */
private enum class OnboardingPage(@StringRes val title: Int, @StringRes val text: Int) {
    SHELVES(R.string.onboarding_shelves_title, R.string.onboarding_shelves_text),
    UNDERSTAND(R.string.onboarding_understand_title, R.string.onboarding_understand_text),
    REMEMBER(R.string.onboarding_remember_title, R.string.onboarding_remember_text),
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
    val current = pagerState.currentPage
    val isLastPage = current == pages.lastIndex
    fun goTo(page: Int) = scope.launch { pagerState.animateScrollToPage(page) }

    Surface(modifier = modifier.fillMaxSize(), color = MaterialTheme.colorScheme.surface) {
        Column(Modifier.fillMaxSize().safeDrawingPadding()) {
            TopRow(showBack = current > 0, showSkip = !isLastPage, onBack = { goTo(current - 1) }, onSkip = onFinish)
            HorizontalPager(state = pagerState, modifier = Modifier.weight(1f)) { index -> Page(pages[index]) }
            BottomRow(
                count = pages.size,
                current = current,
                isLastPage = isLastPage,
                onNext = { if (isLastPage) onFinish() else goTo(current + 1) },
            )
        }
    }
}

@Composable
private fun TopRow(showBack: Boolean, showSkip: Boolean, onBack: () -> Unit, onSkip: () -> Unit) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .height(72.dp)
            .padding(horizontal = 12.dp),
    ) {
        if (showBack) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(48.dp)
                    .clickable(role = Role.Button, onClick = onBack),
            ) {
                Icon(painterResource(R.drawable.ic_arrow_left), contentDescription = stringResource(R.string.navigate_up))
            }
        }
        Spacer(Modifier.weight(1f))
        if (showSkip) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .heightIn(min = 48.dp)
                    .clickable(role = Role.Button, onClick = onSkip)
                    .padding(horizontal = 14.dp),
            ) {
                Text(
                    text = stringResource(R.string.onboarding_skip),
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun Page(page: OnboardingPage) {
    Column(Modifier.fillMaxSize()) {
        when (page) {
            OnboardingPage.SHELVES -> ShelfIllustration()
            OnboardingPage.UNDERSTAND -> ScanIllustration(Modifier.padding(horizontal = 24.dp))
            OnboardingPage.REMEMBER -> StudyIllustration(Modifier.padding(horizontal = 24.dp))
        }
        Text(
            text = stringResource(page.title),
            style = MaterialTheme.typography.headlineLarge,
            modifier = Modifier.padding(start = 24.dp, end = 24.dp, top = 24.dp),
        )
        Text(
            text = stringResource(page.text),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(start = 24.dp, end = 24.dp, top = 10.dp),
        )
    }
}

@Composable
private fun BottomRow(count: Int, current: Int, isLastPage: Boolean, onNext: () -> Unit) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 24.dp, end = 24.dp, bottom = 24.dp),
    ) {
        PageIndicator(count, current)
        Button(
            onClick = onNext,
            shape = CircleShape,
            modifier = Modifier.heightIn(min = 56.dp),
        ) {
            Text(stringResource(if (isLastPage) R.string.onboarding_start else R.string.onboarding_next))
            Spacer(Modifier.size(8.dp))
            Icon(painterResource(R.drawable.ic_arrow_right), contentDescription = null, modifier = Modifier.size(18.dp))
        }
    }
}

@Composable
private fun PageIndicator(count: Int, current: Int) {
    val description = stringResource(R.string.onboarding_page, current + 1, count)
    val reduceMotion = rememberReduceMotion()
    Row(
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        modifier = Modifier.semantics { contentDescription = description },
    ) {
        repeat(count) { index ->
            val active = index == current
            val width by animateDpAsState(
                targetValue = if (active) 26.dp else 8.dp,
                animationSpec = if (reduceMotion) snap() else spring(dampingRatio = 0.6f),
                label = "dotWidth",
            )
            Box(
                Modifier
                    .size(width = width, height = 8.dp)
                    .background(
                        if (active) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant,
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
