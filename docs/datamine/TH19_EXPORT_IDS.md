# Clash of Clans 16 November 2026 ID datamine

Fingerprint: `0f8af5ebc59dc7b88f884643c1fb8b57928113d6`

The underlying CSVs are downloaded from Supercell's game asset CDN by
[tools/datamine_ids.py](../../tools/datamine_ids.py) and extracted into
[game_ids_2026-11-16.json](./game_ids_2026-11-16.json).

**ID confidence:** buildings, spells, and characters use explicit `GlobalID`.
Guardians and seasonal defense archetypes/modules encode IDs as their CSV row
index. The row-index rule is cross-checked against older exported IDs:
Logger `107000008`, Roaster `103000008`, Hot Candle HP `102000033`.

## All 12 squads
| ID | Name |
|---|---|
| 4000195 | Barch Squad |
| 4000196 | Giant Wizard Squad |
| 4000197 | Wall Breaker Squad |
| 4000198 | Minion Squad |
| 4000199 | Balloon Squad |
| 4000200 | Golem Witch Squad |
| 4000201 | Hog Rider Squad |
| 4000202 | Miner Squad |
| 4000203 | Healer Squad |
| 4000204 | P.E.K.K.A Titan Squad |
| 4000205 | Dragon Squad |
| 4000206 | Baby Dragon Squad |

## New TH19 entities
| ID | Name |
|---|---|
| 1000105 | Guardian Shrine (asset name Guardian Building) |
| 1000106 | Squad Barracks |
| 1000107 | Spiky Tower |
| 1000108 | Firecracker Tower |
| 26000153 | Heroic Spell |

Other additions: `4000207`–`4000212` new event characters;
`26000149`–`26000154` recent spell and effect IDs;
`103000000`–`103000016` seasonal defense archetypes;
`102000000`–`102000050` seasonal module IDs;
`107000000`–`107000009` Guardian IDs.

The app maps all of these where relevant and retains user-selected timer names.
Other unknown ID strings remain numeric until a source confirms them.
