package io.github.fabiann1809.reader

import android.app.Application

/** Application entry point; owns the dependency container shared by all screens. */
class ReaderApplication : Application() {
    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
    }
}
