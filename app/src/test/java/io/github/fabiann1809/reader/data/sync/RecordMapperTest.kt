package io.github.fabiann1809.reader.data.sync

import io.github.fabiann1809.reader.data.book.Book
import io.github.fabiann1809.reader.data.book.BookStatus
import io.github.fabiann1809.reader.data.bookmark.Bookmark
import io.github.fabiann1809.reader.data.collection.BookCollectionCrossRef
import io.github.fabiann1809.reader.data.collection.Collection
import io.github.fabiann1809.reader.data.flashcard.Flashcard
import io.github.fabiann1809.reader.data.highlight.Highlight
import io.github.fabiann1809.reader.data.note.Note
import io.github.fabiann1809.reader.data.note.NoteTag
import io.github.fabiann1809.reader.data.session.ReadingSession
import io.github.fabiann1809.reader.data.sync.merge.RecordKey
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class RecordMapperTest {

    private val book = Book(
        id = 7, title = "Dune", author = "Herbert", currentPage = 12, status = BookStatus.READING,
        filePath = "books/random-name.epub", coverPath = "covers/random.jpg", fileHash = "hash1",
        readingLocation = """{"href":"a.xhtml","quote":"\"x\""}""", syncId = "book-sync", updatedAt = 500,
    )

    @Test
    fun aBookLeavesItsLocalIdAndPathsOutAndCarriesTheCloudNames() {
        val record = RecordMapper.record(book, fileName = "book-book-sync.epub", coverName = "cover-abc.jpg")

        assertEquals(RecordKey("book", "book-sync"), record.key)
        assertEquals(500L, record.updatedAt)
        assertEquals("hash1", record.matchKey)
        assertFalse(record.data.contains("random-name"))
        assertFalse(record.data.contains("\"id\""))
        assertEquals("book-book-sync.epub", RecordMapper.cloudName(record, "filePath"))
        assertEquals("cover-abc.jpg", RecordMapper.cloudName(record, "coverPath"))
    }

    @Test
    fun aBookComesBackWithTheLocalIdAndPathsGivenAndEverythingElseIntact() {
        val record = RecordMapper.record(book, "book-book-sync.epub", "cover-abc.jpg")

        val back = RecordMapper.book(record, id = 42, filePath = "books/new-local.epub", coverPath = null)

        assertEquals(book.copy(id = 42, filePath = "books/new-local.epub", coverPath = null), back)
    }

    @Test
    fun aPaperBookHasNoFileNames() {
        val paper = Book(title = "Paper", author = "x", syncId = "p", updatedAt = 1)

        val record = RecordMapper.record(paper, null, null)

        assertNull(RecordMapper.cloudName(record, "filePath"))
        assertNull(record.matchKey)
    }

    @Test
    fun aNoteNamesItsBookAndKeepsItsContent() {
        val note = Note(
            id = 3, bookId = 7, page = 4, content = "Idea with \"quotes\" and ñ", tag = NoteTag.DOUBT,
            audioPath = "voice/local.m4a", syncId = "note-sync", updatedAt = 9,
        )

        val record = RecordMapper.record(note, "book-sync", "voice-note-sync.m4a")

        assertEquals(listOf(RecordKey("book", "book-sync")), record.parents)
        assertFalse(record.data.contains("bookId"))
        val back = RecordMapper.note(record, id = 0, bookId = 55, audioPath = "voice/mine.m4a")
        assertEquals(note.copy(id = 0, bookId = 55, audioPath = "voice/mine.m4a"), back)
    }

    @Test
    fun everyOtherKindSurvivesTheTrip() {
        val collection = Collection(id = 2, name = "Clásicos", colorIndex = 3, syncId = "c", updatedAt = 5)
        val bookmark = Bookmark(id = 1, bookId = 7, location = "{}", position = 3, syncId = "bm", updatedAt = 6)
        val highlight = Highlight(id = 1, bookId = 7, location = "{}", text = "text", syncId = "h", updatedAt = 7)
        val card = Flashcard(id = 1, bookId = 7, front = "Q", back = "A", easeFactor = 2.1, syncId = "f", updatedAt = 8)
        val session = ReadingSession(id = 1, bookId = 7, startedAt = 100, endedAt = 200, pagesRead = 4, syncId = "s", updatedAt = 9)

        assertEquals(collection.copy(id = 0), RecordMapper.collection(RecordMapper.record(collection), 0))
        assertEquals(bookmark.copy(id = 0, bookId = 9), RecordMapper.bookmark(RecordMapper.record(bookmark, "b"), 0, 9))
        assertEquals(highlight.copy(id = 0, bookId = 9), RecordMapper.highlight(RecordMapper.record(highlight, "b"), 0, 9))
        assertEquals(card.copy(id = 0, bookId = 9), RecordMapper.flashcard(RecordMapper.record(card, "b"), 0, 9))
        assertEquals(session.copy(id = 0, bookId = 9), RecordMapper.session(RecordMapper.record(session, "b"), 0, 9))
    }

    @Test
    fun collectionsWithTheSameNameMatchWhateverTheCaseOrSpacing() {
        val a = RecordMapper.record(Collection(name = " Clásicos "))
        val b = RecordMapper.record(Collection(name = "clásicos"))

        assertEquals(a.matchKey, b.matchKey)
    }

    @Test
    fun aLinkIsNamedByBothSyncIdsAndComesBackWithTheLocalIds() {
        val link = BookCollectionCrossRef(bookId = 1, collectionId = 2, addedAt = 777)

        val record = RecordMapper.record(link, "book-sync", "collection-sync")

        assertEquals(RecordKey("book_collection", "book-sync|collection-sync"), record.key)
        assertEquals(777L, record.updatedAt)
        assertEquals(2, record.parents.size)
        assertEquals("book-sync" to "collection-sync", RecordMapper.splitLinkId(record.key.syncId))
        assertEquals(BookCollectionCrossRef(10, 20, 777), RecordMapper.link(record, 10, 20))
    }

    @Test
    fun theFilesOfARecordAreItsCloudNames() {
        val withFiles = RecordMapper.record(book, "book-x.epub", "cover-y.jpg")
        val withoutCover = RecordMapper.record(book, "book-x.epub", null)
        val voice = RecordMapper.record(Note(bookId = 1, content = "c"), "b", "voice-z.m4a")

        assertEquals(listOf("book-x.epub", "cover-y.jpg"), SyncFileNames.of(withFiles))
        assertEquals(listOf("book-x.epub"), SyncFileNames.of(withoutCover))
        assertEquals(listOf("voice-z.m4a"), SyncFileNames.of(voice))
        assertTrue(SyncFileNames.of(RecordMapper.record(Collection(name = "x"))).isEmpty())
    }

    @Test
    fun cloudFileNamesAreStableAndOnlyOursAreManaged() {
        assertEquals("book-abc.epub", SyncFileNames.book("abc", "books/1f3c.epub"))
        assertEquals("book-abc.bin", SyncFileNames.book("abc", "books/noextension"))
        assertEquals("voice-abc.m4a", SyncFileNames.voice("abc"))
        assertEquals(SyncFileNames.cover("0123456789abcdef0123456789abcdef"), SyncFileNames.cover("0123456789abcdef0123456789abcdefFFFF"))
        assertTrue(SyncFileNames.isManaged("cover-1.jpg"))
        assertFalse(SyncFileNames.isManaged("manifest.json"))
        assertFalse(SyncFileNames.isManaged("something-else.bin"))
    }
}
