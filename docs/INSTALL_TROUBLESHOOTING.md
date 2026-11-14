# Android installation troubleshooting

## Build 553 report (14 November 2026)

Build #553 was published successfully (run #553, versionCode 1553), and GitHub
released its APK with SHA-256 digest
`7f31b4e2c85539a36626c0e0009ef2f848acce083c4f067a42bff8cedcd1767d`.
Successful CI verification does not prove that every existing Android installation
can be updated.

## Safe triage

1. Do not uninstall Clash Timer Hub. Keep the existing app data.
2. In Clash Timer Hub, open Settings > App updates > Update diagnosis.
   Record the installed build number, APK install permission and last error.
3. Check that the installation comes from the published GitHub Release
   rather than an Actions artifact, cached browser download or ZIP.
4. When updating from inside the app, try the native 'Via Android installeren'
   fallback if the app offers it. The APK is verified again.
5. If using the browser APK, Android may require 'Allow from this source'
   for that browser or file manager (not only for Clash Timer Hub).
6. If Android still reports 'App not installed', obtain the complete error
   from Update diagnosis or the Android installer. Do not assume the reason:
   installed versionCode, signature, package, free storage, incomplete
   downloads and device policy can all matter.
7. Compare the installed versionCode with the candidate's versionCode.
   Android rejects same-or-lower versionCodes as an upgrade.
8. If the installed build uses a different signing certificate, Android cannot
   install over it. Never advise uninstalling until the user has a verified
   restorable backup.

Builds after this troubleshooting change retain the previous installation
failure in the app's diagnostic screen until an installation succeeds.
