package io.github.fabiann1809.reader.data.book

/**
 * Whether the app holds the book's file (DIGITAL, readable in the app) or only tracks a paper book
 * (PHYSICAL, read outside the app and captured with the camera).
 * Stored by name in Room, so renaming a constant requires a database migration.
 */
enum class BookKind {
    DIGITAL,
    PHYSICAL,
}
