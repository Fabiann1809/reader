package io.github.fabiann1809.reader.ui.navigation

import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import io.github.fabiann1809.reader.ui.AppStartViewModel
import io.github.fabiann1809.reader.ui.AppViewModelProvider
import io.github.fabiann1809.reader.ui.addbook.AddBookScreen
import io.github.fabiann1809.reader.ui.bookdetail.BookDetailScreen
import io.github.fabiann1809.reader.ui.capture.CaptureScreen
import io.github.fabiann1809.reader.ui.explanation.ExplanationScreen
import io.github.fabiann1809.reader.ui.extractedtext.ExtractedTextScreen
import io.github.fabiann1809.reader.ui.library.LibraryScreen
import io.github.fabiann1809.reader.ui.more.AboutScreen
import io.github.fabiann1809.reader.ui.more.BackupScreen
import io.github.fabiann1809.reader.ui.more.MoreEntry
import io.github.fabiann1809.reader.ui.more.MoreScreen
import io.github.fabiann1809.reader.ui.noteeditor.NoteEditorScreen
import io.github.fabiann1809.reader.ui.notes.AllNotesScreen
import io.github.fabiann1809.reader.ui.onboarding.OnboardingScreen
import io.github.fabiann1809.reader.ui.privacy.PrivacyScreen
import io.github.fabiann1809.reader.ui.progress.ProgressScreen
import io.github.fabiann1809.reader.ui.reader.ReaderScreen
import io.github.fabiann1809.reader.ui.review.ReviewScreen
import io.github.fabiann1809.reader.ui.settings.SettingsScreen
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow

/**
 * App root: waits for the first-screen decision (onboarding or library), then shows the navigation.
 * [showLibraryRequests] emits when books arrive from another app, to show the import progress.
 */
@Composable
fun ReaderApp(
    showLibraryRequests: Flow<Unit> = emptyFlow(),
    viewModel: AppStartViewModel = viewModel(factory = AppViewModelProvider.Factory),
) {
    val startDestination by viewModel.startDestination.collectAsStateWithLifecycle()
    // Reading the flag takes a few milliseconds; the window background shows meanwhile.
    startDestination?.let { ReaderNavHost(startDestination = it, showLibraryRequests = showLibraryRequests) }
}

@Composable
fun ReaderNavHost(
    modifier: Modifier = Modifier,
    startDestination: Any = LibraryRoute,
    navController: NavHostController = rememberNavController(),
    showLibraryRequests: Flow<Unit> = emptyFlow(),
) {
    LaunchedEffect(navController, showLibraryRequests) {
        showLibraryRequests.collect {
            // No destination yet means the graph is not ready. The onboarding ends on the library anyway,
            // and jumping there would skip it.
            val destination = navController.currentDestination ?: return@collect
            if (!destination.hasRoute<OnboardingRoute>()) navController.showLibrary()
        }
    }
    val backStackEntry by navController.currentBackStackEntryAsState()
    // The bottom bar only shows on the four top-level screens; everything else is full screen.
    val currentTab = TopLevelTab.of(backStackEntry?.destination)

    Scaffold(
        modifier = modifier,
        // Each screen handles its own system bar insets.
        contentWindowInsets = WindowInsets(0),
        bottomBar = {
            if (currentTab != null) {
                ReaderBottomBar(selected = currentTab, onSelect = { tab -> navController.navigateToTab(tab) })
            }
        },
    ) { innerPadding ->
        ReaderNavGraph(
            navController = navController,
            startDestination = startDestination,
            modifier = Modifier
                .padding(innerPadding)
                .consumeWindowInsets(innerPadding),
        )
    }
}

/**
 * Switching tabs keeps a single copy of each tab and remembers its state; going back from any
 * tab returns to the library (design 03 §6), and back from the library leaves the app.
 */
/**
 * Shows the library itself: from a screen opened on top of it (a book's detail, the reader) the tab
 * switch would restore that same screen, so going back down the stack is what reaches it.
 */
private fun NavHostController.showLibrary() {
    if (!popBackStack<LibraryRoute>(inclusive = false)) navigateToTab(TopLevelTab.LIBRARY)
}

private fun NavHostController.navigateToTab(tab: TopLevelTab) {
    navigate(tab.route) {
        // The library is the root of the tabs even when the graph started on the onboarding.
        popUpTo<LibraryRoute> { saveState = true }
        launchSingleTop = true
        restoreState = true
    }
}

