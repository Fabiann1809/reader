package io.github.fabiann1809.reader.data.book

/**
 * File format of a digital book. Physical books have no format.
 * Stored by name in Room, so renaming a constant requires a database migration.
 */
enum class BookFormat {
    EPUB,
    PDF,
    TXT,
    CBZ,
}
