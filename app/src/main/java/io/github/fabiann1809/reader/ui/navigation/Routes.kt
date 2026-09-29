package io.github.fabiann1809.reader.ui.navigation

import kotlinx.serialization.Serializable

// Type-safe navigation destinations. Screens that need arguments will use data classes.

@Serializable
data object HomeRoute

@Serializable
data object SettingsRoute
