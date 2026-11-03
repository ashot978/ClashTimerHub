# Clash Timer Hub V4 — Product & engineering specification

Status: implementation blueprint, not a claim that V4 is implemented or tested.

## Goal
Android-first Clash of Clans upgrade command center for 0–20+ player accounts. In under a second the user should identify which account needs attention next, with trustworthy local timers sourced from the game's Copy Player Data JSON.

## Non-negotiable invariants
1. Player tag (normalized uppercase with leading #) is the immutable account key. Display name is editable and never used to identify imports. If no tag, require explicit user confirmation and do not silently merge accounts.
2. Every JSON import is an atomic, per-account snapshot. Validate and parse fully before replacing that account's previous imported snapshot. Never modify another account.
3. A valid snapshot with zero active upgrades clears prior imported upgrades for that account; preserve manually created timers separately.
4. Preserve each upgrade's source path, ID, level, optional module/parent IDs, source timestamp, and computed completion time. Multiple upgrades with the same game ID are distinct (e.g., three Hidden Teslas).
5. Never infer free builder count from active upgrades alone without a trusted capacity. Mark unknown explicitly.
6. Timer duration comes from the export timestamp (epoch seconds) + remaining seconds, not the device's import time; reject implausible timestamps and flag stale data rather than silently resetting timestamps.
7. Import and restore never destroy the only recoverable copy: create a local rollback snapshot before destructive operations.
8. User notifications are optional and permission-aware; all schedules are restored after reboot and resync.
9. A fresh install has zero accounts. Existing installations retain saved accounts.
10. All displayed numbers and names are derived from stored data; no demo accounts/timers in production.

## Domain model
Account {id: normalizedPlayerTag or local UUID, playerTag?, displayName, isFavorite, order, builderCapacity?, lastSyncAt?, lastExportAt?, syncStatus}
Upgrade {id, accountId, category: BUILDER|LAB|PET|OTHER, sourcePath, sourceDataId, parentDataId?, instanceIndex, level?, displayName?, startTime?, finishAt, imported: boolean, sourceFingerprint}
ImportResult {accountId, createdAccount, sourceTimestamp, builderCount, labCount, petCount, otherCount, unknownNames, warnings}
Settings {notificationLeadMinutes, notificationEnabled, quietHours, sortMode, compactCards}

Migrate current SharedPreferences JSON carefully, preserving linked playerTags and manual timers. Prefer Room database with versioned migrations; do not change persistence format without a migration test.

## Import pipeline
- Input adapters: Android clipboard, document picker, backup clipboard/file (backup has distinct format and must not be accepted as a Clash export).
- Parse game export; normalize tag; optionally read player name; match account by tag.
- On new tag: create account automatically, default displayName to supplied player name or tag; never silently attach to selected unrelated account.
- If no tag: ask for an explicit account choice or creation. Do not auto-match by name.
- Recursively traverse supported sections and nested building types/modules; include siege_machines and other relevant timer-bearing sections if verified; ignore inventory-only records with no active timer.
- Treat each timer occurrence independently. Distinguish separate building instances with identical source IDs using deterministic path/index identity for the snapshot.
- Distinguish active timers, expired timers and absent timers. Empty active snapshot is a successful import.
- Validate timestamps (seconds vs milliseconds), timer bounds and JSON size. Display a clear warning for stale exports.
- Commit atomically: save rollback, replace imported timers for matching account only, update lastSyncAt and metadata, recalculate alarms/widget.
- Return a summary of counts and warnings, including unknown IDs.

## Main navigation
Home | Timeline | Settings, with account detail pushed from Home. Clipboard sync is prominent on Home and account detail. No scanner/OCR UI or dependency.

### Home
- Hero: next actionable completion across all accounts, with account and upgrade name.
- Summary: accounts, active builder upgrades, upcoming completions.
- Account cards: name, tag, next completion, builder active count, lab/pet counts, last synced, stale indicator.
- Tap card opens account detail (not merely changing a spinner below the fold).
- Sort: soonest, manual, name. Filter favorites; compact/expanded card density.
- 0 accounts: clear onboarding CTA to copy and import Clash data.
- 0 active timers: show “no active upgrades” not “free builders” unless capacity known.

### Account detail
- Header: editable display name, tag, last sync, sync button, account settings.
- Builder status: active and free/total only if capacity known; configure capacity manually if unknown.
- Tabs BUILDER/LAB/PET/OTHER; list every active timer sorted ascending, no artificial “earliest only” filter.
- Each row: human-readable name (or honest unknown-ID fallback), level if available, remaining time, completion local time.
- Manual timers clearly distinguished from imported ones. Avoid misleading progress bars if start time is unknown.
- Optional archived/completed list and explicit edit/delete controls.

### Global timeline
- Chronological merged list grouped by Today/Tomorrow/Later; account badge, category, name, remaining time.
- Filter by account/category; tap to open account detail.
- Do not fabricate events or inferred future upgrade starts.

## Names and categories
Maintain versioned ID mappings with source and tests. Preserve unknown IDs as “Unknown upgrade (ID ...)” and report them in import summary. Known mapping example: buildings 1000015 Builder's Hut, 1000019 Hidden Tesla. Nested module 102000038 needs verification before naming. Category from source section, with exceptions documented and tested; nested modules under buildings are builder-related unless confirmed otherwise.

## Builder capacity
User-configurable total builders per account; optionally derive from verified export metadata. Free = max(0, capacity - active regular builder tasks) only when every relevant builder task is correctly classified. Display “capacity unknown” when not reliable. Goblin Builder and helpers are separate from normal builders.

## Notifications
- Per-account and global preferences; completion / 15m / 30m lead times, quiet hours and grouped notifications.
- Stable notification IDs per timer; reschedule on import, edit, reboot, timezone changes as appropriate.
- Android 13+ notification permission, modern exact-alarm restrictions and fallback strategy.
- Do not claim live sync; local timers are based on last import.

## Widgets
Small: next upgrade; medium: next 3 accounts; large: scrollable multi-account list where platform supports it. Taps deep-link to account. Handle no data, stale data and completed timers. Refresh within Android background limitations; no claim of second-by-second widget updates.

## Backup and recovery
- Export all accounts, settings, tags, timers and schema version as JSON to clipboard/file/share.
- Restore validates schema and counts before a confirmation screen; rollback snapshot is created first.
- Restore preview shows number of accounts/timers and whether current data will be replaced.
- Explicit import-vs-backup separation. Future per-account export optional.
- Backward compatibility with clash-timer-hub-backup-v1.

## Quality and release gates
- Unit tests: zero-account first run, one/multiple account imports, tag routing, duplicate names, repeated import, empty timer snapshot, nested modules, identical building IDs, timestamp math, stale snapshot, backup roundtrip, old data migration.
- Fixture based on real provided export: 6 active builder-related timers (including nested module 102000038), 1 active unit timer, 1 active spell timer; do not confuse 1/7 free builders with 1/7 busy.
- UI tests: Home card opens detail, all six timers visible, sorting, no preloaded accounts, empty state, sync summary.
- Build: clean Gradle debug build, lint, unit tests, GitHub Actions artifact; inspect failures and fix before publishing.
- Release only after passing gates. A successful GitHub commit alone is not a verified APK.

## Implementation sequence
A. Extract domain/import engine, fixture tests and storage migration.
B. Build Home + account details + timeline from the same repository model.
C. Add sync summary, settings, notifications, widgets and backup recovery.
D. Remove OCR/legacy scanner, finish automated checks, build and test one integrated V4 APK.

## Explicitly out of scope
Automated extraction directly from Clash game screens, unverified Supercell APIs, cloud account synchronization, automatic game actions, and inferred builder counts without evidence.
