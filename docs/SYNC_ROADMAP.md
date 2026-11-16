# Sync roadmap (proposals, not implemented)

Reviewed against `QuickSyncImporter.java` on 17 Nov 2026. The app currently reads
active timers from `buildings`, `heroes`, `units`, `spells`,
`siege_machines`, `squads`, `pets`, `traps`, `guardians`; helper
levels and cooldowns; and optional potion and snack boost durations.
The import does not persist the complete level/collection snapshot.

## Proposed backlog (ranked by immediate value for seven accounts)

| Priority | Idea | Source and logic | Critical validation |
|---|---|---|---|
| P0 | Free builder / laboratory / pet slots, and next available slot | Existing in-progress timers per account, total slots configured by player | Active builders outside normal upgrades (e.g. Forge) may not show in exported timers; show estimated/unknown instead of claiming definitely free |
| P0 | Save per-account complete level snapshot | Export arrays with `data`, `lvl`, and counts; preserve previous snapshot when a section is missing | Distinguish temporary/seasonal, Builder Base vs Home Village; validate source shape first |
| P0 | What changed since last sync? | Diff level, quantity and timer snapshots by player tag and entity ID | Never interpret missing section as removed upgrades |
| P1 | Town Hall progression and per-category completion | Export levels + versioned game max-level reference | Separate TH max from absolute max; don't mix temporary units |
| P1 | Upgrade queue / next recommended upgrades | Levels + costs, unlock TH, duration reference tables | Cost may change through Gold Pass and live events; estimates only |
| P1 | Squad & Guardian progress screen | Confirmed new GlobalIDs for 12 squads, guardians mapped via game-asset row index | The `squads` section in the **live export** still needs a real sample |
| P1 | Hero equipment, ores required to upgrade | Equipment levels + game-cost tables | Ore inventory may not be present in export; avoid claiming current balances |
| P1 | Builder Base separate progress and timers | `buildings2`, `heroes2`, `units2`, `traps2` from a verified export | Do not classify Builder Base timers as Home Village builders |
| P2 | Progress charts and historical maxing velocity | Saved timestamped local snapshots | Storage usage; compact history; local retention preferences |
| P2 | Collection tracker (skins/sceneries/decorations) | Collection/ownership sections where present, plus asset catalog | Specific field names and coverage require actual exported JSON |
| P2 | Better notifications for overnight idle time | Derived from known end times and configurable quiet hours | Can't reliably know true idleness without all busy roles |
| P2 | Smarter cross-account next upgrade suggestions | Current levels and next cost/time plus user priorities | Suggestions only, cannot start upgrades in-game |

## Implementation order

1. First, write a **read-only export schema inspector** that lists section names,
   item counts, known fields and unknown fields from an actual export, locally.
   Do **not** log tags or raw player data. Store none of the raw JSON for diagnostics
   unless the player explicitly exports it.
2. Build a versioned level snapshot and safe partial-export merging.
3. Implement account availability and visible confidence labels, then progress.
4. Add a game-data catalog with the exact asset fingerprint and maximum levels.
5. Add recommendations, forecasts, history and collections.

## Scope boundary

The in-game Data Export is a **snapshot copied manually**, not a live API stream.
A single tap on Clash Timer Hub's overlay imports the latest clipboard snapshot;
it cannot retrieve fresh level changes without a new game export.

### External references
- Clash Ninja: https://www.clash.ninja/blog/one-tap-village-updating-is-here
- ClashProgress: https://clashprogress.com/villages
- ClanWarden: https://clanwarden.com/blog/clash-of-clans-upgrade-tracker
- Existing game ID snapshot: [TH19 export IDs](datamine/TH19_EXPORT_IDS.md)
