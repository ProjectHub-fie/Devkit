# Dev Kit — Monetization Plan

This document describes the intended monetization approach. **None of it is
active in this build.** No ads, no billing, and no client-side premium flags are
present, and core functionality is never gated behind payment.

## Principles

1. Essential offline tools stay free forever.
2. No fake payment screens and no fake purchase success.
3. Entitlements are verified, not stored as a client-side boolean.
4. Monetization must not degrade the core editing and copying experience.

## Free edition (what ships today)

All essential offline tools, JSON formatting and validation, encoding and
conversion, generators, basic regex and text utilities, basic API requests,
local favorites and history, and dark/light themes.

## Dev Kit Pro (planned)

Candidate premium features:

- Ad-free experience.
- Advanced API collections (larger limits, extra organisation).
- Advanced export/import options.
- Custom themes and editor preferences.
- Additional productivity features.

### Purchase model

Prefer a **one-time purchase** initially over a subscription.

### Implementation requirements (when built)

- Use **Google Play Billing** when distributing through Google Play.
- Verify purchase entitlements on-device through the Billing Library, and
  restore eligible purchases on new devices.
- Do **not** gate premium behind a locally stored flag that a user could flip.
- Do not ship paid functionality until the purchase flow and Play distribution
  requirements have been verified end to end.

## Advertising (planned, if ever)

If AdMob is integrated later:

- Use **test ads** during development; never click or encourage clicks.
- Never place ads where they could be mistaken for tool controls.
- No intrusive interstitials during editing or copying.
- Keep ads out of sensitive-input screens.
- Do not serve personalised ads without handling applicable consent.

## What is explicitly not done

- No billing library is included in this build.
- No ad SDK is included in this build.
- No premium flags or paywalls exist in the code.
- The app is **not** published and monetization is **not** approved.

## Before enabling monetization

Verify current Google Play developer registration, testing track, payment, and
distribution requirements. Those requirements change over time and must be
re-checked at implementation time.
