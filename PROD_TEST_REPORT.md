# PantryPilot Android Prod-Test Report

Date: 2026-06-06

## Scope

Android-only local/prod-test candidate for package `app.pantrypilot.app`.

iOS support is intentionally removed from this candidate.

## Current Artifacts

- Debug APK: `C:\tmp\PantryPilot-debug.apk`
- Signed release APK: `C:\tmp\PantryPilot-release.apk`
- Release app bundle: `C:\tmp\PantryPilot-release.aab`
- Version: `1.0.3`
- Version code: `103`
- Target SDK: `35`

## Verified Gates

- `tools\android_preflight.ps1` passes.
- `PantryRulesSelfTest` passes 53 checks.
- `tools\android_s20_feature_suite.ps1` passes on the connected S20.
- Android debug APK builds.
- Android release APK builds.
- Android release AAB builds.
- Signed release APK verifies with Android SDK `apksigner`.
- Signed release AAB verifies with JDK `jarsigner`.
- Internet and network-state permissions are present for Google AdMob.
- Google Play Billing permission is present in the merged release manifest.
- No precise location, notification, storage, or account permission is present.
- Release build is not debuggable.
- Android backup is disabled.
- Cleartext traffic is disabled.
- Free, Plus, and Pro plan constants exist.
- Remove Ads, Plus, and Pro product IDs are declared.
- First-run tutorial source gate is present.
- Free-only consent-gated Google AdMob adaptive banner is integrated.
- Debug builds use Google's sample banner unit; release builds use the PantryPilot production banner unit.
- Google User Messaging Platform privacy choices gate ad requests where required.
- Paid plans and the standalone ad-free entitlement hide the Free banner.
- Release builds enable Google Play Billing 9.0.0.
- Debug builds retain clearly marked non-charging controls for automated tests.
- Product query, purchase launch, restore, pending-state handling, and
  acknowledgement compile in release.
- Photo review, label text parsing, and sharing are Plus gated.
- Meal extras are Pro gated.
- Pantry and grocery limits are enforced.
- Compact dates such as `20261231` are accepted and saved as `2026-12-31`.
- Expandable section headers do not intercept their inner fields or action buttons.
- Clearing checked grocery items removes them and reports the number cleared.
- Android package is `app.pantrypilot.app`.

## S20 Smoke Evidence

Device serial: `R5CN30X6DDT`

Repeatable command:

```powershell
powershell -ExecutionPolicy Bypass -File C:\tmp\PantryPilot\tools\android_s20_smoke.ps1
```

Previously verified before the monetization/tutorial patch:

- `adb -s R5CN30X6DDT install -r C:\tmp\PantryPilot-debug.apk` completed with `Success`.
- `adb -s R5CN30X6DDT shell am start -n app.pantrypilot.app/.MainActivity` launched the app.
- Smoke files:
  - `C:\tmp\pantrypilot_final_smoke.png`
  - `C:\tmp\pantrypilot_final_smoke.xml`
  - `C:\tmp\pantrypilot_s20_smoke.png`
  - `C:\tmp\pantrypilot_s20_smoke.xml`
  - `C:\tmp\pantrypilot_plans.png`
  - `C:\tmp\pantrypilot_plans.xml`
  - `C:\tmp\pantrypilot_plans_scrolled.png`
  - `C:\tmp\pantrypilot_plans_scrolled.xml`
  - `C:\tmp\pantrypilot_free_gate.xml`
  - `C:\tmp\pantrypilot_scan_parser.png`
  - `C:\tmp\pantrypilot_scan_parser.xml`

Visible smoke UI:

- App title: PantryPilot
- Tabs: Home, Scan, Pantry, Grocery, Meals, Plans
- Free plan status was visible on first launch.
- No Billing surface is visible on first launch.

Current monetization/tutorial smoke status:

- `tools\android_s20_smoke.ps1` clears app data by default, verifies the first-run
  tutorial, taps through it, verifies the Free advertising surface, and refuses
  to target any non-S20 serial.
- Latest run on 2026-06-06 passed on S20 serial `R5CN30X6DDT`.
- Verified first-run tutorial appears and all Next/Start buttons are tappable.
- Verified first-launch UI contains PantryPilot, Home, Scan, Pantry, Grocery, Meals, Plans, Free plan, Free plan sponsor space, and Remove ads.
- Verified first-launch UI does not contain Billing.

## S20 Automated Feature Suite

Repeatable command:

```powershell
powershell -ExecutionPolicy Bypass -File C:\tmp\PantryPilot\tools\android_s20_feature_suite.ps1
```

