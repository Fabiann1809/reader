package io.github.fabiann1809.reader

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import io.github.fabiann1809.reader.ui.navigation.ReaderApp
import io.github.fabiann1809.reader.ui.theme.ReaderTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            ReaderTheme {
                ReaderApp()
            }
        }
    }
}
