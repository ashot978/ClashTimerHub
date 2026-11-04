# ClashTimerHub — Google Drive setup

ClashTimerHub 7.5.1-beta1 implements direct Google Drive backup in visible **My Drive** files. The feature uses the official Google Identity authorization flow with only the `https://www.googleapis.com/auth/drive.file` scope.

## One-time setup (repository/application owner)

1. Open https://console.cloud.google.com/ and create or choose a Google Cloud project.
2. Enable the **Google Drive API** for this project.
3. Open **Google Auth Platform** (Branding / Audience / Data Access). Complete the application name and support/contact details. If the app is in Testing mode, add each tester Google account as a test user.
4. Create an **OAuth client ID** of application type **Android**:
   - Package name: `nl.clashtimerhub.app`
   - SHA-1 certificate fingerprint: use the SHA1 line printed under **Verify original signing key is present** in the GitHub Actions job for a successfully published build. Never generate a new signing key for the OAuth client.
5. Save the client registration. **You do not need to paste any client secret into the Android source code.**
6. Install an APK built with the registered signing certificate and open **Settings → Back-up → Google-account koppelen**.
7. Select your Google account and approve access. You can now create a visible `ClashTimerHub-backup.json` in My Drive, replace an existing app-created JSON file, or restore from a selected app-accessible file.

## Backups and security

- New backups are visible JSON files in **My Drive**, not hidden in Drive's `appDataFolder`.
- The file contains accounts, player tags, upgrades, MAX status, app and widget settings. Anyone with a copy of this file can read those details. Avoid public sharing.
- The `drive.file` scope does not allow general browsing of unrelated Drive content. It can manage files this application created (or a file explicitly shared/authorized through Google Picker, if implemented in future).
- Auto-backup updates the selected file only after a successful Clash sync, while Android/Google can silently reauthorize the existing account. If Google needs an interactive sign-in, automatic backup is skipped and a descriptive error is recorded.
- Existing clipboard/local JSON backup and recovery remain available.
- A backup is uploaded only after the user chooses "New backup" or explicitly confirms replacement; opting into auto-backup authorizes future replacements of the selected target.
- Unlinking revokes Google Drive app access and clears the saved file reference without deleting files in Drive.

## Troubleshooting

- **Google sign-in fails before granting permission:** Check the Drive API enablement, OAuth consent configuration, testing users and matching APK signing SHA-1.
- **No backup files listed:** The app has not created or been granted access to any JSON backups with this Google account. Use "New Drive backup" first.
- **Could not replace a file:** The file may have been deleted, the connection may have expired, or you are signed in to a different Google account.
- **Older Downloads backup:** The earlier storage-picker implementation could save a JSON file locally. Use the existing local restore option to import this file, then create a new My Drive backup.

Google documentation: https://developers.google.com/identity/authorization/android and https://developers.google.com/workspace/drive/api/guides/manage-uploads
