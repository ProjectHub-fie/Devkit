# Dev Kit — Features

Every tool listed here is implemented and unit-tested. The tool catalog
(`core/ToolCatalog.kt`) and the router (`ui/screens/tools/ToolHost.kt`) are the
only places tools become visible, so nothing appears in the UI that has no screen.

## JSON & Data
- **JSON Formatter** — pretty-print (configurable indent 1–8), minify, validate.
  Malformed input yields a readable syntax error, never a crash.
- **JSON Viewer** — collapsible tree of objects, arrays, and values; large input
  is parsed off the main thread.
- **JSON ⇄ YAML** — both directions via SnakeYAML's `SafeConstructor` (no
  arbitrary object instantiation).
- **XML Formatter** — format/validate/minify; external DTDs and stylesheets are
  disabled and secure processing is on.
- **SQL Formatter** — keyword casing and clause line breaks for common SQL.
- **CSV ⇄ JSON** — header-aware CSV parsing with quoted-field support.

## Encoders & Decoders
- **Base64** — standard and URL-safe, UTF-8, clear errors for malformed input,
  tolerant of missing padding.
- **URL Codec** — component encoding vs. full-URL encoding, both reversible.
- **HTML Entities** — named and numeric entities, encode/decode.
- **Unicode Inspector** — code points, surrogate handling, UTF-8 byte view.
- **Radix Converter** — hex/decimal/binary/octal with validation.
- **Timestamp Converter** — seconds vs. milliseconds (ambiguous values are
  flagged, not guessed silently), UTC and selectable time zones.

## Generators
- **UUID** — v4 (random) and v7 (time-ordered), single or batch.
- **Random String** — length and character sets, `SecureRandom`, optional
  ambiguity exclusion.
- **Password** — guarantees one character from each selected set; strength and
  entropy estimate.
- **Hash** — MD5, SHA-1/256/384/512 and HMAC; text is processed locally. These
  are message digests, not encryption.
- **QR Code** — generate, preview, save, or share; validates empty/oversized
  input.

## Text Tools
- **Word & Character Counter** — characters, words, lines, paragraphs, plus word
  frequency.
- **Case Converter** — upper, lower, title, sentence, camel, Pascal, snake,
  kebab, constant.
- **Text Diff** — LCS-based line diff with added/removed counts; very large
  inputs are truncated safely.
- **Regex Tester** — flags, groups, syntax errors, and a nested-quantifier guard
  to avoid catastrophic backtracking.
- **Markdown Preview** — rendered as selectable text; raw HTML is escaped.
- **Color Converter** — HEX/RGB/HSL, preview, and WCAG relative luminance.
- **Lorem Ipsum** — paragraphs, sentences, words, or lists.
- **Cron Helper** — validates 5/6/7-field expressions and aliases and explains
  each field. It does not claim an expression is safe to run.
- **Date & Time Converter** — parse ISO-8601 / RFC-1123 / common strings.
- **Developer Snippets** — save, search, edit, copy, delete locally.

## Web & API Tester
- Methods: GET, POST, PUT, PATCH, DELETE, HEAD, OPTIONS.
- Query-parameter and header editors; JSON (validated/formatted),
  form-urlencoded, and plain-text bodies.
- Timeout configuration, send, and cancel.
- Response: status, headers, body (JSON formatting), size, and duration.
- History, named collections, save/rename/duplicate/delete, search.
- Environments with `{{variable}}` substitution and unresolved-name reporting.
- Export/import in the documented `devkit.collection.v1` JSON format.

### Security controls
- TLS validation is never disabled; cleartext HTTP is blocked.
- Sensitive headers and token query parameters are redacted in history,
  previews, and exports by default.
- A cross-origin warning is available before sending sensitive headers to a
  different host.
- Responses are treated as untrusted text; no script/HTML execution.
- Response size is capped (5 MiB) and requests are cancellable.

## Saved data
Recent tools, favorites, pinned tools, snippets, collections, environments, and
history — all local, all deletable from Settings.

## Settings
System/light/dark theme, accent color, dynamic color, JSON indentation, default
timeout, redirect handling, cross-origin warnings, response wrapping, font
scale, history retention, clear history, clear all data, version info, privacy
policy link, and feedback.
