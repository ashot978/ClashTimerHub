# Monetization closed-test branch (not for production)

This branch is isolated from main. The CI workflow compiles and tests the two distributions
without publishing a GitHub release or signing/uploading a Play AAB. No signing secrets are
needed for this verification; do not add keys or passwords to Git.

## Behavior

- Official Google demo AdMob app ID and App Open ad unit ID ONLY. No live ad revenue.
- One ad opportunity on the first direct launcher creation per process, after the first-ever
  launcher start. Returning from Clash, resuming an existing process, configuration changes,
  widget/deep-link entry and overlay imports never trigger ads.
- A visible loading screen, maximum four-second attempt before main content; Continue skips.
  If the user backgrounds the app, the ad is unavailable, consent is missing or Play ownership
  is unknown, skip the ad. No ad may arrive after main content is shown.
- UMP consent request and a privacy options entry where required. Consent form completion
  goes directly to the main screen; no ad immediately after the consent form.
- Premium is a non-consumable one-time product with ID remove_ads. Use a single permanent
  buy option, intended NL base price EUR 2.99, no subscription. Show localized Play price.
- Query purchases on resume and restore, acknowledge PURCHASED transactions, never consume,
  do not grant pending purchases. Cached premium is separate from user-exported timer data;
  never show ads if ownership is unknown. Client entitlement is a TEST prototype, not a
  production purchase verifier.

## Play Console setup / safe testing

1. Wait for the reset upload certificate to be approved and configure the existing four
   Actions signing secrets. Do NOT replace the pinned certificate before approval.
2. In Play Console create and activate the one-time product remove_ads with the single
   permanent buy option and EUR 2.99 base price. Finish merchant/payment profile setup as
   required. Add every account that will test purchases to Play Console license testing
   and the relevant test track.
3. The purchase button is disabled by default to prevent charging ordinary closed testers.
   Only for a controlled build distributed to license testers, pass
   -PenableMonetizationTestPurchases=true to Gradle. Confirm the Play payment sheet explicitly
   shows a test payment instrument. The app cannot independently establish that an account
   is a license tester; do not distribute this enabled test build to other users.
4. Test: new install first launch without ad; kill process and launch again; skip; offline;
   switch to Clash and back; widget/overlay launches; rotate; deny consent; ad load timeout;
   test success, cancel and pending payment; restore after reinstall with same account;
   refund/revocation and failed ownership query. Verify both English and Dutch, and Fold layout.

## Before production (separate review, not yet configured)

- Create AdMob app and App Open ad unit, configure EEA/UK/Swiss consent message and
  privacy choices, verify store linkage/ad readiness, implement app-ads.txt on the developer
  website and update public privacy policy, Play Data safety, ads declaration and SDK disclosures.
- Confirm app audience and applicable child-directed treatment before any production ads.
- Add server-side purchase token verification and durable entitlement/refund handling via
  Google Play Developer API (no service credentials in the app). Keep purchase off until
  the test and production security review is complete. Only then replace demo IDs with
  the approved AdMob IDs in an explicit production change.
- Keep GitHub APK distribution free of ads and Play Billing dependencies. Do not publish
  or merge this prototype before review and device testing.

Official references:
https://developers.google.com/admob/android/app-open
https://developers.google.com/admob/android/privacy
https://developer.android.com/google/play/billing/integrate
https://developer.android.com/google/play/billing/test
