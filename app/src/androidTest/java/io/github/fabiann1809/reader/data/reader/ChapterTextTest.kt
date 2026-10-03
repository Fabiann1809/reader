package io.github.fabiann1809.reader.data.reader

import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ChapterTextTest {

    @Test
    fun aChapterFileBecomesItsPlainTextWithoutHeadStylesOrScripts() {
        val html = """
            <?xml version="1.0" encoding="utf-8"?>
            <html><head><title>Capítulo 1</title><style>p { color: red; }</style></head>
            <body>
              <h1>El orden del universo</h1>
              <p>La entropía&nbsp;mide el <em>desorden</em>.</p>
              <script>console.log("no");</script>
              <p>El calor va de lo caliente a lo frío.</p>
            </body></html>
        """.trimIndent()

        val text = htmlToText(html)

        assertEquals(
            "El orden del universo\nLa entropía mide el desorden.\nEl calor va de lo caliente a lo frío.",
            text.lines().map { it.trim() }.filter { it.isNotEmpty() }.joinToString("\n"),
        )
    }
}
