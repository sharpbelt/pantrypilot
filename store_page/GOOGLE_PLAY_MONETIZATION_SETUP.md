# Google Play Monetization Setup

This app is prepared for monetization, but the checked-in build is intentionally inert:

- Billing mode is `demo`, so purchase buttons only change local test entitlements.
- AdMob IDs are default placeholders, so real ads do not initialize.
- The manifest has no internet or billing permission until the real SDK integrations are added.

## Product IDs

Create these one-time products in Play Console. Keep them non-consumable.

| Product | Play product ID | Suggested price | Effect |
| --- | --- | --- | --- |
| Remove Ads | `pantrypilot_remove_ads_one_time` | `0.99 USD` or local equivalent | Keeps Free limits but hides the banner ad. |
| Plus | `pantrypilot_plus_one_time` | low one-time unlock | Removes ads, raises limits, unlocks scanner review, label parsing, sharing, and full meal ideas. |
| Pro | `pantrypilot_pro_one_time` | higher one-time unlock | Removes ads, raises limits further, and unlocks meal extras. |

For the low-upkeep business model, use one-time products instead of subscriptions.

Official reference: Google describes one-time products as single-charge digital items, including non-consumable permanent benefits such as ad-free app versions: https://developer.android.com/google/play/billing/one-time-products

## Current Default Gates

The production switches are in `app/src/main/java/app/pantrypilot/app/MainActivity.java`:

```java
private static final String ADMOB_APP_ID = DEFAULT_ADMOB_APP_ID;
private static final String ADMOB_BANNER_ID = DEFAULT_ADMOB_BANNER_ID;
private static final String BILLING_MODE = BILLING_MODE_DEMO;
```

Do not change only the user-facing text. The app checks these values:

- Default AdMob IDs mean the banner surface stays a placeholder and real ads do not start.
- Real AdMob-shaped IDs make `isAdMobConfigured()` true.
- `BILLING_MODE_PRODUCTION` plus non-empty product IDs makes `isProductionBillingConfigured()` true.

## Google Play Console Setup

1. Create the app in Play Console.
2. App name: `PantryPilot`.
3. Default language: English.
4. App type: App.
5. Pricing: Free, because monetization is through ads and in-app products.
6. Category: Productivity or Food & Drink. Productivity is the safer fit for the current pantry/list workflow.
7. Upload `C:\tmp\PantryPilot-release.aab` to an internal testing track first.
8. Complete Store Listing using `STORE_LISTING.md` and `store_page\PLAY_STORE_PAGE.md`.
9. Add screenshots and graphics from `store_page`.
10. Complete Data Safety using `DATA_SAFETY.md`.
11. Publish `PRIVACY_POLICY.md` to a public HTTPS URL and paste that URL in Play Console.
12. In App access, state that no login is required. Reviewers can use the app immediately. In test builds, the Plans screen has local demo purchase buttons that do not charge money.

Official Play Console app creation guide: https://support.google.com/googleplay/android-developer/answer/9859152

## In-App Product Setup

1. In Play Console, open Monetize > Products > In-app products.
2. Create each product ID exactly as listed above.
3. Mark each product active.
4. Add clear names:
   - `Remove Ads`
   - `PantryPilot Plus`
   - `PantryPilot Pro`
5. Add short descriptions that match the in-app plan cards.
6. Use one-time non-consumable purchase behavior.
7. Add license testers before internal testing.
8. Upload the AAB to an internal testing track before expecting real product queries to work.

## Billing Integration Drop-In

When ready to accept real money:

1. Add the Google Play Billing Library dependency.
2. Add the billing permission required by the library setup.
3. Change:
   ```java
   private static final String BILLING_MODE = BILLING_MODE_PRODUCTION;
   ```
4. Replace `confirmDemoPurchase()` and `confirmDemoRemoveAds()` with BillingClient purchase flows.
5. Query product details for:
   - `pantrypilot_remove_ads_one_time`
   - `pantrypilot_plus_one_time`
   - `pantrypilot_pro_one_time`
6. Only call `setAdsRemoved(true, true)` after a valid Remove Ads purchase is purchased and acknowledged.
7. Only call `setPlan(plan, true)` after a valid Plus or Pro purchase is purchased and acknowledged.
8. On app launch, call Play Billing restore/query purchases and re-apply owned entitlements.
9. Keep demo purchase wording out of the production build.

Official Billing integration guide: https://developer.android.com/google/play/billing/integrate

## AdMob Setup

1. Create an AdMob app for PantryPilot.
2. Create one banner ad unit.
3. Replace:
   ```java
   private static final String ADMOB_APP_ID = DEFAULT_ADMOB_APP_ID;
   private static final String ADMOB_BANNER_ID = DEFAULT_ADMOB_BANNER_ID;
   ```
   with the real AdMob app ID and banner ad unit ID.
4. Add the Google Mobile Ads SDK dependency.
5. Add the AdMob application ID metadata to `AndroidManifest.xml`.
6. Add any required network/ads permissions and update Play Console Data Safety.
7. Replace the placeholder branch in `freeAdBanner()` with a real banner `AdView`.
8. Use Google test ads during development. Do not click live ads on test devices.

Official AdMob quick start: https://developers.google.com/admob/android/quick-start
Official banner guide: https://developers.google.com/admob/android/banner

## Review Notes To Paste Into Play Console

Use this for App access / review notes while the demo purchase build is under test:

```text
No account or login is required. Open PantryPilot and use the tabs at the top of the app. The Plans screen includes local demo purchase buttons for reviewer testing only; they do not charge money or contact Google Play in this build. Free users see a small sponsor banner placeholder. Remove Ads, Plus, and Pro entitlements hide ads.
```

For the real billing build, update the note:

```text
No account or login is required. Paid features are available through Google Play one-time in-app products. License testers can purchase Remove Ads, Plus, or Pro using Play test payment methods from the Plans screen.
```

## Release Blockers Before Real Monetization

- Do not publish real ads until Google Mobile Ads SDK, consent handling if needed, Data Safety, and privacy disclosures are updated.
- Do not accept real payments until Google Play Billing purchase, acknowledgement, and restore flows are implemented and tested.
- Keep the first public build conservative: Free plus banner, `0.99` Remove Ads, and one-time Plus/Pro unlocks. Avoid forced interstitials.
