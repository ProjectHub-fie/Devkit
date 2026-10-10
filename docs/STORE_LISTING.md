# Dev Kit — Store Listing Draft

_Draft only. The app is not published. Verify current Google Play requirements
before submitting._

## Short description (≤ 80 characters)

Developer tools, anywhere — JSON, Base64, UUID, regex, and a REST API client.

## Full description

Dev Kit is a fast, offline-first toolbox for developers, students, and API
tinkerers. It keeps the everyday utilities you reach for on a phone or tablet —
without an account, and without sending your data to a server.

Everything runs on your device:

• JSON Formatter — pretty-print, minify, and validate with clear errors
• JSON Viewer — explore nested data in a collapsible tree
• JSON ⇄ YAML, XML Formatter, SQL Formatter, CSV ⇄ JSON
• Base64, URL, HTML entity, and Unicode encoders and decoders
• Radix converter and a timestamp converter (seconds vs. milliseconds)
• UUID v4/v7, secure random strings, password generator, hashes and HMAC
• QR code generator
• Word/character counter, case converter, text diff, regex tester
• Markdown preview, color converter with contrast, Lorem Ipsum, cron helper
• Local developer snippet library

**API Tester**

Send real REST requests from your device: GET, POST, PUT, PATCH, DELETE, HEAD,
and OPTIONS. Edit query parameters and headers, send JSON, form, or plain-text
bodies, set timeouts, and cancel long calls. See status, headers, body, size,
and duration. Organise requests into collections, reuse environments, search
your saved requests, and export or import them.

**Private by design**

• No account required
• Local tools never use the network
• Requests go straight to the endpoint you enter — nothing is proxied
• Authorization headers and tokens are redacted in history, previews, and
  exports by default
• TLS validation is never disabled and cleartext HTTP is blocked

**Free**

All essential tools are free. An optional Pro tier (ad-free and advanced
organisation) is planned for the future — core functionality will stay free.

## Category

Developer tools / Productivity

## Suggested assets (to produce)

- App icon (adaptive) — see `app/src/main/res/mipmap-anydpi-v26`.
- Phone screenshots: Home, JSON Formatter, API Tester request, API Tester
  response, Regex Tester, Settings.
- Tablet screenshots: Tools grid with navigation rail, API Tester.
- Feature graphic 1024×500.

## Content rating / data safety

- Data safety: no data collected for the developer; user content sent only by
  user action to the user's chosen endpoint; all app data stored locally and
  user-deletable. See `PRIVACY_POLICY.md`.
