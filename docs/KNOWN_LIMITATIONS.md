# Dev Kit — Known Limitations

This document lists what Dev Kit deliberately does **not** do, so the app never
over-promises. Items marked _planned_ are not implemented yet.

## By design

- **No account, no cloud sync.** All data is local. Cloud sync is _planned_ for a
  future phase and will be optional.
- **No backend.** Local tools never require a network connection.
- **SQL Formatter** formats common statements; it is **not** a full
  dialect-aware engine (no vendor-specific procedural syntax).
- **Cron Helper** validates and explains expressions. It does not execute them
  and cannot guarantee an expression is safe to run.
- **Hash tools** compute message digests and HMACs. A general-purpose hash is
  **not** encryption and is not reversible.
- **JWT Decoder** decodes and inspects a token. It does **not** verify the
  signature, because Dev Kit does not hold your signing keys.
- **Random String** uses `SecureRandom`, but only the dedicated Password
  generator guarantees inclusion of each selected character class.
- **Markdown Preview** renders to escaped HTML shown as text. It does not run a
  browser or execute embedded scripts/HTML.
- **QR Code** generation is local; scanning is not part of this build.

## API Tester

- Cleartext `http://` requests are blocked by design.
- Responses are capped at 5 MiB; larger responses are refused with an
  explanatory error to protect memory on low-end devices.
- Client-certificate (mTLS) authentication is not supported.
- WebSocket and gRPC are not supported; this is a REST/HTTP client.
- Locally stored secrets are only as safe as the device. They are not
  hardware-encrypted per value in this version.

## Platform

- Minimum Android 6.0 (API 26). Core library desugaring is enabled for
  `java.time`.
- Tested on portrait and landscape layouts and on compact and expanded widths
  (bottom navigation vs. navigation rail). Tablet-specific multi-pane layouts are
  _planned_.

## Monetization

- No ads and no billing are included in this build. The Pro tier and AdMob are
  _planned_ only; see `MONETIZATION.md`. Core functionality is not gated.

## Verification status

- Unit tests pass (see `README.md`). Instrumented UI tests are minimal.
- The app has **not** been published to Google Play, and no cloud service or
  hosted APK download is live. Any claim otherwise would be inaccurate.
