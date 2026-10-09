package io.github.fabiann1809.reader.ui.more

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onLast
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.test.ext.junit.runners.AndroidJUnit4
import io.github.fabiann1809.reader.R
import io.github.fabiann1809.reader.ui.theme.ReaderTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class BackupContentTest {

    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    private fun string(id: Int) = composeRule.activity.getString(id)

    private fun setContent(
        export: ExportState = ExportState.Idle,
        import: ImportState = ImportState.Idle,
        onExport: () -> Unit = {},
        onImport: () -> Unit = {},
    ) {
        composeRule.setContent {
            ReaderTheme {
                BackupContent(export, onExport, onNavigateUp = {}, import = import, onImport = onImport)
            }
        }
    }

    @Test
    fun exportAndImportButtonsCallBack() {
        var exported = false
        var imported = false
        setContent(onExport = { exported = true }, onImport = { imported = true })

        composeRule.onAllNodesWithText(string(R.string.backup_export)).onLast().performScrollTo().performClick()
        composeRule.onAllNodesWithText(string(R.string.backup_import)).onLast().performScrollTo().performClick()

        assertEquals(true, exported)
        assertEquals(true, imported)
    }

    @Test
    fun buttonsWaitWhileWorking() {
        setContent(export = ExportState.Exporting, import = ImportState.Importing)

        composeRule.onAllNodesWithText(string(R.string.backup_export)).onLast().performScrollTo().assertIsNotEnabled()
        composeRule.onAllNodesWithText(string(R.string.backup_import)).onLast().performScrollTo().assertIsNotEnabled()
    }

    @Test
    fun failuresAreExplained() {
        setContent(export = ExportState.Failed, import = ImportState.Failed(ImportFailure.NOT_A_BACKUP))

        composeRule.onNodeWithText(string(R.string.backup_export_failed)).performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithText(string(R.string.backup_import_not_a_backup)).performScrollTo().assertIsDisplayed()
    }
}
