package io.github.fabiann1809.reader.ui

import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.CreationExtras
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import io.github.fabiann1809.reader.ReaderApplication
import io.github.fabiann1809.reader.ui.library.LibraryViewModel

/** Creates every ViewModel with its dependencies taken from the AppContainer. */
object AppViewModelProvider {
    val Factory = viewModelFactory {
        initializer {
            LibraryViewModel(readerApplication().container.bookRepository)
        }
    }
}

private fun CreationExtras.readerApplication(): ReaderApplication =
    this[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY] as ReaderApplication
