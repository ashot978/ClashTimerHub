# Agent instructions

## Google Play builds

**A Google Play update requires a verified `.aab` from the `playRelease` variant.**
The GitHub updater uses the APK from `githubDebug`. Successful APK builds do not satisfy a Play request. Use the existing package ID `nl.clashtimerhub.app` for both distributions.

1. Check App Bundle Explorer in Play Console for the highest version code already used across tracks. Use a strictly higher unused code. The first verified Play builds used `1570` and `1575`. Never infer the next Play code from the GitHub APK build number.
2. Run `.github/workflows/build-play.yml` on the requested commit on `main` with that `version_code`. Use only the original AshLab Studios **Play upload key**, alias `clashtimerhub-upload`. Its SHA-256 certificate fingerprint is `8A:EE:DB:5B:7D:7A:47:5F:B9:D8:42:06:D4:4A:4F:C5:A6:CC:B2:D7:42:2D:7E:17:41:40:4B:08:27:B3:34:3E`. Compare the upload certificate in Play Console if Google reports a mismatch. Keep the upload key separate from the GitHub APK signing key and the app signing key managed by Google Play.
3. Use the four Actions secrets declared in the Play workflow. Keep signing material out of commits, release attachments, logs and chat. If the workflow or its secrets are unavailable, build locally only with a securely available copy of the original upload key. If the original key is unavailable, stop the Play build and report the blocker. A key rotation requires explicit approval and verification in Play Console.
4. Run the Play release unit tests and `.github/scripts/verify-play-bundle.sh` against the finished signed file. The verifier checks the certificate and signature, package ID, requested version code, Android API 36 or newer, release flags, 16 KB alignment, and removal of the APK installation permission and receiver. Keep the GitHub updater disabled in the Play variant. If any check fails, do not distribute the bundle.
5. Verify the workflow run and downloaded artifact against the exact source commit. Report the Play version code and provide the verified `.aab` link. For a local build, state the source commit and that it was built locally. Report Play Console acceptance only after it is observed. Upload to a Play track only when the user requests it.

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
