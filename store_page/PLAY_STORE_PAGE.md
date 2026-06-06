# PantryPilot Play Store Page

This folder contains paste-ready listing copy and upload-ready image assets for Google Play Console.

## Upload Assets

App icon:

- `graphics/app_icon_512.png`

Feature graphic:

- `graphics/feature_graphic_1024x500.png`

Phone screenshots:

- `phone_screenshots/01_home.png`
- `phone_screenshots/02_scan.png`
- `phone_screenshots/03_scan_parser.png`
- `phone_screenshots/04_pantry.png`
- `phone_screenshots/05_grocery.png`
- `phone_screenshots/06_meals.png`
- `phone_screenshots/07_plans.png`
- `phone_screenshots/08_plans_products.png`

Signed Play upload artifact:

- `C:\tmp\PantryPilot-release.aab`

## Store Listing Fields

App name:

PantryPilot

Short description:

Offline pantry, grocery, expiry, and meal planning with photo-assisted item entry.

Full description:

PantryPilot helps you track what food you already have, what expires soon, what you need to buy, and what you can cook next.

Built for everyday kitchens, PantryPilot keeps pantry, fridge, freezer, and grocery planning in one local-first Android app. Add food manually, review package photos, paste label text copied from camera OCR or receipts, track expiry dates, and build grocery lists from what you are missing.

Core features:

- Pantry, fridge, freezer, counter, and other storage zones
- Expiry dates and expiring-soon summaries
- Categorized pantry cards with simple food icons
- Grocery lists grouped by aisle-style category
- Add pantry items to the grocery list
- Photo-assisted item entry
- Local label text parser for package text, receipts, or notes
- Meal ideas from your pantry categories
- Share pantry inventory or grocery lists as local text
- Offline-first local storage
- No accounts or analytics
- Production Free version may include a small banner ad, with a one-time Remove Ads option

Plans in this build:

- Free: starter pantry and grocery tracking with a small sponsor space
- Remove Ads: one-time ad-free unlock for Free users
- Plus: larger local limits, photo review, label text parsing, sharing, full meal ideas, and no ads
- Pro: highest local limits, meal extras, and no ads

Important: the current purchase screen is a demo entitlement flow for testing plan packaging. It does not charge money and does not contact Google Play. Replace the demo flow with Google Play Billing before accepting real in-app payments. Free builds use consent-gated Google AdMob banner ads. Debug builds use Google's sample banner unit; release builds use the PantryPilot production banner unit.

PantryPilot is designed for low-maintenance, local-first food organization. It is not a diet tracker, calorie tracker, cloud sync service, or account-based shopping platform.

## Paid App Variant

Use this if you publish PantryPilot as a paid upfront app first, without real in-app purchases:

Short description:

Offline pantry, grocery, expiry, and meal planning for everyday kitchens.

Full description note:

Remove the "Plans in this build" section from the public description, or keep it only if the demo flow is removed before release. A paid upfront app is the lowest-maintenance monetization path for the current local-first build.

## Future IAP Variant

Use this only after Google Play Billing is integrated and tested:

In-app products:

- `pantrypilot_remove_ads_one_time`
- `pantrypilot_plus_one_time`
- `pantrypilot_pro_one_time`

Short description:

Offline pantry tracking with optional Plus and Pro tools for busy kitchens.

Public IAP wording:

Optional one-time upgrades remove ads, unlock higher local limits, add photo review tools, label text parsing, sharing, and meal-planning extras.

## Screenshot Captions

1. `01_home.png`
   - Caption: See pantry count, expiry warnings, grocery items, and storage zones from the dashboard.
   - Alt text: PantryPilot dashboard showing pantry totals, expiry count, grocery list count, and plan status.

2. `02_scan.png`
   - Caption: Review package photos before adding pantry items.
   - Alt text: Scanner screen with import photo, take photo, and sample label controls.

3. `03_scan_parser.png`
   - Caption: Paste label text from camera OCR, receipts, or notes to draft item fields.
   - Alt text: Label text parser section for pasted package or receipt text.

4. `04_pantry.png`
   - Caption: Keep food organized by fridge, freezer, pantry, counter, and other zones.
   - Alt text: Categorized pantry inventory with food cards and expiry information.

5. `05_grocery.png`
   - Caption: Build grocery lists by category and clear checked items.
   - Alt text: Grocery list screen with grouped shopping items and checklist controls.

6. `06_meals.png`
   - Caption: Get local meal ideas from what is already in your kitchen.
   - Alt text: Meal ideas generated from pantry items with local offline planning.

7. `07_plans.png`
   - Caption: Free, Plus, and Pro plan gates are visible for testing.
   - Alt text: Plans screen explaining demo purchase behavior and local entitlements.

8. `08_plans_products.png`
   - Caption: Draft product IDs are ready for Play Billing integration.
   - Alt text: Remove Ads, Plus, and Pro plan cards showing draft Google Play product IDs.

## Keywords

pantry inventory, grocery list, food expiry tracker, fridge inventory, freezer inventory, meal planner, reduce food waste, kitchen organizer, grocery planner

## Category

Primary category: House & Home

Alternative category: Productivity

## Content Notes

- Do not claim automatic package-image OCR in public copy yet.
- Safe wording: "photo-assisted", "label text parser", "paste text copied from camera OCR".
- Do not claim real in-app purchases until Google Play Billing replaces the demo purchase flow.
- Internet and network-state access are used by Google AdMob. No account, precise location, contacts, storage, or notification permissions are present.
- AdMob Data Safety and privacy disclosures are documented in `..\DATA_SAFETY.md` and `..\PRIVACY_POLICY.md`.

## Play Console Checklist

- Upload `C:\tmp\PantryPilot-release.aab`.
- Upload `graphics/app_icon_512.png`.
- Upload `graphics/feature_graphic_1024x500.png`.
- Upload at least 4 phone screenshots from `phone_screenshots`.
- Paste the short and full description above.
- Add a public HTTPS privacy policy URL before production release.
- Complete Data Safety using `..\DATA_SAFETY.md`.
- Follow monetization setup in `..\GOOGLE_PLAY_MONETIZATION_SETUP.md` before enabling real ads or real purchases.
- Back up `C:\tmp\PantryPilot\pantrypilot-upload.jks` and the password source before uploading.
