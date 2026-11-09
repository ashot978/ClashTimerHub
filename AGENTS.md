# Agent instructions

## Mandatory release completion gate

## Unconditional build after EVERY prompt

## Mandatory fresh release notes for every build

**Every newly published APK MUST include release notes written for that build, not reused historical notes.**

### Concise, fully readable in-app release notes

The in-app update dialog must show every release-note bullet without artificially
truncating bullet text or discarding bullets. The Android message area stays scrollable.
For every future release, write **more short bullets rather than fewer long paragraphs**:
- Aim for at most 90 characters per bullet, including spaces.
- Keep each bullet to one specific change, in plain user-friendly language.
- Split longer explanations into separate concise bullets; do not shorten by cutting off text.
- Put technical investigation and lengthy background in commit messages, not release notes.
- Keep both NL and EN sections factual, scannable and comparable in length.
- Never show literal ellipses in place of a cut-off change.

 Before committing feature or bug-fix changes, update `RELEASE_NOTES.md` with accurate, concise bullet points under both `### NL` and `### EN`. Describe only changes actually made in the current request; do not carry forward removed features or older instructions. Keep the language understandable to app users.

The GitHub Actions publish workflow compares this file against the previous release. If it was changed, the new bilingual notes are published. If it was not changed (such as an instructions-only build or an unchanged-source rebuild), the workflow must instead generate fresh build-specific notes from the new commit subjects, or explicitly say there are no new features. `CHANGELOG.md` is a history archive only and MUST NOT be used as the release body for every build.

Before saying the task is complete, inspect the newly published GitHub Release `body` in addition to checking the signed APK: ensure it includes the current build number and actual changes, has `### NL` and `### EN`, and does not claim that old removed functionality was added. An incorrect or reused release body is a build/publication failure to fix and rebuild.

**Non-negotiable: EACH NEW USER PROMPT addressed to the Clash Timer Hub Maintainer MUST result in a NEW Android APK build.** This applies to every request, without exception: code changes, UI tweaks, bug reports, instruction/agent changes, reviews, questions, clarification requests, and prompts that require no source changes. Never reuse an older build as proof of completing a new prompt.

- For prompts that change the repository, commit the requested changes to `main` (or merge a PR to `main`), which triggers the APK GitHub Actions workflow.
- For prompts that do not change source files, trigger a fresh `workflow_dispatch` run of `.github/workflows/build-apk.yml` against the current `main` commit. If dispatch is not available, make an appropriate meaningful, transparent repository update that triggers the normal push build; never pretend an old run is new.
- Verify that the run was launched **in response to this specific prompt**, capture its run ID and run number, and confirm that it creates a newly published and installable APK for the intended commit.
- Report completion only when the new run finishes successfully AND the APK artifact and matching release asset are verified. A queued, running, failed, or missing-artifact build does NOT count.
- When a run fails or APK publication fails, inspect logs, fix what is wrong, and launch a **new build**. Retry until successful. If blocked by external permissions, unavailable tools, or other irrecoverable limitations, clearly state that the prompt is **not yet completed** rather than declaring success.

This policy also applies when the user asks to change these agent instructions: commit the instruction update, run a fresh build, and verify it before claiming completion.

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
