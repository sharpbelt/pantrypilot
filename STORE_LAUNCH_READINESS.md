# PantryPilot Store Launch Readiness

Date: 2026-06-06

## Current Verdict

PantryPilot is ready for Google Play Console internal testing as a free Android app candidate.

Public production launch is safe only under one of these choices:

1. Launch as a free local-first app without claiming live ads or real paid purchases.
2. Integrate Google Play Billing and AdMob first, then launch with Remove Ads / Plus / Pro and Free banner ads.

The current APK/AAB has demo purchase buttons and placeholder ad surfaces. They are useful for reviewer/testing flow, but they do not charge money, do not load real ads, and must not be described as active public monetization.

## Upload Artifacts

- App bundle for Play Console: `C:\tmp\PantryPilot-release.aab`
- Signed release APK for side testing: `C:\tmp\PantryPilot-release.apk`
- Debug APK for device smoke testing: `C:\tmp\PantryPilot-debug.apk`
- Store-page package: `C:\tmp\PantryPilot-store-page-package.zip`
- Upload keystore: `C:\tmp\PantryPilot\pantrypilot-upload.jks`
- Keystore properties: `C:\tmp\PantryPilot\keystore.properties`

Back up the `.jks` and password source before uploading. Losing the upload key can block future updates.

## Build Facts

- Package name: `app.pantrypilot.app`
- Version name: `1.0.0`
- Version code: `100`
- Target SDK: `35`
- Platform: Android only
- iOS support: intentionally abandoned for this candidate
- Manifest permissions: none
- Internet permission: absent
- Billing permission: absent
- Location and notification permissions: absent
- Backup disabled: yes
- Cleartext traffic disabled: yes

Google Play currently requires new apps and app updates to target Android 15 / API 35 or higher for standard Android phone/tablet apps. This build targets API 35.

Official target API reference: https://developer.android.com/distribute/best-practices/develop/target-sdk

## Verified Evidence

See `PROD_TEST_REPORT.md` for the full record.

- `tools\android_preflight.ps1` passes.
- `PantryRulesSelfTest` passes 49 checks.
- Debug APK builds.
- Signed release APK builds.
- Signed release AAB builds.
- APK signature verifies with Android SDK `apksigner`.
- AAB signature verifies with JDK `jarsigner` using the local upload certificate.
- S20 first-run smoke passed on serial `R5CN30X6DDT`.
- S20 smoke verified tutorial, tabs, Free plan, Free sponsor space, Remove ads button, and no Billing surface.

## Store Listing Assets

Use files from `store_page` or the zip package.

- App icon: `store_page\graphics\app_icon_512.png`
- Feature graphic: `store_page\graphics\feature_graphic_1024x500.png`
- Phone screenshots: `store_page\phone_screenshots\*.png`
- Listing copy: `store_page\PLAY_STORE_PAGE.md`
- Captions / alt text: `store_page\SCREENSHOT_MANIFEST.csv`
- Upload checklist: `store_page\UPLOAD_CHECKLIST.md`

Current asset dimensions have already been checked:

- App icon: 512 x 512 PNG
- Feature graphic: 1024 x 500 PNG
- Phone screenshots: 1080 x 2160 PNG

Official preview asset reference: https://support.google.com/googleplay/android-developer/answer/9866151

## Play Console Fields

Recommended values:

- App name: `PantryPilot`
- Short description: use `store_page\PLAY_STORE_PAGE.md`
- Full description: use `store_page\PLAY_STORE_PAGE.md`
- Category: Productivity
- Alternative category: House & Home
- Tags: pantry, grocery list, meal planner, food expiry
- Pricing: Free for the current upload candidate
- Countries: start narrow if you want slower feedback, or use all available countries for broad reach
- Contact email: add your public support email
- Privacy policy URL: publish `PRIVACY_POLICY.md` at a public HTTPS URL and paste that URL
- App access: no login required
- Data Safety: use `DATA_SAFETY.md`
- Content rating: complete the questionnaire honestly; current app has no user-generated content, no location, no accounts, and no payments in-app
- Target audience: general household productivity; do not target children

## Agreement And Terms Notes

This is a practical checklist, not legal advice. Read and accept the current agreements yourself inside Play Console.

Relevant official terms:

- Google Play Developer Distribution Agreement: https://play.google/developer-distribution-agreement.html
- Play Console Terms of Service: https://play.google/console/terms-of-service/

