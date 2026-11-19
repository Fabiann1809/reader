package io.github.fabiann1809.reader.ui.account

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import io.github.fabiann1809.reader.R
import io.github.fabiann1809.reader.ui.AppViewModelProvider
import io.github.fabiann1809.reader.ui.components.ReaderTopAppBar

/** The "Cuenta" destination of Más (T22.9): who is signed in, signing out, and the Drive sync. */
@Composable
fun AccountScreen(
    onNavigateUp: () -> Unit,
    modifier: Modifier = Modifier,
    account: AccountViewModel = viewModel(factory = AppViewModelProvider.Factory),
    sync: SyncViewModel = viewModel(factory = AppViewModelProvider.Factory),
) {
    val email by account.email.collectAsStateWithLifecycle()
    val accountState by account.uiState.collectAsStateWithLifecycle()
    val syncState by sync.uiState.collectAsStateWithLifecycle()
    AccountContent(
        email = email,
        accountState = accountState,
        onSignOut = account::signOut,
        onNavigateUp = onNavigateUp,
        modifier = modifier,
        sync = { SyncSection(syncState, sync) },
    )
}

@Composable
fun AccountContent(
    email: String?,
    accountState: AccountUiState,
    onSignOut: () -> Unit,
    onNavigateUp: () -> Unit,
    modifier: Modifier = Modifier,
    sync: @Composable () -> Unit = {},
) {
    Scaffold(
        modifier = modifier,
        topBar = { ReaderTopAppBar(title = stringResource(R.string.account_title), onNavigateUp = onNavigateUp) },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(start = 20.dp, top = 8.dp, end = 20.dp, bottom = 40.dp),
            verticalArrangement = Arrangement.spacedBy(22.dp),
        ) {
            AccountSection(email, accountState, onSignOut)
            sync()
        }
    }
}
