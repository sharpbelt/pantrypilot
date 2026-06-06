# Google Play Monetization Setup

PantryPilot `1.0.3` uses Google Mobile Ads SDK 24.9.0, UMP 4.0.0, and Google
Play Billing Library 9.0.0. It sells non-consumable one-time upgrades only.

## Product IDs

| Product | ID | Suggested price |
| --- | --- | --- |
| Remove Ads | `pantrypilot_remove_ads_one_time` | GBP/USD 0.99 |
| Plus | `pantrypilot_plus_one_time` | GBP/USD 2.99 |
| Pro | `pantrypilot_pro_one_time` | GBP/USD 4.99 |

Create, price, activate, and make each product's default buy option
legacy-compatible in Play Console.

## Release Billing Behavior

- Queries active products and localized prices from Google Play.
- Launches Google Play's purchase screen.
- Grants features only for completed `PURCHASED` purchases.
- Does not grant pending purchases.
- Restores owned purchases on connection, resume, and user request.
- Acknowledges completed non-consumable purchases.
- Removes revoked/refunded entitlements when Google Play reports the current
  owned-purchase set.

Release builds enable Billing. Debug builds retain local, non-charging demo
controls so automated feature tests never make purchases.

## Security Tradeoff

This client-only integration matches the low-maintenance launch objective and
uses Google Play's supported client acknowledgement path. It does not perform
server-side token verification and is therefore less resistant to tampering.
Add a verification backend only if fraud becomes material.

## Billing Test Gate

Before production:

1. Add a Google account under Play Console **License testing**.
2. Add it to the Internal testing track.
3. Upload version code `103`.
4. Install from the Play test link.
5. Test success, decline, pending approval, pending decline, restore,
   acknowledgement, refund, and revoke behavior.
6. Confirm prices and product names load from Play.

## AdMob Status

- Release uses the production banner unit.
- Debug uses Google's test adaptive-banner unit.
- Ad requests wait for UMP privacy status.
- US-state and European-regulations messages are active.
- Root publisher file: `https://sharpbelt.github.io/app-ads.txt`
- Privacy policy:
  `https://sharpbelt.github.io/pantrypilot/PRIVACY_POLICY.md`

Never click live release ads during testing.

## Play Console Review Note

```text
No account or login is required. Open PantryPilot and use the tabs at the top
of the app. Optional Remove Ads, Plus, and Pro upgrades use Google Play one-time
products. Free users may see a small consent-gated banner ad. Reviewers can use
the core pantry, expiry, grocery, scanner sample, and meal-idea features without
special access.
```

## Official References

- Billing integration: https://developer.android.com/google/play/billing/integrate
- Billing testing: https://developer.android.com/google/play/billing/test
- AdMob quick start: https://developers.google.com/admob/android/quick-start