Launch implications for PantryPilot:

- Developer account details, app details, contact information, permissions, prices, and policy declarations must be complete, accurate, and kept current.
- Your app and listing must comply with Google Play Developer Program Policies.
- You are responsible for user support and must provide accurate support/contact information.
- If you sell products or IAPs, you need a valid payments profile and must account for taxes/fees where applicable.
- Free apps can stay free and charge only for alternate or additional features, which fits the planned Remove Ads / Plus / Pro model.
- Users generally must be able to reinstall products obtained through Google Play without paying again, which means real Remove Ads / Plus / Pro purchases need restore logic before public monetized launch.
- You must own or have rights to all app content, screenshots, graphics, code, names, and third-party materials.
- Any statements given to Google or users must be current, true, accurate, supportable, and complete.
- Play Console usage is logged by Google and associated with the developer account; account owner/admin actions should be treated as auditable.
- Do not use Play Console outside its intended publishing/listing/metrics workflows.

PantryPilot-specific risk controls already in place:

- No unsupported automatic OCR claim in store copy.
- No real ad claim while AdMob is only a placeholder.
- No real IAP claim while Billing is demo-only.
- No permissions declared that the app does not need.
- App access note says no login is required and explains demo purchase behavior.

## App Access Note

Paste this into Play Console while using the current demo-purchase build:

```text
No account or login is required. Open PantryPilot and use the tabs at the top of the app. The Plans screen includes local demo purchase buttons for reviewer testing only; they do not charge money or contact Google Play in this build. Free users see a small sponsor banner placeholder. Remove Ads, Plus, and Pro entitlements hide ads.
```

## Data Safety Position

For the current upload candidate:

- Data collected: none
- Data shared: none
- Permissions: none
- Ads SDK: no live ads SDK in this build
- Billing SDK: no Google Play Billing SDK in this build
- Analytics SDK: none
- Account system: none

Official Data Safety reference: https://support.google.com/googleplay/android-developer/answer/10787469

## Monetization Status

Current product IDs are prepared in code and docs:

- Remove Ads: `pantrypilot_remove_ads_one_time`
- Plus: `pantrypilot_plus_one_time`
- Pro: `pantrypilot_pro_one_time`

Current build state:

- `BILLING_MODE` is `demo`.
- Purchases are local and non-charging.
- AdMob IDs are default placeholders.
- Real ads do not load.

Before public monetized launch:

1. Add Google Play Billing Library.
2. Add Google Mobile Ads SDK.
3. Add required manifest entries/permissions.
4. Replace demo purchase buttons with real Play Billing flows.
5. Restore owned purchases on app start.
6. Replace placeholder banner with a real AdMob `AdView`.
7. Update `DATA_SAFETY.md`, `PRIVACY_POLICY.md`, and public store copy.
8. Re-run `tools\android_preflight.ps1`.
9. Re-run `tools\android_s20_smoke.ps1`.

Detailed setup is in `GOOGLE_PLAY_MONETIZATION_SETUP.md`.

## Submission Order

1. Back up `pantrypilot-upload.jks` and password source.
2. Create app in Play Console.
3. Enable Play App Signing.
4. Upload `C:\tmp\PantryPilot-release.aab` to Internal testing first.
5. Fill Store Listing from `store_page\PLAY_STORE_PAGE.md`.
6. Upload graphics and screenshots from `store_page`.
7. Add Privacy Policy HTTPS URL.
8. Complete App access with the note above.
9. Complete Data Safety from `DATA_SAFETY.md`.
10. Complete Content rating.
11. Complete Target audience and content.
12. Select countries/regions.
13. Add testers and publish Internal testing.
14. Install from Play internal test link and verify the same flows as the S20 smoke.
15. Only promote to production after Play internal install works.

## Launch Blockers

These are not blockers for internal testing, but they are blockers for a polished public monetized launch:

- Real Google Play Billing is not integrated.
- Real AdMob is not integrated.
- Privacy policy still needs a public HTTPS URL and support contact.
- Store copy must avoid automatic OCR claims until OCR is integrated.
- Store copy must avoid real purchase/ad claims until those SDK integrations are live.

## Final Go / No-Go

Go for internal testing now.

Go for public free launch only if the listing is honest that purchases and live ads are not active.

No-go for public monetized launch until Billing and AdMob are actually integrated and re-tested.
