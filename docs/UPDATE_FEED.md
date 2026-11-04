# ClashTimerHub private repository update feed

The source repository remains private. An installed Android APK cannot read GitHub Actions artifacts from a private repository anonymously. Do not embed a GitHub PAT or other repository credential in the APK.

## Publishing a new build

1. Wait for the Android workflow to complete successfully and verify that its APK artifact is signed with the existing signing key.
2. Distribute that APK at a publicly readable HTTPS download URL, e.g. via a separate public *binary-only* release repository. The APK (not the source) is public at that URL.
3. Publish a publicly readable JSON file `latest.json` containing the highest **successful published** build. Example (illustrative; replace the URLs and numbers with real ones):

```json
{
  "build": 286,
  "versionCode": 1286,
  "version": "7.2.0-beta1",
  "apkUrl": "https://example.org/clashtimerhub/ClashTimerHub-286.apk",
  "notes": "Widget Studio: sorting, font size, lab and pet visibility, themes.\nSmart Sync: validates sections and player tags; safeguards incomplete exports.\nUpdate checker: compares installed and published build."
}
```

4. In the app, go to Settings / Instellingen > App updates > Updatefeed instellen, and enter the public HTTPS `latest.json` URL. On subsequent settings visits the app will check no more than once every six hours; manual checking is always possible.
5. The user sees both local and published build numbers, changelog and the APK download button for a newer version. Android still needs the user's installation confirmation.

Do not publish metadata for a workflow run until its APK has been verified. Version comparison uses Android `versionCode`, NOT the display `version` string. Existing GitHub Actions workflow sets `versionCode = 1000 + github.run_number`.

## Current limitations

- This app-side support does not create the public distribution server, public release repository, or upload its APK. These need one-time hosting/deployment configuration.
- The update button currently opens the public APK HTTPS URL in the user's browser; it does not silently install APKs.
- The private GitHub Actions link remains as a fallback for signed-in repository collaborators.
- Full-screen automatic capture of Clash of Clans requires Android consent and reliable context; the current scanner accepts screenshots and copied game-data JSON. It is not a continuous screen reader.
