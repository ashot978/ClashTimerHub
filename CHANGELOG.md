# ClashTimerHub release notes

## 7.3.0-beta1 — 4 November 2026

### New
- Widget Studio: configure each multi-account widget independently. Choose account selection and sort order, move accounts up/down, change timer text size, show or hide laboratory and pets, and select the classic, dark or translucent background.
- App update checking: see installed and latest published build numbers, release notes and a direct APK link when a public HTTPS update feed is configured. The source repository may stay private.

### Improved
- Smart Sync: copied Clash export JSON and shared/file imports now use the same player-tag-based parser. Incomplete/invalid category snapshots are rejected before existing imported timers are replaced.
- Sync feedback displays counts of builder, laboratory and pet timers.
- Screenshot scanner checks for an upgrade menu first and asks for confirmation when it cannot recognize one.

### Important limitations
- The public update feed and APK download location have not yet been deployed. They require one-time configuration outside the private source repository, documented in `docs/UPDATE_FEED.md`.
- Screenshot OCR is an assisted single-upgrade scan with user review; it is not unattended screen capture and does not scan every upgrade row.
- Widget rendering is built in GitHub Actions; visual inspection on Samsung One UI is still required.

### Safety
- Player tags are the stable account keys. Other accounts and manual timers are not overwritten by a partial Sync.
- Android installation prompts remain mandatory for APK updates.
