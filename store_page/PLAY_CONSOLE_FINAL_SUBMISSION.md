# Play Console Final Submission

Use this checklist for PantryPilot `1.0.3` / version code `103`.

## 1. Create One-Time Products

Open **Monetize > Products > In-app products / One-time products** and create:

| Product ID | Name | Description | Suggested base price |
| --- | --- | --- | --- |
| `pantrypilot_remove_ads_one_time` | Remove Ads | Permanently removes banner ads while keeping Free limits. | GBP 0.99 / USD 0.99 |
| `pantrypilot_plus_one_time` | PantryPilot Plus | Permanently unlocks larger limits, photo review, label parsing, sharing, full meal ideas, and removes ads. | GBP 2.99 / USD 2.99 |
| `pantrypilot_pro_one_time` | PantryPilot Pro | Permanently unlocks the highest limits, meal extras, all Plus tools, and removes ads. | GBP 4.99 / USD 4.99 |

Configure each as a non-consumable one-time buy product, make its default buy
option legacy-compatible, set countries/prices, and activate it.

## 2. Upload Internal Release

Upload:

`C:\tmp\PantryPilot-release.aab`

Suggested release name:

`1.0.3 billing and ads production candidate`

Release notes:

```text
Production-candidate release with consent-gated banner ads, Google Play one-time
upgrades, purchase restore, expiry tracking, pantry inventory, grocery lists,
photo-assisted item entry, label parsing, and local meal ideas.
```

## 3. Configure Billing Testing

1. Add your testing Google account under **Settings > License testing**.
2. Add the same account to the Internal testing tester list.
3. Install PantryPilot from the Play internal-test link.
4. Verify product names and localized prices load.
5. Test Remove Ads, Plus, and Pro with Google's test payment methods.
6. Verify successful purchases unlock immediately and remain after reinstall.
7. Verify a declined purchase grants nothing.
8. Verify a pending purchase grants nothing until it becomes purchased.
9. Verify purchases are acknowledged and are not automatically refunded.
10. Refund and revoke a test purchase in Order management, then verify restore
    removes the entitlement.

## 4. Store Listing

- App name: `PantryPilot`
- Category: `House & Home`
- Pricing: `Free`
- Contains ads: `Yes`
- Support email: `tarkovchains@gmail.com`
- Website: `https://sharpbelt.github.io`
- Privacy policy:
  `https://sharpbelt.github.io/pantrypilot/PRIVACY_POLICY.md`
- Copy: `PLAY_STORE_PAGE.md`
- Icon: `graphics/app_icon_512.png`
- Feature graphic: `graphics/feature_graphic_1024x500.png`
- Upload screenshots 01 through 06 only.

## 5. App Content Declarations

- App access: no login or special access required.
- Ads: yes.
- Data Safety: answer from `..\DATA_SAFETY.md`.
- Financial features: no financial-service features.
- Health features: none.
- Target audience: adults; do not select child age groups.
- Content rating: answer based on the app and active advertising. The app has no
  user-generated content, violence, sexual content, gambling, or age-restricted
  product promotion.

## 6. Closed Testing And Production

New personal developer accounts created after November 13, 2023 generally must
run a closed test with at least 12 opted-in testers continuously for 14 days
before applying for production access.

Start the closed test after Billing and the store listing are complete. Keep
testers opted in and collect genuine feedback. Apply for production access only
after the test requirement and all declarations are complete.

## 7. Remaining Technical Verification

- Reconnect the S20 and run:
  `tools\android_s20_feature_suite.ps1`
- Confirm the automated release lint gate still passes before uploading.
- Do not promote to production until real Play Billing tests pass.
