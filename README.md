# Reader

Android reading assistant: organize your books, track your reading, and use AI to understand what you read.

> Snap a page → extract the text on-device (OCR) → get a Feynman-style explanation → save it as a note.

**Status:** in development (MVP).

## Tech stack
Kotlin · Jetpack Compose · MVVM · Room · CameraX · ML Kit Text Recognition · OkHttp/Retrofit · kotlinx.serialization

The app uses your own AI provider API key (BYOK). The key is stored encrypted on the device and is never sent anywhere except to the provider you choose.

## Documentation
- Product spec and task list (Spanish): [`docs/PROJECT_SPEC.md`](docs/PROJECT_SPEC.md)
- Contribution rules: [`CLAUDE.md`](CLAUDE.md)

Build, API key and install instructions will be added at the end of the MVP.

## License
[MIT](LICENSE)
