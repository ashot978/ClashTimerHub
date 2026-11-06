# Agent instructions

## Mandatory release completion gate

For **every user-requested application change**, implement it, commit/push it to the build-triggering branch, and initiate the Android APK build in the same session. A code commit, merged PR, green checkmark on a PR, or queued/running workflow **does not mean the task is finished**.

Before saying "done", "klaar", "completed", or equivalent:
1. Find the GitHub Actions run for the **exact commit** containing the requested changes (not an earlier successful run).
2. Check the run through to final status: `status=completed` and `conclusion=success`.
3. Verify an APK artifact exists for that run. If the workflow publishes releases, also verify the new release and its installable APK asset; make sure the release targets the correct commit.
4. Only then confirm completion. Include the build number, GitHub Actions run link, and a direct APK/release download link.

If any build step fails or no usable APK is published, **do not announce completion**. Read the failing job/logs, identify and fix the cause, push the correction, and trigger another build. Repeat verification/recovery until a valid successful APK build is obtained. Keep user informed of progress. If technical/tool limits prevent further retries or verification, clearly report that the task is **not complete**, with the failure reason and the most recent run link. Never present an unverified APK as ready.

Apply this gate to visual/UI fixes as well, including in-game floating overlays; changing a widget or feedback toast is not sufficient if the requested on-screen control remains unchanged. Preserve the widget's icon design when the user asks to reuse it.

Keep the application ID and signing identity stable and increment versionCode for updates. Preserve user account data.

Fresh-install default account names, editable: Ashot, Ashotgun, AshotRush, Rushot, Shotty, Ashotty, RushShot. Never include account email addresses.

Provide account add, rename, and delete operations. A single tap on the floating Scan nu button while the Clash of Clans upgrades menu is open should read and store active timers.
