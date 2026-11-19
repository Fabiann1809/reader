<div align="center">

<img src="docs/images/icon.png" width="120" alt="Reader icon" />

# Reader

**Read. Understand. Remember.**

An Android reading companion that explains the hard parts with AI<br/>
and makes sure you don't forget what you read.

![Android](https://img.shields.io/badge/Android-8.0%2B-3DDC84?logo=android&logoColor=white)
![Kotlin](https://img.shields.io/badge/Kotlin-2.4-7F52FF?logo=kotlin&logoColor=white)
![Jetpack Compose](https://img.shields.io/badge/Jetpack%20Compose-Material%203-4285F4?logo=jetpackcompose&logoColor=white)
![Gemini](https://img.shields.io/badge/AI-Gemini%20(BYOK)-8E75B2?logo=googlegemini&logoColor=white)
![License: MIT](https://img.shields.io/badge/License-MIT-73091C)

</div>

---

## The idea

Most readers retain only part of what they read and forget much of it within days. Reader closes that loop:

```
   Read  ──▶  Understand  ──▶  Save  ──▶  Review  ──▶  Test yourself
     ▲                                                       │
     └───────────── what you miss comes back as a card ◀─────┘
```

Photograph a page of a paper book, or select text in an ebook. The text is recognized on the device, the AI explains it in plain words (Feynman method), and you save the result as a note and turn the key ideas into flashcards.

## Screenshots

<div align="center">

| Library | Book | Capture and OCR | Review | Flashcard |
|:---:|:---:|:---:|:---:|:---:|
| <img src="docs/images/library.png" width="160" alt="Library shelves" /> | <img src="docs/images/book-detail.png" width="160" alt="Book detail" /> | <img src="docs/images/capture-ocr.png" width="160" alt="Recognized text from a photo" /> | <img src="docs/images/review.png" width="160" alt="Cards due today" /> | <img src="docs/images/review-session.png" width="160" alt="Review session" /> |

</div>

## Features

### Library and reading
- **Shelves, grid or list** views, with collections, search, sorting and filters.
- **Ebooks** (EPUB and PDF) read inside the app: themes, fonts, table of contents, bookmarks and highlights.
- **Paper books** too: register them and track your page, progress and remaining time.
- **Watched folder**: books placed in it are imported automatically.

### Understand with AI
- Select text in an EPUB, mark an area of a PDF, or take a photo of a paper page.
- On-device **OCR** (ML Kit) extracts the text, warns when it is not confident, and lets you correct it.
- The AI **explains the passage simply**, using everyday analogies.
- **"Ahora tú"**: write your own interpretation and receive feedback on it.

### Notes
- **Written notes**, **AI explanations** and **voice notes** (with optional transcription), each linked to its book and page.
- Search every note by text and filter by type.

### Remember
- **Flashcards** created from notes, explanations, selected text or missed quiz questions. The AI can suggest the question and answer.
- **Spaced repetition** (simplified SM-2): each card returns shortly before you would forget it.
- **Quizzes** on a chapter, a paragraph, your cards or your notes, and missed questions become new cards.

### Progress
- Daily reading goal, streak, weekly chart and finished books.
- Per-book summary: time read, sessions, notes and cards.

### Account and Google Drive sync
- **Sign in** with email and password or with Google. Each account has its own library: another person signing in on the same phone never sees your books.
- **Optional sync with your own Google Drive.** After signing in, Reader offers to save the library to Drive; it can also be turned on or off in **Más → Cuenta**. Books, covers, notes, flashcards, bookmarks and reading progress are kept in step across devices, and the most recent change wins.
- **Limited access.** Reader requests only the Drive application-data scope: a hidden folder that belongs to the app. It cannot read or list anything else in your Drive.
- **Automatic or manual.** Changes are uploaded in the background a few seconds after they are made, optionally only on Wi-Fi, and **Sincronizar ahora** forces a pass.
- **Signing out is safe.** Reader first saves everything to Drive and then clears the phone. Signing in again with the same account restores the library, with a loading screen while it downloads. Accounts without data in Drive go straight to an empty library.
- **No servers of our own.** Your data lives on your phone and in your own Drive.

## Install

1. Download the latest `Reader-x.y.z.apk` from the [**Releases**](https://github.com/Fabiann1809/reader/releases) page, or [build it yourself](#build-from-source).
2. Open it on your phone. Android will ask you to **allow installing apps from this source**: allow it.
3. Open **Reader**, sign in, add your first book and start reading.

> Requires **Android 8.0 (API 26)** or newer.

## Get your free AI key

Reader uses **Google Gemini** with your own API key (*bring your own key*). The key is free and takes a minute to create:

1. Open [aistudio.google.com/apikey](https://aistudio.google.com/apikey).
2. Sign in with your Google account.
3. Select **Create API key** and copy it.
4. In Reader, go to **Más → Configuración → Clave de IA**, paste it and select **Guardar clave**. Use **Probar clave** to check it.

> The key is stored **encrypted** on your phone (Android Keystore) and is only sent to your AI provider when you ask for an explanation, a quiz or a transcription. It is never synced.

Everything else (library, reader, OCR, notes, reviews) works without a key.

## Build from source

**Requirements:** Android Studio (recent), JDK 17 or newer and the Android SDK (compile SDK 37).

```bash
git clone https://github.com/Fabiann1809/reader.git
cd reader
./gradlew assembleDebug        # app/build/outputs/apk/debug/app-debug.apk
./gradlew testDebugUnitTest    # unit tests
```

### Signed release APK

1. Create a keystore (keep it, and its password, somewhere safe and **out of the repo**):
   ```bash
   keytool -genkeypair -keystore ~/keystores/reader-release.jks -alias reader \
     -keyalg RSA -keysize 4096 -validity 10000
   ```
2. Create `keystore.properties` in the project root (it is git-ignored):
   ```properties
   storeFile=/absolute/path/to/reader-release.jks
   storePassword=your-password
   keyAlias=reader
   keyPassword=your-password
   ```
3. Build:
   ```bash
   ./gradlew assembleRelease      # app/build/outputs/apk/release/app-release.apk
   ```

Without `keystore.properties`, the release APK is built unsigned. Release builds are minified and shrunk with R8.

## Tech stack

| Layer | Technology |
|---|---|
| Language & UI | Kotlin, Jetpack Compose, Material 3, Navigation Compose |
| Architecture | MVVM (ViewModel + `StateFlow`), repositories, manual DI (`AppContainer`) |
| Storage | Room, DataStore |
| Reader | Readium Kotlin Toolkit (EPUB, PDF via PDFium) |
| Camera & OCR | CameraX, ML Kit Text Recognition (on-device) |
| AI | Google Gemini over OkHttp + kotlinx.serialization, behind an `AiProvider` interface |
| Background work | WorkManager |
| Account | Firebase Authentication (email and Google sign-in) |
| Sync | Google Drive REST API (application-data folder) over OkHttp, with a pure merge engine |

```
io.github.fabiann1809.reader
├── data/   Room entities, DAOs and repositories
├── ai/     AiProvider, Gemini implementation and prompts
├── ocr/    ML Kit text recognition wrapper
├── ui/     Compose screens and ViewModels, one package per feature
└── util/   small shared helpers
```

## License

[MIT](LICENSE) © 2026 Fabiann1809
