# reader — project rules

Android reading assistant (Kotlin, Jetpack Compose, MVVM, Room, CameraX, ML Kit OCR, BYOK AI).
Product spec and task list: `docs/PROJECT_SPEC.md` (written in Spanish). Always read it before starting a task.

## Workflow
1. Work on **one task at a time**, in the order of `docs/PROJECT_SPEC.md` (section 8 for the MVP, then the phases in section 9). Do not add anything that is not in the spec or the design.
2. A task or fix is done only when:
   - `./gradlew assembleDebug` succeeds,
   - relevant tests pass (`./gradlew testDebugUnitTest`, and `connectedDebugAndroidTest` when a device is available),
   - its "Hecho cuando" criterion is verified,
   - a short explanation of what was done and why is given to the user (in Spanish).
3. If anything is ambiguous, **ask the user** before assuming.

## Commits
4. One commit per task or fix. Tick the task checkbox (`[x]`) in `docs/PROJECT_SPEC.md` in the same commit, then push to `main`.
5. Format: [Conventional Commits](https://www.conventionalcommits.org/) in English: `type(scope): imperative summary` (no task id).
   - Types: `feat`, `fix`, `refactor`, `test`, `docs`, `build`, `chore`, `style`.
   - Scopes: `data`, `library`, `notes`, `ai`, `ocr`, `camera`, `ui`, `build`, `reader`, `voice`, `review`, `quiz`, `progress`, `backup`, `sync`.
   - Summary ≤ 72 chars, imperative mood, no trailing period. Example: `feat(library): add book list screen`.
6. The only author is `Fabiann1809 <leiderfabian538@gmail.com>`. **Never** add `Co-Authored-By` trailers or any AI/tool attribution to commits or PRs.

## Code
7. Everything in **English** (identifiers, comments, docs, commits) **except user-facing text**, which is **Spanish** and always lives in `app/src/main/res/values/strings.xml` — never hardcode UI strings in Compose.
8. Architecture (MVVM):
   - Packages under `io.github.fabiann1809.reader`:
     - `data/` — Room entities, DAOs, `AppDatabase`, repositories.
     - `ai/` — `AiProvider` interface, provider implementations, prompts.
     - `ocr/` — ML Kit text recognition wrapper.
     - `ui/` — Compose screens and their ViewModels (one sub-package per feature), navigation, `theme/`.
     - `util/` — small helpers shared across layers (no business logic).
   - Composables never touch repositories; ViewModels expose `StateFlow<UiState>`.
   - Repositories are the only entry point to Room, OCR and AI. Every model call goes through `AiProvider`.
   - Manual dependency injection via an `AppContainer` owned by `ReaderApplication` (no Hilt).
9. All dependency versions live in `gradle/libs.versions.toml`.
10. Security: never put API keys in code, the repo, logs or exception messages. No HTTP body/header logging for authenticated requests. Release keystore and `local.properties` stay out of git.
11. Keep code simple and readable — this is also a learning project. Small functions; comment only the non-obvious "why".
12. Tests: JUnit unit tests for ViewModels and use cases using fakes; instrumented tests for DAOs.
