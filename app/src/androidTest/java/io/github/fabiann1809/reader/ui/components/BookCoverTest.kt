package io.github.fabiann1809.reader.ui.components

import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.background
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import io.github.fabiann1809.reader.data.book.Book
import io.github.fabiann1809.reader.ui.theme.ReaderTheme
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File

@RunWith(AndroidJUnit4::class)
class BookCoverTest {

    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    private val titles = listOf(
        "Cosmos" to "Carl Sagan",
        "Cien años de soledad" to "Gabriel García Márquez",
        "El principito" to "Antoine de Saint-Exupéry",
        "Sapiens" to "Yuval Noah Harari",
        "Don Quijote de la Mancha" to "Miguel de Cervantes",
        "Rayuela" to "Julio Cortázar",
        "1984" to "George Orwell",
        "Pedro Páramo" to "Juan Rulfo",
        "La metamorfosis" to "Franz Kafka",
        "Dune" to "Frank Herbert",
        "Fundación" to "Isaac Asimov",
        "El nombre de la rosa" to "Umberto Eco",
    )

    @Test
    fun differentTitlesGetDifferentLooks() {
        val looks = titles.map { (title, _) -> coverPattern(title) to coverLayout(title) }.toSet()

        // The twelve sample titles are not clones of each other.
        assertTrue("only ${looks.size} different looks", looks.size >= 6)
    }

    // Draws the sample covers and keeps a picture of them in the app's cache, to look at the design.
    @Test
    fun drawsTheCoverGallery() {
        composeRule.setContent {
            ReaderTheme {
                Column(
                    Modifier.background(MaterialTheme.colorScheme.surface).padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    titles.chunked(4).forEach { row ->
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            row.forEach { (title, author) ->
                                BookCover(Book(title = title, author = author), modifier = Modifier.width(78.dp), titleSize = 11.sp)
                            }
                        }
                    }
                }
            }
        }
        val bitmap = composeRule.onRoot().captureToImage().asAndroidBitmap()
        val file = File(InstrumentationRegistry.getInstrumentation().targetContext.cacheDir, "cover_gallery.png")
        file.outputStream().use { bitmap.compress(android.graphics.Bitmap.CompressFormat.PNG, 100, it) }
        assertTrue(file.length() > 0)
    }
}
