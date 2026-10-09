package io.github.fabiann1809.reader.ui.navigation

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavDestination
import androidx.navigation.NavDestination.Companion.hasRoute
import io.github.fabiann1809.reader.R
import io.github.fabiann1809.reader.ui.components.rememberReduceMotion
import io.github.fabiann1809.reader.ui.theme.ReaderElevation
import io.github.fabiann1809.reader.ui.theme.ReaderTheme
import kotlin.reflect.KClass

/** The four top-level destinations of the bottom bar, in display order. */
enum class TopLevelTab(
    val route: Any,
    val routeClass: KClass<*>,
    @StringRes val label: Int,
    @DrawableRes val icon: Int,
    @DrawableRes val selectedIcon: Int,
) {
    LIBRARY(LibraryRoute, LibraryRoute::class, R.string.tab_library, R.drawable.ic_books, R.drawable.ic_books_fill),
    REVIEW(ReviewRoute, ReviewRoute::class, R.string.tab_review, R.drawable.ic_cards, R.drawable.ic_cards_fill),
    PROGRESS(
        ProgressRoute,
        ProgressRoute::class,
        R.string.tab_progress,
        R.drawable.ic_chart_donut,
        R.drawable.ic_chart_donut_fill,
    ),
    MORE(
        MoreRoute,
        MoreRoute::class,
        R.string.tab_more,
        R.drawable.ic_dots_three_circle,
        R.drawable.ic_dots_three_circle_fill,
    ),
    ;

    companion object {
        fun of(destination: NavDestination?): TopLevelTab? =
            entries.firstOrNull { tab -> destination?.hasRoute(tab.routeClass) == true }
    }
}

/**
 * Floating bottom navigation of the redesign: a rounded bar with a pill that slides to the active
 * tab, whose filled icon pops. Labels are always visible so the icons never stand alone.
 */
@Composable
fun ReaderBottomBar(
    selected: TopLevelTab,
    onSelect: (TopLevelTab) -> Unit,
    modifier: Modifier = Modifier,
    // Tabs with something waiting, marked with a dot (cards to review today).
    dotted: Set<TopLevelTab> = emptySet(),
) {
    val reduceMotion = rememberReduceMotion()
    val colors = ReaderTheme.colors
    Box(
        modifier = modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(start = BarMargin, end = BarMargin, bottom = BarBottomMargin),
    ) {
        Surface(
            shape = RoundedCornerShape(BarHeight / 2),
            color = colors.navBar,
            shadowElevation = ReaderElevation.Overlay,
            modifier = Modifier
                .fillMaxWidth()
                .height(BarHeight),
        ) {
            BoxWithConstraints(Modifier.padding(horizontal = BarPadding)) {
                val itemWidth = maxWidth / TopLevelTab.entries.size
                val pillOffset by animateDpAsState(
                    targetValue = itemWidth * selected.ordinal,
                    animationSpec = if (reduceMotion) snap() else spring(dampingRatio = 0.6f),
                    label = "tabPill",
                )
                Box(
                    Modifier
                        .offset(x = pillOffset, y = BarPadding)
                        .size(itemWidth, ItemHeight)
                        .background(colors.navActive, ItemShape),
                )
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxSize()) {
                    TopLevelTab.entries.forEach { tab ->
                        BarItem(
                            tab = tab,
                            isSelected = tab == selected,
                            hasDot = tab in dotted,
                            reduceMotion = reduceMotion,
                            onClick = { onSelect(tab) },
                            modifier = Modifier.weight(1f),
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun BarItem(
    tab: TopLevelTab,
    isSelected: Boolean,
    hasDot: Boolean,
    reduceMotion: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val tint = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSecondaryContainer
    val iconScale = remember { Animatable(1f) }
    LaunchedEffect(isSelected) {
        if (isSelected && !reduceMotion) {
            iconScale.snapTo(POP_START_SCALE)
            iconScale.animateTo(1f, spring(dampingRatio = 0.45f, stiffness = Spring.StiffnessMediumLow))
        }
    }
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(3.dp, Alignment.CenterVertically),
        modifier = modifier
            .height(ItemHeight)
            .clip(ItemShape)
            .selectable(selected = isSelected, role = Role.Tab, onClick = onClick),
    ) {
        Box {
            Icon(
                painter = painterResource(if (isSelected) tab.selectedIcon else tab.icon),
                contentDescription = null,
                tint = tint,
                modifier = Modifier
                    .size(24.dp)
                    .graphicsLayer {
                        scaleX = iconScale.value
                        scaleY = iconScale.value
                    },
            )
            if (hasDot) {
                Box(
                    Modifier
                        .align(Alignment.TopEnd)
                        .offset(x = 4.dp, y = (-2).dp)
                        .size(9.dp)
                        .background(MaterialTheme.colorScheme.error, CircleShape),
                )
            }
        }
        Text(
            text = stringResource(tab.label),
            color = tint,
            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.ExtraBold),
        )
    }
}

private val BarHeight = 70.dp
private val BarMargin = 14.dp
private val BarBottomMargin = 20.dp
private val BarPadding = 7.dp
private val ItemHeight = 56.dp
private val ItemShape = RoundedCornerShape(28.dp)
private const val POP_START_SCALE = 0.6f
