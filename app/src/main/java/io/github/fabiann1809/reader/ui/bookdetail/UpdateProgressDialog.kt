package io.github.fabiann1809.reader.ui.bookdetail

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import io.github.fabiann1809.reader.R
import io.github.fabiann1809.reader.data.book.Book
import io.github.fabiann1809.reader.data.book.BookStatus
import io.github.fabiann1809.reader.ui.components.labelRes

/**
 * Parses the page typed by the user. Returns null when it is not a number,
 * is negative, or exceeds the book's total pages (when known).
 */
fun parseCurrentPage(input: String, totalPages: Int?): Int? {
    val page = input.toIntOrNull() ?: return null
    if (page < 0) return null
    if (totalPages != null && page > totalPages) return null
    return page
}

@Composable
fun UpdateProgressDialog(
    book: Book,
    onConfirm: (currentPage: Int, status: BookStatus) -> Unit,
    onDismiss: () -> Unit,
) {
    var pageInput by rememberSaveable { mutableStateOf(book.currentPage.toString()) }
    var status by rememberSaveable { mutableStateOf(book.status) }
    val page = parseCurrentPage(pageInput, book.totalPages)

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.update_progress_title)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                OutlinedTextField(
                    value = pageInput,
                    onValueChange = { input -> if (input.all(Char::isDigit) && input.length <= 5) pageInput = input },
                    label = { Text(stringResource(R.string.book_field_current_page)) },
                    isError = page == null,
                    supportingText = {
                        when {
                            page != null -> Unit
                            book.totalPages != null ->
                                Text(stringResource(R.string.book_page_out_of_range, book.totalPages))
                            else -> Text(stringResource(R.string.book_page_invalid))
                        }
                    },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth(),
                )
                Text(
                    text = stringResource(R.string.book_field_status),
                    style = MaterialTheme.typography.labelLarge,
                )
                SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                    BookStatus.entries.forEachIndexed { index, option ->
                        SegmentedButton(
                            selected = status == option,
                            onClick = { status = option },
                            shape = SegmentedButtonDefaults.itemShape(index = index, count = BookStatus.entries.size),
                        ) {
                            Text(stringResource(option.labelRes()), maxLines = 1)
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = { page?.let { onConfirm(it, status) } },
                enabled = page != null,
            ) {
                Text(stringResource(R.string.action_save))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.action_cancel))
            }
        },
    )
}
