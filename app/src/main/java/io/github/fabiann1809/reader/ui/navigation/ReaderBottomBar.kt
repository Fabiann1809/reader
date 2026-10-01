package io.github.fabiann1809.reader.ui.navigation

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.navigation.NavDestination
import androidx.navigation.NavDestination.Companion.hasRoute
import io.github.fabiann1809.reader.R
import kotlin.reflect.KClass

/** The four top-level destinations of the bottom bar (design 03 §1), in display order. */
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
 * Bottom navigation (design 7.2): tonal pill and filled icon on the active tab, outlined icons otherwise.
 * Labels are always visible so the icons never stand alone.
 */
@Composable
fun ReaderBottomBar(
    selected: TopLevelTab,
    onSelect: (TopLevelTab) -> Unit,
    modifier: Modifier = Modifier,
) {
    NavigationBar(modifier = modifier, containerColor = MaterialTheme.colorScheme.surfaceContainer) {
        TopLevelTab.entries.forEach { tab ->
            val isSelected = tab == selected
            NavigationBarItem(
                selected = isSelected,
                onClick = { onSelect(tab) },
                icon = {
                    Icon(
                        painter = painterResource(if (isSelected) tab.selectedIcon else tab.icon),
                        contentDescription = null,
                    )
                },
                label = { Text(stringResource(tab.label), style = MaterialTheme.typography.labelSmall) },
                colors = NavigationBarItemDefaults.colors(
                    indicatorColor = MaterialTheme.colorScheme.secondaryContainer,
                    selectedIconColor = MaterialTheme.colorScheme.onSecondaryContainer,
                    selectedTextColor = MaterialTheme.colorScheme.onSurface,
                    unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant,
                ),
            )
        }
    }
}
