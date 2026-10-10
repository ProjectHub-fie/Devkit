# AGENTS.md — Dev Kit

## Project
- Android app, single module `:app`. Package `com.devkit.app`, app name "Dev Kit".
- Stack: Kotlin 1.9.24, AGP 8.5.2, Compose + Material 3, MVVM, Room, DataStore,
  OkHttp, kotlinx.serialization, SnakeYAML (safe ctor), commonmark, ZXing.
- Gradle Kotlin DSL, JDK 17/21, `minSdk 26`, `targetSdk 34`, `versionName 1.0.0`.

## Layout
- `core/` — pure, unit-testable tool logic (no Compose). Result type is
  `ToolResult<T>` = `Success<T>` / `Failure(message)`.
- `data/` — Room (`data/local`), DataStore settings (`data/prefs`),
  `DevKitRepository` (single gateway; owns redaction rules).
- `ui/` — Compose screens, reusable components, theme, navigation.
- `core/ToolCatalog.kt` is the single source of truth for visible tools, and
  `ui/screens/tools/ToolHost.kt` routes every catalog id to a real screen.
  A `ToolCatalogTest` guards this invariant — never add a catalog entry without
  a matching `ToolHost` branch.

## Build / test commands
```bash
./gradlew testDebugUnitTest     # JVM + Robolectric tests, no emulator needed
./gradlew assembleDebug         # -> app/build/outputs/apk/debug/app-debug.apk
./gradlew lintDebug             # keep at 0 errors
```

## Conventions / gotchas
- The main activity lives in `com.devkit.app.ui.MainActivity`; the manifest must
  reference `.ui.MainActivity`. Lint flags a wrong manifest class name.
- Never change DB schema without bumping `@Database(version)` and adding a
  `Migration` (see `docs/DATABASE.md`).
- API tester: TLS validation is never disabled; cleartext HTTP is blocked except
  for hosts in `ApiClient.cleartextTestHosts` (a test-only seam). Sensitive
  headers/tokens are redacted in history, previews, and exports by default.
- No backend, no analytics/ad/billing SDKs are included. Do not add them without
  an explicit request and a privacy-policy update.
- Keep `AGENTS.md` current when architecture or commands change.
