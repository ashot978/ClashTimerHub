# ClashTimerHub release notes

## 7.4.3-beta1 — 4 November 2026

### Fixed
- **Compact account names:** Account-name cells now use only their required width (up to a reasonable maximum); builder status and timers follow immediately instead of leaving a large gap after the username.
- **Never abbreviate countdowns:** Allocated timer widths are preserved. The laboratory, pet, builder countdown and finish time are displayed in full rather than clipped with `...`.
- **Responsive layout:** The widget uses one line per account when wide enough, two lines on medium widths, and three or five lines on narrow launcher widths instead of shrinking timer fonts.
- **Readability:** The active font-size preference remains in effect while resizing.
- **Preserved behavior:** Existing accounts, countdowns, MAX checkmarks, widget sorting and transparency settings are not changed.

### Known limitation
- The amount of information that fits within a fixed widget height depends on available screen space. Make the widget taller on narrow layouts to show all seven accounts.
- Android widget dimensions are launcher-controlled; exact appearance still requires validation on Samsung One UI.


## 7.4.2-beta1 — 4 November 2026

### Fixed
- APK updates now use Android's official PackageInstaller session instead of forwarding an unverified DownloadManager content URI.
- Before installing, verify the APK is complete, matches the expected GitHub SHA-256 digest, uses ClashTimerHub's package ID, has a higher versionCode, and is signed with the installed app's certificate.
- Installation consent is shown by Android. If 'Install unknown apps' permission is missing, guide the user to enable it for ClashTimerHub.
- If an installation fails, show a useful reason (e.g. signature conflict, corrupted APK, incompatible Android version, blocked permission, or insufficient storage) instead of leaving only the generic 'App not installed' message.
- Existing accounts, timers, widgets and preferences are never deleted as part of this update.

### Note
- A build that passes GitHub Actions may still be incompatible with an already installed APK if an older build uses a different signing key. The app now detects that conflict before attempting to install.
- Android installation confirmation is still mandatory; silent installs are unsupported.


## 7.4.1-beta1 — 4 November 2026

### Fixed
- **Consistent widget text size:** resizing to narrower home-screen columns no longer reduces fonts to 8sp; the app uses a readable two-line row for narrower widths instead. For short widget heights, fewer rows may fit; increase widget height to display every account.
- **Update popup:** real line breaks, short release notes without raw Markdown and a compact up-to-date message.
- **Settings backup:** account timers, display names and MAX statuses are now joined by language, OCR timer calibration, notification preferences and widget settings.
- Old v1 backups still import. Widget presets are matched to currently installed widgets of the same type and order; a missing widget must be added to the launcher first.

### Safety
- Existing settings continue to persist through app updates without using backup/restore.
- Android still requires confirmation before installing updates.


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
