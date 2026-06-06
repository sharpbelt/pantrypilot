# PantryPilot

Production-track Android app for smart pantry inventory, expiry tracking, grocery lists, and meal ideas.

## Outputs

- Debug APK: `C:\tmp\PantryPilot-debug.apk`
- Signed release APK: `C:\tmp\PantryPilot-release.apk`
- Release app bundle: `C:\tmp\PantryPilot-release.aab`
- Prod-test report: `C:\tmp\PantryPilot\PROD_TEST_REPORT.md`
- Package: `app.pantrypilot.app`
- Version: `1.0.2` / version code `102`
- Visible app name: PantryPilot
- Marketing line: your smart pantry pal

## MVP Features

- App-like categorized dashboard with expandable sections
- Photo-assisted scanner workflow for import photo / take photo -> review -> add pantry item
- Label text parser for text copied from camera OCR, labels, receipts, or notes
- Packaging text parser with a built-in sample parser test inside the scanner
- Android camera capture stores a full-size local scan image through the app's private image provider
- Pantry/fridge/freezer inventory
- Expiry dates and "expiring soon" summary
- Categorized grocery list with checked item cleanup
- Add to list action from pantry item to grocery list
- Local text sharing for pantry inventory and grocery lists
- Local meal ideas from owned ingredients
- Free, Plus, and Pro plan gating with a local demo purchase/restore flow
- Consent-gated AdMob adaptive banner for Free users, standalone Remove Ads demo unlock, and paid plans that hide ads
- Privacy and About dialogs
- Local-first storage
- Internet/network-state access is used only for consent and AdMob; no contacts, location, or account permissions
- Scanner images stay local; Android production image-to-text recognition still needs an OCR provider integration and updated disclosures

## Plans And Demo Purchase

The current APK exposes plan restrictions and a local demo purchase flow:

- Free: 12 pantry items, 12 grocery items, sample scanner preview, and 2 meal ideas.
- Remove Ads: one-time ad-free entitlement for users who want Free limits without banners.
- Plus: 150 pantry items, 100 grocery items, photo import/camera review, label text parsing, sharing, full meal ideas, and no ads.
- Pro: 500 pantry items, 300 grocery items, photo review, label text parsing, sharing, full meal ideas, meal extras, and no ads.

Demo purchase uses local device storage only. It does not charge money and does not contact Google Play. Intended Play product IDs are:

- `pantrypilot_remove_ads_one_time`
- `pantrypilot_plus_one_time`
- `pantrypilot_pro_one_time`

Replace demo purchase with Google Play Billing before accepting real payments or publishing a paid in-app purchase build.

## Ads And Play Monetization

Free users see a small consent-gated adaptive AdMob banner. Plus, Pro, and the standalone Remove Ads entitlement hide that surface. Debug builds use Google's sample adaptive-banner unit; release builds use the PantryPilot production banner unit. Google Mobile Ads and UMP are integrated, and ads do not initialize before UMP permits ad requests.

Publish `app-ads.txt` at `https://sharpbelt.github.io/app-ads.txt` and use `https://sharpbelt.github.io` as the developer website in the Play listing.

Use `GOOGLE_PLAY_MONETIZATION_SETUP.md` for the Play Console, AdMob, App access, and IAP setup path.

## Future Paid Features

These are roadmap items, not active features in the current APK:

- Opt-in store reminders: after the user enables location and notification permissions, remind them about their grocery list when they arrive near a chosen grocery store.
- On-device OCR, barcode scan, receipt scan, PDF/CSV export, pantry label templates, and richer local meal ideas.

Do not add location permissions, background location, notifications, real billing charges, or billing claims until the feature is fully implemented, tested, disclosed in the privacy policy, and approved for Play policy requirements.

## Business Model Direction

Keep PantryPilot local-first and low-maintenance. Prefer a paid app listing or one-time unlock over subscriptions, cloud sync, server-side AI, accounts, or any feature that requires active backend operations.

## OCR Provider Status

Live camera OCR is practical, but the phone camera app's Lens feature is not a general Android API that this app can call for structured results. Android production OCR should use Google ML Kit or a comparable provider.

This build includes the item scanner UI, import photo / take photo flow, pasted label text parsing, item field review, add pantry item flow, and packaging text parser. Android does not bundle an automatic OCR model yet. Do not submit Play Store copy that claims automatic Android image recognition until OCR is integrated and tested from real package photos.

## Build

Android:

```bat
C:\tmp\gradle-8.7\bin\gradle.bat assembleDebug
copy app\build\outputs\apk\debug\app-debug.apk C:\tmp\PantryPilot-debug.apk
```

Preflight verification:

```powershell
powershell -ExecutionPolicy Bypass -File C:\tmp\PantryPilot\tools\android_preflight.ps1
```

S20 smoke test:

```powershell
powershell -ExecutionPolicy Bypass -File C:\tmp\PantryPilot\tools\android_s20_smoke.ps1
```

Release candidate:

```bat
C:\tmp\gradle-8.7\bin\gradle.bat assembleRelease bundleRelease
copy app\build\outputs\apk\release\app-release.apk C:\tmp\PantryPilot-release.apk
copy app\build\outputs\bundle\release\app-release.aab C:\tmp\PantryPilot-release.aab
```

Release signing is configured when `keystore.properties` exists and points at the local upload keystore. Do not share `keystore.properties`, the `.jks`, or the password source.

To create a local upload keystore and matching `keystore.properties` from an interactive prompt:

```powershell
powershell -ExecutionPolicy Bypass -File C:\tmp\PantryPilot\tools\create_upload_keystore.ps1
```

To create it from a local password file without printing the password:

```powershell
powershell -ExecutionPolicy Bypass -File C:\tmp\PantryPilot\tools\create_upload_keystore.ps1 -PasswordFile D:\Downloads\1.txt
```

Back up the generated `.jks` file and passwords before uploading to Play Console.

## Samsung Install

Use serial-scoped commands only:

```bat
adb -s R5CN30X6DDT install -r C:\tmp\PantryPilot-debug.apk
adb -s R5CN30X6DDT shell am start -n app.pantrypilot.app/.MainActivity
```

## Platform Scope

PantryPilot is now Android-only. iOS support was removed from the production candidate because there is no macOS/Xcode device pipeline available to build, sign, test, or maintain it.

## Brand Note

PantryPilot remains the product name. "Your smart pantry pal" is the consumer-facing pitch line, which keeps the warmer assistant feel without depending on a crowded PantryPal name or claiming AI features before an AI/OCR provider exists.
