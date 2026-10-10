# Dev Kit — Database

Dev Kit uses a single local Room database (`devkit.db`) and Android DataStore
for preferences. There is no server-side database in Phase 1.

## Schema (version 1)

| Table | Purpose |
| --- | --- |
| `tool_history` | Recently used tools, for the Home "recent" row. |
| `favorite_tools` | Favorites and pinned state per tool id. |
| `snippets` | User code snippets (title, language, code). |
| `collections` | Named groups of saved API requests. |
| `saved_requests` | Saved requests: raw URL (to send) + redacted `displayUrl` (to show). |
| `request_history` | Executed requests: method, **redacted** URL, status, duration, time. |
| `environments` | Named key/value variable sets for `{{name}}` substitution. |

All entities are declared in `data/local/Entities.kt`; the DAO is
`data/local/ToolDao.kt`; the database and migration slots are in
`data/local/DevKitDatabase.kt`.

`exportSchema = true` and `ksp { arg("room.schemaLocation", ...) }` are enabled,
so generated JSON schemas are written to `app/schemas/`.

## Migrations

The database is versioned. When the schema changes:

1. Bump `version` in `@Database`.
2. Add a `Migration(n, n+1)` object and register it via `.addMigrations(...)`.
3. Keep old migrations — never edit a shipped migration.
4. Add a test that opens an in-memory database and exercises the migrated path.

A placeholder `MIGRATION_1_2` exists as the slot for the first schema change.
`fallbackToDestructiveMigrationOnDowngrade()` is intentionally set so a downgrade
does not crash; it only wipes data when moving to an older schema version.

## Data retention

History retention is a user setting (1/7/30/90 days or forever). "Clear all
data" wipes every table listed above.

## Phase 2 (optional cloud sync — not implemented)

Cloud sync is explicitly out of scope for this build. If real users request it,
the plan is documented in the app's specification: an optional free-tier backend
with per-user row-level security, no service-role keys in the APK, no syncing of
sensitive headers or tokens by default, and full account/data deletion. Until
that is built and verified, no such feature is claimed.
