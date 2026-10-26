package io.github.fabiann1809.reader.ui.addbook

import android.content.Context
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.codescanner.GmsBarcodeScannerOptions
import com.google.mlkit.vision.codescanner.GmsBarcodeScanning
import io.github.fabiann1809.reader.R
import io.github.fabiann1809.reader.ui.components.readerTextFieldColors
import io.github.fabiann1809.reader.ui.components.readerTextFieldShape

/**
 * The optional ISBN of a paper book: typed or scanned from its barcode, it fills the title, the
 * author and the pages (T10.7).
 */
@Composable
fun IsbnSection(
    uiState: AddBookUiState,
    onIsbnChange: (String) -> Unit,
    onSearch: () -> Unit,
    onScanned: (String) -> Unit,
    onScanFailed: () -> Unit,
) {
    val context = LocalContext.current
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            OutlinedTextField(
                shape = readerTextFieldShape,
                colors = readerTextFieldColors(),
                value = uiState.isbn,
                onValueChange = onIsbnChange,
                label = { Text(stringResource(R.string.isbn_field)) },
                singleLine = true,
                isError = uiState.isbnStatus == IsbnStatus.NOT_VALID,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Search),
                keyboardActions = KeyboardActions(onSearch = { onSearch() }),
                modifier = Modifier.weight(1f),
            )
            IconButton(onClick = { scan(context, onScanned, onScanFailed) }) {
                Icon(painterResource(R.drawable.ic_barcode), contentDescription = stringResource(R.string.isbn_scan))
            }
        }
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
            if (uiState.isbnStatus == IsbnStatus.SEARCHING) {
                CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
            }
            Text(
                text = stringResource(uiState.isbnStatus.message()),
                style = MaterialTheme.typography.labelMedium,
                color = if (uiState.isbnStatus.isProblem()) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.weight(1f).padding(start = 4.dp),
            )
            TextButton(onClick = onSearch, enabled = uiState.isbn.isNotBlank() && uiState.isbnStatus != IsbnStatus.SEARCHING) {
                Text(stringResource(R.string.isbn_search))
            }
        }
    }
}

// Google's scanner opens its own screen and needs no camera permission.
private fun scan(context: Context, onScanned: (String) -> Unit, onFailed: () -> Unit) {
    val options = GmsBarcodeScannerOptions.Builder().setBarcodeFormats(Barcode.FORMAT_EAN_13).build()
    GmsBarcodeScanning.getClient(context, options).startScan()
        .addOnSuccessListener { barcode -> barcode.rawValue?.let(onScanned) ?: onFailed() }
        .addOnFailureListener { onFailed() }
}

private fun IsbnStatus.message(): Int = when (this) {
    IsbnStatus.IDLE -> R.string.isbn_help
    IsbnStatus.SEARCHING -> R.string.isbn_searching
    IsbnStatus.FILLED -> R.string.isbn_filled
    IsbnStatus.NOT_VALID -> R.string.isbn_not_valid
    IsbnStatus.NOT_FOUND -> R.string.isbn_not_found
    IsbnStatus.NO_CONNECTION -> R.string.isbn_no_connection
    IsbnStatus.SCAN_FAILED -> R.string.isbn_scan_failed
}

private fun IsbnStatus.isProblem(): Boolean = this in setOf(
    IsbnStatus.NOT_VALID, IsbnStatus.NOT_FOUND, IsbnStatus.NO_CONNECTION, IsbnStatus.SCAN_FAILED,
)
