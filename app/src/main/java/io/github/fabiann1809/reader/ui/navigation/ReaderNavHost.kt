package io.github.fabiann1809.reader.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import io.github.fabiann1809.reader.ui.addbook.AddBookScreen
import io.github.fabiann1809.reader.ui.bookdetail.BookDetailScreen
import io.github.fabiann1809.reader.ui.library.LibraryScreen
import io.github.fabiann1809.reader.ui.noteeditor.NoteEditorScreen
import io.github.fabiann1809.reader.ui.settings.SettingsScreen

@Composable
fun ReaderNavHost(
    modifier: Modifier = Modifier,
    navController: NavHostController = rememberNavController(),
) {
    NavHost(
        navController = navController,
        startDestination = LibraryRoute,
        modifier = modifier,
    ) {
        composable<LibraryRoute> {
            LibraryScreen(
                onBookClick = { bookId -> navController.navigate(BookDetailRoute(bookId)) },
                onAddBook = { navController.navigate(AddBookRoute) },
                onOpenSettings = { navController.navigate(SettingsRoute) },
            )
        }
        composable<AddBookRoute> {
            AddBookScreen(onNavigateUp = { navController.navigateUp() })
        }
        // Route arguments reach each ViewModel through its SavedStateHandle (see AppViewModelProvider).
        composable<BookDetailRoute> { entry ->
            val bookId = entry.toRoute<BookDetailRoute>().bookId
            BookDetailScreen(
                onNavigateUp = { navController.navigateUp() },
                onAddNote = { navController.navigate(NoteEditorRoute(bookId)) },
                onNoteClick = { noteId -> navController.navigate(NoteEditorRoute(bookId, noteId)) },
            )
        }
        composable<NoteEditorRoute> {
            NoteEditorScreen(onNavigateUp = { navController.navigateUp() })
        }
        composable<SettingsRoute> {
            SettingsScreen(onNavigateUp = { navController.navigateUp() })
        }
    }
}
