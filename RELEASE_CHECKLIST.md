# Release Checklist

Visible app name: PantryPilot. Marketing line: "your smart pantry pal." Keep PantryPal as tagline language, not the public product name, because similar PantryPal apps already exist.

## Build

Android only:

```bat
C:\tmp\gradle-8.7\bin\gradle.bat clean assembleRelease bundleRelease
```

## Signing

- Generate an upload keystore:
  ```powershell
  powershell -ExecutionPolicy Bypass -File C:\tmp\PantryPilot\tools\create_upload_keystore.ps1
  ```
- Or generate it from a local password file without printing the password:
  ```powershell
  powershell -ExecutionPolicy Bypass -File C:\tmp\PantryPilot\tools\create_upload_keystore.ps1 -PasswordFile D:\Downloads\1.txt
  ```
- Back up `pantrypilot-upload.jks` and the passwords somewhere safe.
- Rerun `powershell -ExecutionPolicy Bypass -File C:\tmp\PantryPilot\tools\android_preflight.ps1`.
- Upload signed `.aab` to Play Console.
- Without `keystore.properties`, the generated APK/AAB artifacts are for local/prod testing only, not Play upload.

## Billing

This build exposes Free, Remove Ads, Plus, and Pro entitlements with a local demo purchase flow.

Current demo product IDs:

- Remove Ads one-time unlock: `pantrypilot_remove_ads_one_time`
- Plus one-time unlock: `pantrypilot_plus_one_time`
- Pro one-time unlock: `pantrypilot_pro_one_time`

Demo purchase is local-only. It does not charge money and does not contact Google Play.

Use `GOOGLE_PLAY_MONETIZATION_SETUP.md` for the Play Console setup, App access note, Billing integration checklist, and AdMob drop-in checklist.

Before adding paid features on Android:

- Prefer a paid Play Store listing for the lowest-maintenance path.
- If using in-app monetization, prefer one-time unlocks over subscriptions.
- Replace the demo purchase flow with Google Play Billing before accepting real payments.
- Create matching Play Console products for `pantrypilot_remove_ads_one_time`, `pantrypilot_plus_one_time`, and `pantrypilot_pro_one_time`, or update the in-app IDs to match the final Play Console IDs.
- Add real purchase verification and restore logic for paid products.
- Avoid server-backed purchases, cloud accounts, or subscription promises that require ongoing service upkeep.

## Ads

- Free users may see a small consent-gated adaptive banner.
- Remove Ads, Plus, and Pro users must not see banners.
- Google Mobile Ads and UMP are integrated. Debug uses Google's sample banner; release uses the PantryPilot production banner.
- Publish the AdMob European regulations message for EEA/UK/Swiss users.
- Publish `app-ads.txt` at `https://sharpbelt.github.io/app-ads.txt`.
- Ensure the hosted privacy policy and Play Data Safety answers match `PRIVACY_POLICY.md` and `DATA_SAFETY.md`.
- Verify a sample ad loads with VPN/ad-blocking DNS disabled.

Before adding opt-in store reminders:

- Add notification permission handling for Android 13 and newer.
- Add foreground/background location only if the final implementation genuinely needs it.
- Add in-app disclosure before requesting location permission.
- Update Play Console permissions declarations, privacy policy, and Data Safety answers.
- Verify reminders trigger only after the user opts in and can be disabled from inside the app.

## Store

- Use `STORE_LISTING.md` for copy.
- Publish `PRIVACY_POLICY.md` at a public HTTPS URL.
- Use `DATA_SAFETY.md` for Play Console.
- Add screenshots from a real Android device.

## Verification

- Review `PROD_TEST_REPORT.md`.
- Run `powershell -ExecutionPolicy Bypass -File C:\tmp\PantryPilot\tools\android_preflight.ps1`.
- Run `powershell -ExecutionPolicy Bypass -File C:\tmp\PantryPilot\tools\android_s20_smoke.ps1` with the S20 connected.
- Verify Free limits: 12 pantry items, 12 grocery items, sample scanner preview, and 2 meal ideas.
- Verify Free shows the sponsor banner placeholder and Remove Ads hides it.
- Verify Plus demo purchase unlocks photo import/camera review, label text parsing, sharing, 150 pantry items, 100 grocery items, and full meal ideas.
- Verify Pro demo purchase unlocks meal extras and raises limits to 500 pantry items and 300 grocery items.
- Verify demo restore re-applies the highest local demo purchase.
- Add pantry item.
- Import or capture an item image in Scan.
- On Android, verify camera capture returns a full-size local image preview from `app.pantrypilot.app.images`.
- Run the scanner sample parser and verify the drafted name, quantity, expiry, and category.
- Integrate and verify an OCR provider before claiming automatic package image recognition in store copy.
- Paste copied label text and verify it drafts name, quantity, expiry, category, and storage location.
- Draft scanner fields.
- Add pantry item from the scanner draft.
- Reject invalid expiry date.
- Show expiring-soon count.
- Add item to grocery list from pantry.
- Share pantry inventory through Android share sheet.
- Share grocery list through Android share sheet.
- Check and clear grocery items.
- Generate meal ideas.
- Open Privacy/About dialogs.
- Verify package: `app.pantrypilot.app`.
- Verify target SDK 35.
- Verify internet and network-state permissions are limited to consent and AdMob use.
