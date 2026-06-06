# PantryPilot

Android pantry inventory, expiry tracking, grocery lists, photo-assisted item
entry, local label parsing, and local meal ideas.

## Current Release Candidate

- Package: `app.pantrypilot.app`
- Version: `1.0.3` / version code `103`
- Target SDK: 35
- Signed bundle: `C:\tmp\PantryPilot-release.aab`

## Monetization

- Free plan with consent-gated adaptive AdMob banner.
- One-time Google Play products for Remove Ads, Plus, and Pro.
- Release builds use Google Play Billing 9.0.0.
- Debug builds use local non-charging controls and Google's test ad.
- No subscriptions or backend services.

## Privacy

Pantry, grocery, image-review, label-text, and meal-idea content stays in local
app storage. AdMob processes advertising data as disclosed in
`PRIVACY_POLICY.md` and `DATA_SAFETY.md`. Google Play handles checkout and
payment details.

Public URLs:

- `https://sharpbelt.github.io/pantrypilot/PRIVACY_POLICY.md`
- `https://sharpbelt.github.io/app-ads.txt`

## Build And Test

```powershell
powershell -ExecutionPolicy Bypass -File C:\tmp\PantryPilot\tools\android_preflight.ps1
powershell -ExecutionPolicy Bypass -File C:\tmp\PantryPilot\tools\android_s20_feature_suite.ps1
```

The restricted local dependency mirror cannot execute Android release lint's
Kotlin compiler tooling. Run connected Android Studio/Gradle lint before
production promotion.

## Store Submission

Use `store_page\PLAY_CONSOLE_FINAL_SUBMISSION.md`, `STORE_LAUNCH_READINESS.md`,
`DATA_SAFETY.md`, and `PLAY_POLICY_COMPLIANCE_AUDIT.md`.

Do not claim automatic image OCR, cloud sync, accounts, location reminders, or
other unavailable features.
