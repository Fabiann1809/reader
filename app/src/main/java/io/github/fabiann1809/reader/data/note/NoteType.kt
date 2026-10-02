package io.github.fabiann1809.reader.data.note

// Stored by name in Room, so renaming a constant requires a database migration.
enum class NoteType {
    MANUAL,
    EXPLANATION,

    // Recorded by voice (T13.3): the content is the transcript and Note.audioPath the recording.
    VOICE,
}
