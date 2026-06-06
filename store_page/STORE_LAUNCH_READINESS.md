# PantryPilot Store Launch Readiness

Date: 2026-06-06

## Current Verdict

PantryPilot `1.0.3` / version code `103` is a production-candidate Android app.

Implemented:

- Signed Android App Bundle and APK.
- Consent-gated AdMob adaptive banner for Free users.
- Active US-state and European-regulations AdMob messages.
- Root-domain `app-ads.txt`.
- Google Play Billing 9.0.0 release flow for one-time Remove Ads, Plus, and Pro
  products.
- Purchase restore, completed/pending-state handling, and acknowledgement.
- Debug-only non-charging demo purchases for automated testing.
- Public privacy policy, Data Safety draft, store copy, graphics, and screenshots.

Remaining launch gates require Play Console account actions:

1. Create and activate the three one-time products.
2. Add license testers and test successful, declined, pending, restored, and
   revoked purchases from a Play-distributed build.
3. Upload version code `103` to Internal testing.
4. Complete all Play Console declarations and store listing fields.
5. Run the required closed test if the personal developer account is subject to
   Google's 12-testers-for-14-days production-access rule.

## Upload Artifacts

- App bundle: `C:\tmp\PantryPilot-release.aab`
- Signed release APK: `C:\tmp\PantryPilot-release.apk`
- Debug APK: `C:\tmp\PantryPilot-debug.apk`
- Upload keystore: `C:\tmp\PantryPilot\pantrypilot-upload.jks`
- Store assets: `store_page`

Back up the upload keystore and password source. They are intentionally excluded
from Git.

## Build Facts

- Package: `app.pantrypilot.app`
- Version: `1.0.3` / `103`
- Target SDK: `35`
- Minimum SDK: `26`
- Release billing: enabled
- Debug billing: local non-charging demo
- AdMob release unit: production
- AdMob debug unit: Google's test unit
- Direct permissions: Internet and network state
- SDK-merged permissions: Google Play Billing, advertising ID/attribution,
  wake lock, foreground service, and an internal non-exported receiver
- No precise/coarse location, contacts, storage, camera, notification, or
  account permission
- Android backup disabled
- Cleartext traffic disabled

## Public URLs

- Privacy policy:
  `https://sharpbelt.github.io/pantrypilot/PRIVACY_POLICY.md`
- App ads:
  `https://sharpbelt.github.io/app-ads.txt`
- Developer website:
  `https://sharpbelt.github.io`

## Store Listing Assets

Upload:

- `store_page\graphics\app_icon_512.png`
- `store_page\graphics\feature_graphic_1024x500.png`
- Screenshots `01_home.png` through `06_meals.png`

The upload package intentionally omits a Plans screenshot. Capture one from a
Play internal-test install only after the real products are active and prices
are visible.

## Monetization Status

Release builds use Google Play Billing for these non-consumable one-time
products:

- `pantrypilot_remove_ads_one_time`
- `pantrypilot_plus_one_time`
- `pantrypilot_pro_one_time`

The app is client-only and acknowledges purchases locally. This matches the
low-maintenance objective but is less resistant to tampering than server-side
purchase verification. A backend is recommended if fraud becomes material.

## Verification Status

- Billing 9.0.0 debug and release code compiles.
- Release bundle includes the Billing permission.
- Release and debug builds are separated by `PLAY_BILLING_ENABLED`.
- Release lint passes.
- AdMob/UMP test banner was verified on the S20.
- Final S20 regression suite passes on the current build.
- Real Billing runtime verification is impossible until Play Console products
  are active and the build is installed through Play or tested by a license
  tester.

## Production Go / No-Go

Go for uploading version `103` to Internal testing.

No-go for public production until the account-side launch gates above are
completed and real Billing tests pass.
