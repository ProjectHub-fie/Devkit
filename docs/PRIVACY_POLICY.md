# Privacy Policy — Dev Kit

_Last updated: 2026-10-10_

Dev Kit is an offline-first developer toolbox for Android. This policy
describes what the app actually does with your data. It is written to match the
behaviour of the code in this repository.

## Summary

- Dev Kit does **not** require an account.
- Dev Kit does **not** run a backend server. There is no Dev Kit cloud service in
  this version.
- All local tools (formatting, encoding, generation, regex, snippets, favorites,
  history, settings) process data **entirely on your device**.
- The API Tester sends the requests **you** create directly from your device to
  the URL you enter. Dev Kit is not an intermediary.

## What is stored on your device

Dev Kit stores the following locally, using Android Room and DataStore:

- Tool usage history and recently used tools.
- Favorite and pinned tools.
- Your saved API requests, collections, and environment variables.
- Request history (method, **redacted** URL, status code, duration, time).
- Code snippets you create.
- App settings (theme, accent, JSON indentation, default timeout, retention).

You can delete any of this at any time from **Settings → Clear history** and
**Settings → Clear all data**. Uninstalling the app removes all locally stored
data.

## What leaves your device

Only what you explicitly send:

- **API Tester requests.** When you press Send, the HTTP request (method, URL,
  query parameters, headers, and body you entered) is transmitted directly to
  the server at the URL you specified. That server, not Dev Kit, then handles
  your data under its own privacy policy. Do not send credentials you do not
  trust to the destination you are contacting.
- **Copy / Share.** If you tap Copy or Share, the Android system shares the
  text with the app you choose. Dev Kit does not choose the destination.

Dev Kit does **not** contact any analytics, advertising, or crash-reporting
service in this version. If that changes, this policy and the app's data-safety
disclosure will be updated before release, and consent will be handled where
required.

## Sensitive data handling

This applies to data you enter (such as tokens) and data you receive:

- Authorization headers, proxy authorization, cookies, API-key headers, and
  common token query parameters are **redacted** before being written to history,
  shown in previews, or included in exports. Redaction is applied by default.
- Exports redact sensitive headers and secret-looking bodies unless you
  explicitly opt in to include them.
- TLS certificate validation is **never** disabled, and cleartext HTTP traffic is
  blocked, so requests use encrypted connections where the server supports it.
- API responses are treated as untrusted text. Dev Kit does not execute
  JavaScript or render received HTML as live markup.
- No API keys, tokens, or secrets are embedded in the application source.

## Permissions

- `INTERNET` and `ACCESS_NETWORK_STATE` — used only by the API Tester and by the
  offline-indicator. Offline tools never make a network request.

## Children

Dev Kit is a developer tool and is not directed at children.

## Data safety (Google Play data safety section)

For the data-safety form: Dev Kit collects no data for the developer and shares
no data with third parties in this version. User-entered request content is
transmitted only by user action to the user's chosen endpoint. All app data is
stored locally and can be deleted by the user.

## Changes

Material changes to this policy will be reflected in the app and repository
history.

## Contact

Report privacy questions via the repository's issue tracker or the in-app
feedback option.
