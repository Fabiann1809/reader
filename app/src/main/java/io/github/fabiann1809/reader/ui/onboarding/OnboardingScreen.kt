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
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import io.github.fabiann1809.reader.ui.components.RoundBackButton
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
    val wave = ReaderTheme.colors.authWave

    Surface(modifier = modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Box(Modifier.fillMaxSize()) {
            Box(Modifier.offset((-150).dp, (-170).dp).size(360.dp).clip(CircleShape).background(wave))
            Box(
                Modifier
                    .align(Alignment.BottomCenter)
                    .offset(y = 120.dp)
                    .fillMaxWidth()
                    .height(210.dp)
                    .clip(RoundedCornerShape(percent = 50))
                    .background(wave),
            )
            Column(Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding()) {
                TopRow(showBack = current > 0, onBack = { goTo(current - 1) }, onSkip = onFinish)
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
}

@Composable
private fun TopRow(showBack: Boolean, onBack: () -> Unit, onSkip: () -> Unit) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .height(84.dp)
            .padding(start = 24.dp, end = 16.dp),
    ) {
        if (showBack) RoundBackButton(onBack)
        Spacer(Modifier.weight(1f))
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .heightIn(min = 48.dp)
                .clickable(role = Role.Button, onClick = onSkip)
                .padding(horizontal = 12.dp),
        ) {
            Text(
                text = stringResource(R.string.onboarding_skip),
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
            )
        }
    }
}

@Composable
private fun Page(page: OnboardingPage) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 28.dp),
    ) {
        when (page) {
            OnboardingPage.SHELVES -> ShelfIllustration()
            OnboardingPage.UNDERSTAND -> ScanIllustration()
            OnboardingPage.REMEMBER -> StudyIllustration()
        }
        Text(
            text = stringResource(page.title),
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.ExtraBold,
            color = MaterialTheme.colorScheme.primary,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = 8.dp),
        )
        Text(
            text = stringResource(page.text),
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSecondaryContainer,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = 10.dp),
        )
    }
}

@Composable
private fun BottomRow(count: Int, current: Int, isLastPage: Boolean, onNext: () -> Unit) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(18.dp),
        modifier = Modifier.fillMaxWidth().padding(start = 28.dp, end = 28.dp, bottom = 32.dp, top = 8.dp),
    ) {
        Button(
            onClick = onNext,
            shape = CircleShape,
            modifier = Modifier.fillMaxWidth().heightIn(min = 58.dp),
        ) {
            Text(stringResource(if (isLastPage) R.string.onboarding_start else R.string.onboarding_next), style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.size(10.dp))
            Icon(painterResource(R.drawable.ic_caret_right), contentDescription = null, modifier = Modifier.size(18.dp))
        }
        PageIndicator(count, current)
    }
}

@Composable
private fun PageIndicator(count: Int, current: Int) {
    val description = stringResource(R.string.onboarding_page, current + 1, count)
    val reduceMotion = rememberReduceMotion()
    Row(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.semantics { contentDescription = description },
    ) {
        repeat(count) { index ->
            val active = index == current
            val width by animateDpAsState(
                targetValue = if (active) 26.dp else 9.dp,
                animationSpec = if (reduceMotion) snap() else spring(dampingRatio = 0.6f),
                label = "dotWidth",
            )
            Box(
                Modifier
                    .size(width = width, height = 9.dp)
                    .background(
                        if (active) MaterialTheme.colorScheme.primary else ReaderTheme.colors.authFieldBorder,
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
