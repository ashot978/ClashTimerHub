# Sync roadmap (proposals, not implemented)

## Status voor appgebruikers · 17 november 2026

- **Al beschikbaar:** upgrades en timers, vrije builders, widgets, meldingen en Sync voor Squads, Guardians, helpers en potions.
- **Gekozen, nog niet gebouwd:** Hero Equipment. Levels per account, Common/Epic en benodigde Shiny, Glowy en Starry Ore.
- **Gekozen, nog niet gebouwd:** Verzamelingen. Decoraties, obstakels, skins en sceneries per account.
- **Nog uitzoeken:** of de nieuwste Clash-export alle Equipment- en verzamelgegevens bevat.
- **Overige ideeën:** Town Hall-voortgang, upgradegeschiedenis, upgradeplanner en Builder Base.

De roadmap wordt **meegeleverd met appupdates**. Een gepland onderdeel is nog geen werkende functie. Voor nieuwe spelergegevens moet je nog steeds een actuele Clash-export synchroniseren.

---


## Next selected features - Hero Equipment & Collection Tracker (17 Nov 2026)

Builder availability already exists in Clash Timer Hub and is **not** a requested new feature.

### Feature 6: Hero Equipment

- Per-account equipment by hero; show owned equipment name, level, rarity, and max level.
- From verified level + current game-data cost table, calculate Shiny, Glowy,
  and Starry Ore needed for the **next** level and remaining to max.
- Mark costs as estimates until current game's ore prices are verified.
- Do not claim to know ore balances when they aren't in the export.
- Highlight equipment that increased in level since the prior sync.
- Require actual synced `equipment`/matching-equipment section before displaying levels.
- Do not mistake equipment instance `id` for the game item `data` ID.

### Feature 10: Collection Tracker

- Per-account owned decorations and obstacles for Home Village and Builder Base.
- Owned hero skins and sceneries where the export *explicitly* lists unlocked items.
- Show Owned / Not found in export, grouped by type; do not equate
  "not found" with "not owned" when exports have incomplete coverage.
- Resolve export IDs against an updated asset catalog (game-data fingerprint,
  name, category, release year and optional artwork). No guessed mappings.
- Keep collectible/seasonal obstacles separate from ordinary spawn obstacles.
- Preserve earlier category snapshots if newer exports omit that category.
- Never identify a selected scenery as "owned" merely because a background ID
  appears in the active-layout setting.

### Initial technical work

1. Confirm **actual JSON structure from at least one current 2026 village export**.
   A read-only local schema inspector may report section names and field shapes
   without uploading tags, player metadata or the raw JSON.
2. Save per-player-tag equipment and collection state locally on Sync.
3. Build two distinct account-filterable screens, accessible without breaking
   the existing Overview/Timeline/Settings navigation.
4. Add offline game-ID mapping, ore cost validation and tests for missing/partial
   exports; make backups retain these snapshots.
5. Retest each of the user's seven accounts and publish an APK only after tests.

References:
- https://www.clashmetic.com/tracker (village JSON based cosmetics collector)
- https://clashprogress.com/villages (equipment levels in JSON)
- https://github.com/ClashKingInc/ClashKingAssets/tree/main/assets/static_data

**Status:** selected for development, not implemented by this roadmap-only commit.

---


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
