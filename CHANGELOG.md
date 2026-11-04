# ClashTimerHub release notes

## 7.4.0-beta1 — 4 November 2026

### New
- **Updates without ZIP files:** GitHub automatically publishes every successful APK build as a public Release.
- **In-app update checks:** ClashTimerHub reads the latest GitHub Release directly; no extra feed, GitHub login, or API token is required.
- **One-tap update start:** compare installed and latest build numbers, read release notes and download the APK from the app. Android still requires the user's confirmation for installation.
- **Automatic reminder:** on opening the app, check at most once every six hours and offer a newer published build once per release.
- **Fallback:** Settings links directly to the latest GitHub Release, never the Actions ZIP artifact.

### Safety
- The installer updates the existing app; local accounts and timers are not deleted.
- The build workflow refuses to build if the previous signing key is missing.
- GitHub must finish a successful build before the new APK is published.


## 7.3.1-beta1 — 4 November 2026

### Fixed
- Multi-account widget supports a **two-column** home-screen size with a two-line account row. All the key information remains available: account name, builder status, lab, pets, next countdown, finish time.
- Widget Studio **Comfort/Large** text now enlarges the account name and main countdown rather than every secondary value, preventing overlapping or cramped labels.
- **Transparent/Dark** themes also apply to every account row and the Sync footer, not only the outer card.
- The transparent widget background is lighter to reveal more of the wallpaper.
- Floating Sync feedback shows account, timer counts, and result in a small readable card; literal `\\n` is fixed at both the source and display.
- Existing widgets and account data remain unchanged after installing the update.

### Verification
- APK is compiled and signed by GitHub Actions. Samsung One UI launcher sizing and theme appearance still need on-device confirmation.


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
