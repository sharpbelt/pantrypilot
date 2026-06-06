# Play Console Upload Checklist

Use these files:

- App bundle: `C:\tmp\PantryPilot-release.aab`
- App icon: `graphics/app_icon_512.png`
- Feature graphic: `graphics/feature_graphic_1024x500.png`
- Phone screenshots: `phone_screenshots/01_home.png` through
  `phone_screenshots/06_meals.png`
- Store copy: `PLAY_STORE_PAGE.md`
- Screenshot captions and alt text: `SCREENSHOT_MANIFEST.csv`
- Monetization setup: `..\GOOGLE_PLAY_MONETIZATION_SETUP.md`
- Launch readiness: `..\STORE_LAUNCH_READINESS.md`
- Closed-test recruitment and instructions: `CLOSED_TESTING_GUIDE.md`
- Closed-test participation tracker: `CLOSED_TESTER_TRACKER.csv`

Verified local asset dimensions:

- App icon: 512 x 512, RGBA PNG
- Feature graphic: 1024 x 500, RGB PNG
- Phone screenshots: 1080 x 2160, RGB PNG

Google Play requirements checked on 2026-06-06:

- App icon: 512 x 512 PNG, max 1024 KB.
- Feature graphic: 1024 x 500 JPEG or 24-bit PNG.
- Screenshots: minimum 2 screenshots, PNG or JPEG, minimum dimension 320 px, maximum dimension 3840 px, and the long side must not be more than twice the short side.
- Recommended for app promotion: at least 4 phone screenshots with minimum 1080 px resolution in 9:16 portrait or 16:9 landscape.
- Standard Android phone/tablet apps must target Android 15 / API 35 or higher for current new app/update submission; this build targets SDK 35.

Safe release wording:

- Use "photo-assisted item entry" and "label text parser."
- Do not claim automatic package-image OCR yet.
- Google Play Billing and AdMob are integrated; do not promote until real
  Play-distributed purchase tests pass.
- Upload screenshots 01 through 06 only. A Plans screenshot is intentionally
  omitted until it can be captured from Play internal testing with active
  products and real localized prices.
- Run `tools\capture_store_screenshots.ps1` before packaging. It captures the
  legitimate Remove Ads experience and writes `SCREENSHOT_VERIFICATION.json`.
- Run `tools\create_store_page_package.ps1` to create the upload package. It
  refuses stale or modified screenshots and excludes debug Plans screens.

Remaining manual Play Console fields:

- Developer name
- Support email
- Privacy policy HTTPS URL
- App category and tags
- Data Safety answers from `..\DATA_SAFETY.md`
- App access / review note from `..\GOOGLE_PLAY_MONETIZATION_SETUP.md`
- Launch go/no-go from `..\STORE_LAUNCH_READINESS.md`
- In-app products: `pantrypilot_remove_ads_one_time`, `pantrypilot_plus_one_time`, `pantrypilot_pro_one_time`
- Real purchase test results for success, decline, pending, restore, refund, and revoke
- Pricing country availability
- Content rating questionnaire
- Target audience questionnaire
- Closed-test opt-in link and at least 15 recruited testers, providing a buffer
  above the required 12 continuously opted-in testers
