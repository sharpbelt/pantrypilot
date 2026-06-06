# PantryPilot Play Policy Compliance Audit

Date: 2026-06-06

This is an engineering review, not legal advice or a guarantee of approval.

## Current Technical Position

- Package: `app.pantrypilot.app`
- Version: `1.0.3` / `103`
- Target SDK: 35
- Google Mobile Ads SDK with UMP consent handling
- Google Play Billing 9.0.0 for optional one-time digital upgrades
- Merged Google SDK permissions for advertising ID/attribution, wake lock,
  foreground service, and an internal non-exported receiver are documented in
  `DATA_SAFETY.md`
- No accounts, analytics SDK, location, contacts, notifications, or broad
  storage access
- Local pantry, grocery, image-review, parser, and meal-idea data
- No unsupported automatic OCR or AI claim

## Monetization Compliance

Remove Ads, Plus, and Pro are digital app features and use Google Play Billing.
Release builds do not expose the debug-only local purchase controls. Purchases
are granted only in `PURCHASED` state, pending purchases do not unlock features,
and completed purchases are acknowledged.

The client-only implementation has no backend verification. Google supports
client-only acknowledgement, but recommends secure backend verification. This
is a fraud-resistance tradeoff, not a Play Billing policy bypass.

## Advertising Compliance

- Free users receive a small adaptive banner.
- Remove Ads, Plus, and Pro users do not receive banner requests.
- UMP checks applicable privacy choices before Mobile Ads initializes.
- US-state and European-regulations messages are active.
- `https://sharpbelt.github.io/app-ads.txt` contains the publisher record.
- The privacy policy and Data Safety draft disclose AdMob handling.
- The in-app Privacy dialog links directly to the hosted full policy and
  developer support email.

## Required Play Console Declarations

- Contains ads: **Yes**
- Data collection/sharing: **Yes**, complete from `DATA_SAFETY.md`
- App access: no login or restricted access
- Financial features: the app does not provide financial services
- Health features: none
- Target audience: adults; do not target children
- In-app products: declare and activate all three one-time products

## Remaining Review Risks

- Real purchase flows must be tested through Play before production.
- Store screenshots must not show debug/demo purchase controls.
- Hosted privacy policy must remain synchronized with app scope.
- Store copy must not claim automatic image OCR, cloud sync, accounts, location
  reminders, subscriptions, or other unavailable features.
- Complete the required closed test before applying for production access when
  applicable to the developer account.

## Official References

- Play Billing integration:
  https://developer.android.com/google/play/billing/integrate
- Play Billing testing:
  https://developer.android.com/google/play/billing/test
- Data Safety:
  https://support.google.com/googleplay/android-developer/answer/10787469
- Payments policy:
  https://support.google.com/googleplay/android-developer/answer/9858738
- Personal-account testing requirement:
  https://support.google.com/googleplay/android-developer/answer/14151465
