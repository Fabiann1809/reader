package io.github.fabiann1809.reader.ui.interpretation

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import io.github.fabiann1809.reader.R
import io.github.fabiann1809.reader.ai.InterpretationAnalysis
import io.github.fabiann1809.reader.ui.theme.ReaderTheme

/** The analysis (lámina 1i): "Bien entendido", "Incompleto" and "Confuso", each only when it has something to say. */
@Composable
fun AnalysisBlocks(analysis: InterpretationAnalysis, modifier: Modifier = Modifier) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp), modifier = modifier) {
        analysis.understood?.let {
            AnalysisBlock(R.drawable.ic_check_circle, R.string.interpretation_understood, it, ReaderTheme.colors.success, ReaderTheme.colors.successContainer, ReaderTheme.colors.onSuccessContainer)
        }
        analysis.incomplete?.let {
            AnalysisBlock(R.drawable.ic_warning_circle, R.string.interpretation_incomplete, it, ReaderTheme.colors.warning, ReaderTheme.colors.warningContainer, ReaderTheme.colors.onWarningContainer)
        }
        analysis.confused?.let {
            AnalysisBlock(R.drawable.ic_x, R.string.interpretation_confused, it, MaterialTheme.colorScheme.error, MaterialTheme.colorScheme.errorContainer, MaterialTheme.colorScheme.onErrorContainer)
        }
    }
}

@Composable
private fun AnalysisBlock(@DrawableRes icon: Int, @StringRes title: Int, text: String, color: Color, container: Color, ink: Color) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier
            .fillMaxWidth()
            .background(container, RoundedCornerShape(18.dp))
            .padding(14.dp),
    ) {
        Icon(painterResource(icon), contentDescription = null, tint = color, modifier = Modifier.size(22.dp))
        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(stringResource(title), style = MaterialTheme.typography.titleSmall, color = ink)
            Text(text, style = MaterialTheme.typography.bodyMedium)
        }
    }
}