Latest run on 2026-06-06 passed on S20 serial `R5CN30X6DDT`.

Covered workflows:

- First-run tutorial and Free-plan ad surface.
- Manual pantry item entry using name, quantity, and compact expiry date inputs.
- Saved pantry item persistence and expiry normalization.
- Scanner sample label name, quantity, and expiry extraction.
- Grocery add, duplicate rejection, checkbox completion, and checked-item clearing.
- Local meal-idea rendering.
- Remove Ads demo unlock, Free-banner removal, and ad-free demo reset.

## AdMob And Consent Runtime

- Google Mobile Ads SDK and UMP are integrated.
- Debug builds use Google's sample adaptive-banner unit; release builds use the PantryPilot production banner unit.
- AdMob US-state and European-regulations messages are active.
- On 2026-06-06, the S20 successfully retrieved UMP status and initialized Mobile Ads.
- The Home screen displayed Google's adaptive `Test Ad` creative and the app reported `Test ad loaded.`
- The earlier VPN/DNS failure was handled safely: no ad request was made while privacy status could not be checked.

## Debug Plan-Gating Evidence

- Free plan shows `3/12` pantry usage and `2/12` grocery usage on seeded first launch.
- Plans screen shows Free, Remove Ads, Plus, and Pro cards.
- Scrolled Plans screen shows product IDs:
  - `pantrypilot_remove_ads_one_time`
  - `pantrypilot_plus_one_time`
  - `pantrypilot_pro_one_time`
- S20 demo purchase dialog for Plus opened and clearly stated it does not charge money.
- Free photo import gate redirected to Plans with `Photo import review requires Plus`.
- Scan screen shows the sellable label text parser controls after scrolling:
  - `Parse Label Text`
  - `Paste label text`
  - `Parse label text`
- These debug-only controls do not appear as purchase controls in release builds.

## Google Play Billing Release Evidence

- Release `BuildConfig.PLAY_BILLING_ENABLED` is `true`; debug is `false`.
- Google Play Billing Library 9.0.0 is integrated.
- The merged release manifest contains `com.android.vending.BILLING`.
- Release code queries all three products and launches Google Play purchase
  flows using `ProductDetails`.
- Owned purchases are restored on connection, resume, and user request.
- Features are granted only for `PURCHASED`; `PENDING` does not unlock.
- Completed purchases are acknowledged.
- Real runtime Billing tests remain pending until the products are active in
  Play Console and version code 103 is installed through Play/license testing.
- Client-only processing is less fraud-resistant than server verification.

## Play Upload Status

Local production-candidate build readiness is complete. The final S20
regression suite passed on the current build after Billing integration,
including onboarding, pantry entry, compact date normalization, scanner sample
parsing, grocery operations, meal ideas, and Remove Ads behavior.

Play upload signing is configured with the local upload keystore:

- Keystore: `C:\tmp\PantryPilot\pantrypilot-upload.jks`
- Signing properties: `C:\tmp\PantryPilot\keystore.properties`
- Signed app bundle: `C:\tmp\PantryPilot-release.aab`
- Signed release APK: `C:\tmp\PantryPilot-release.apk`

Before uploading to Play Console, back up the `.jks` and password source.
Losing the upload key can block future app updates. To create a replacement
upload key interactively for a different app:

```powershell
powershell -ExecutionPolicy Bypass -File tools\create_upload_keystore.ps1
```

The generated signed `.aab` is the Play Console upload artifact.

Release lint also passes. The lint-driven fixes include API-26-compatible
navigation styling, explicit backup/data-transfer exclusions, monochrome
launcher icon support, and a localized dashboard summary.

The verified store upload package is:

- `C:\tmp\PantryPilot-store-page-package.zip`
- Six visually audited customer-facing screenshots
- No debug Plans screenshots or raw captures
- Screenshot hashes recorded in `store_page\SCREENSHOT_VERIFICATION.json`

## Signature Evidence

- `C:\tmp\PantryPilot-release.apk` verifies with APK Signature Scheme v2 and one signer.
- `C:\tmp\PantryPilot-release.aab` is jar-verified and signed by `CN=PantryPilot, OU=Release, O=PantryPilot, L=Local, ST=Local, C=GB`.
- The upload certificate is self-signed, which is expected for an Android upload key.

## Store Copy Caveat

Do not claim automatic package-image OCR until an Android OCR provider is integrated and tested. Current scanner support includes image import/camera capture, pasted label text parsing, a sample parser, and review/add flows, but not bundled production OCR.