@Composable
private fun ReaderNavGraph(navController: NavHostController, startDestination: Any, modifier: Modifier = Modifier) {
    NavHost(
        navController = navController,
        startDestination = startDestination,
        modifier = modifier,
        // Cross-fade between screens (design motion: short, no sideways slide).
        enterTransition = { fadeIn(tween(FADE_MILLIS)) },
        exitTransition = { fadeOut(tween(FADE_MILLIS)) },
        popEnterTransition = { fadeIn(tween(FADE_MILLIS)) },
        popExitTransition = { fadeOut(tween(FADE_MILLIS)) },
    ) {
        composable<OnboardingRoute> {
            OnboardingScreen(
                onFinished = {
                    navController.navigate(LibraryRoute) { popUpTo<OnboardingRoute> { inclusive = true } }
                },
            )
        }
        composable<LibraryRoute> {
            LibraryScreen(
                onBookClick = { bookId -> navController.navigate(BookDetailRoute(bookId)) },
                onOpenBook = { bookId -> navController.navigate(ReaderRoute(bookId)) },
                onAddPhysicalBook = { navController.navigate(AddBookRoute) },
            )
        }
        composable<ReviewRoute> {
            ReviewScreen()
        }
        composable<ProgressRoute> {
            ProgressScreen()
        }
        composable<MoreRoute> {
            MoreScreen(
                onOpen = { entry ->
                    navController.navigate(
                        when (entry) {
                            MoreEntry.ALL_NOTES -> AllNotesRoute
                            MoreEntry.BACKUP -> BackupRoute
                            MoreEntry.SETTINGS -> SettingsRoute
                            MoreEntry.PRIVACY -> PrivacyRoute
                            MoreEntry.ABOUT -> AboutRoute
                        },
                    )
                },
            )
        }
        composable<AllNotesRoute> {
            AllNotesScreen(
                onNavigateUp = { navController.navigateUp() },
                onNoteClick = { note -> navController.navigate(NoteEditorRoute(note.bookId, note.id)) },
            )
        }
        composable<BackupRoute> {
            BackupScreen(onNavigateUp = { navController.navigateUp() })
        }
        composable<AboutRoute> {
            AboutScreen(onNavigateUp = { navController.navigateUp() })
        }
        composable<AddBookRoute> {
            AddBookScreen(onNavigateUp = { navController.navigateUp() })
        }
        // Route arguments reach each ViewModel through its SavedStateHandle (see AppViewModelProvider).
        composable<ReaderRoute> {
            ReaderScreen(onNavigateUp = { navController.navigateUp() })
        }
        composable<BookDetailRoute> { entry ->
            val bookId = entry.toRoute<BookDetailRoute>().bookId
            BookDetailScreen(
                onNavigateUp = { navController.navigateUp() },
                onAddNote = { navController.navigate(NoteEditorRoute(bookId)) },
                onNoteClick = { noteId -> navController.navigate(NoteEditorRoute(bookId, noteId)) },
                onCapturePage = { navController.navigate(CaptureRoute(bookId)) },
                onRead = { navController.navigate(ReaderRoute(bookId)) },
            )
        }
        composable<NoteEditorRoute> {
            NoteEditorScreen(onNavigateUp = { navController.navigateUp() })
        }
        composable<CaptureRoute> { entry ->
            val bookId = entry.toRoute<CaptureRoute>().bookId
            CaptureScreen(
                onNavigateUp = { navController.navigateUp() },
                onImageReady = { uri -> navController.navigate(ExtractedTextRoute(bookId, uri.toString())) },
            )
        }
        composable<ExtractedTextRoute> { entry ->
            val bookId = entry.toRoute<ExtractedTextRoute>().bookId
            ExtractedTextScreen(
                onNavigateUp = { navController.navigateUp() },
                onExplain = { text -> navController.navigate(ExplanationRoute(bookId, text)) },
            )
        }
        composable<ExplanationRoute> {
            ExplanationScreen(
                onNavigateUp = { navController.navigateUp() },
                // Back to the book, dropping the capture screens so "back" doesn't walk through them again.
                onNoteSaved = { navController.popBackStack<BookDetailRoute>(inclusive = false) },
            )
        }
        composable<SettingsRoute> {
            SettingsScreen(
                onNavigateUp = { navController.navigateUp() },
                onOpenPrivacy = { navController.navigate(PrivacyRoute) },
            )
        }
        composable<PrivacyRoute> {
            PrivacyScreen(onNavigateUp = { navController.navigateUp() })
        }
    }
}

private const val FADE_MILLIS = 200
