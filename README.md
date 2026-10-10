# Dev Kit

**Developer tools, anywhere.**

Dev Kit is an offline-first Android toolbox for developers, students, and API
tinkerers. Format and validate data, encode and decode, generate identifiers,
test regular expressions, inspect colors, and send real REST requests — all
from your phone, without an account and without a server.

- **Package:** `com.devkit.app`
- **Category:** Developer tools & productivity
- **Platform:** Android phones and tablets (min SDK 26, target SDK 34)

---

## Features

All tools below are fully implemented and covered by unit tests. Nothing is a
placeholder.

### JSON & Data
| Tool | What it does |
| --- | --- |
| JSON Formatter | Pretty-print, minify, and validate JSON with clear syntax errors and indentation settings. |
| JSON Viewer | Explore nested objects/arrays in a collapsible tree. |
| JSON ⇄ YAML | Convert both directions using a safe parser. |
| XML Formatter | Format and validate XML with external entities disabled. |
| SQL Formatter | Format common SQL statements (not a full dialect engine). |
| CSV ⇄ JSON | Parse CSV and turn it into structured JSON. |

### Encoders & Decoders
Base64 (standard + URL-safe), URL component / full-URL encoding, HTML entities,
Unicode inspector, hexadecimal/decimal/binary/octal converter, and a Unix
timestamp converter (seconds vs milliseconds, UTC and time zones).

### Generators
UUID v4 and v7, cryptographically secure random strings, password generation
with guaranteed character sets, message digests (MD5, SHA-1/256/384/512),
HMAC, and QR code generation (preview / save / share).

### Text Tools
Word & character counter, case converter (upper, lower, title, camel, Pascal,
snake, kebab, constant), text diff, regex tester with a catastrophic-backtracking
guard, Markdown preview (scripts escaped), color converter (HEX/RGB/HSL with
WCAG contrast), Lorem Ipsum, cron expression helper, date & time converter, and
a local developer snippet library.

### Web & API
A full REST client: GET/POST/PUT/PATCH/DELETE/HEAD/OPTIONS, query and header
editors, JSON/form/plain bodies, timeouts, cancel, response viewer with status,
headers, size and duration, history, named collections, environments, search,
and import/export.

### Security & Inspection
JWT decoder (header/payload/expiry), hash identifier and constant-time compare,
URL parser, and header/sensitive-data redaction that is applied everywhere data
is logged, stored, or exported.

---

## Architecture

- **Kotlin** + **Jetpack Compose** + **Material 3**
- **MVVM** — pure tool logic lives in `core/`, UI in `ui/`, persistence in `data/`
- **Room** for history, favorites, snippets, collections, and environments
- **DataStore** for theme and preferences
- **OkHttp** for the API tester, **kotlinx.serialization** for JSON
- **Coroutines + Flow** with heavy work dispatched off the main thread

```
app/src/main/java/com/devkit/app/
├── core/          Pure, testable tool logic (no Android UI)
│   ├── json/      JsonTools, YamlTools, XmlTools, CsvTools
│   ├── encoding/  EncodingTools
│   ├── generators/GeneratorTools
│   ├── text/      TextTools, DiffTools, RegexTools, ColorTools, ...
│   └── web/       ApiClient, SensitiveData, JwtTools, UrlParser, ...
├── data/          Room entities/DAO, repository, DataStore settings
└── ui/            Compose screens, components, theme, navigation
```

---

## Build and run

### Requirements
- Android Studio (latest stable) **or** JDK 17 + Android SDK (platform 34, build-tools 34)
- Gradle wrapper is included (`./gradlew`)

### Command line

```bash
# Unit tests (fast, JVM only — no device needed)
./gradlew testDebugUnitTest

# Debug APK
./gradlew assembleDebug
# Output: app/build/outputs/apk/debug/app-debug.apk

# Install on a connected device/emulator
./gradlew installDebug
```

Open the project root in Android Studio and press **Run** to launch on an
emulator or device.

### Release signing (not committed)

Create `keystore.properties` in the project root (already git-ignored):

```properties
storeFile=/absolute/path/to/release.jks
storePassword=********
keyAlias=devkit
keyPassword=********
```

Then build a signed release bundle:

```bash
./gradlew bundleRelease
```

**Never commit keystores, passwords, or service keys.** The `.gitignore` blocks
`*.jks`, `*.keystore`, and `local.properties`.

---

## Testing

```bash
./gradlew testDebugUnitTest
```

The suite covers valid/invalid/empty/unicode/large inputs, Base64 padding and
malformed data, URL edge cases, timestamp seconds vs milliseconds, UUID
formatting, hash output, regex syntax and expensive patterns, text diff edge
cases, API timeout/cancellation/invalid URLs/HTTP errors, database persistence,
export/import validation, theme persistence, and sensitive-header redaction.

Report: `app/build/reports/tests/testDebugUnitTest/index.html`

---

## Continuous integration

The CI pipeline is defined in [`ci/android-ci.yml`](ci/android-ci.yml). It checks
out the repo, sets up JDK 17 and Gradle, runs unit tests, builds the debug APK,
and uploads the APK and test report as workflow artifacts. It fails visibly if
any step fails.

GitHub only reads workflows from `.github/workflows/`, and pushing files under
that path requires a token with the `workflow` scope. Copy the file into place
once (with a token that has `workflow` scope) to activate CI:

```bash
mkdir -p .github/workflows
cp ci/android-ci.yml .github/workflows/android-ci.yml
git add .github/workflows/android-ci.yml
git commit -m "Enable Android CI workflow"
git push
```

Alternatively, add the file through the GitHub web UI
(**Actions → New workflow → set up a workflow yourself**).

---

## Privacy and security

See [`docs/PRIVACY_POLICY.md`](docs/PRIVACY_POLICY.md). In short:

- Local tools process data on-device only. No backend is required.
- The API tester sends requests **directly from your device** to the URL you
  enter. Nothing is proxied through a Dev Kit server.
- Authorization headers, cookies, and common token parameters are redacted in
  history, previews, and exports by default.
- TLS certificate validation is never disabled and cleartext HTTP is blocked.

---

## Docs

- [`docs/PRIVACY_POLICY.md`](docs/PRIVACY_POLICY.md)
- [`docs/FEATURES.md`](docs/FEATURES.md)
- [`docs/KNOWN_LIMITATIONS.md`](docs/KNOWN_LIMITATIONS.md)
- [`docs/DATABASE.md`](docs/DATABASE.md)
- [`docs/MONETIZATION.md`](docs/MONETIZATION.md)
- [`docs/STORE_LISTING.md`](docs/STORE_LISTING.md)

---

## License

Add your chosen license here before distributing.
