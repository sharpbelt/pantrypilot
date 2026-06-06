# PantryPilot Play Policy Compliance Audit

Date: 2026-06-06

This is an engineering compliance audit, not legal advice and not a guarantee of Google approval. Google Play review is the authority.

## Verdict

Current local build appears suitable for Google Play internal testing and a carefully worded free public test release.

Do not submit it as a public monetized release that claims live ads or real in-app purchases until Google Mobile Ads SDK and Google Play Billing are integrated, disclosed, and retested.

## Official Policy Checks Used

- Developer Distribution Agreement: https://play.google/developer-distribution-agreement.html
- Play Console Terms of Service: https://play.google/console/terms-of-service/
- Deceptive Behavior / Misleading Claims: https://support.google.com/googleplay/android-developer/answer/9888077
- Data Safety: https://support.google.com/googleplay/android-developer/answer/10787469
- Target SDK guidance: https://developer.android.com/distribute/best-practices/develop/target-sdk

## Current Build Evidence

- Package: `app.pantrypilot.app`
- App name: `PantryPilot`
- Target SDK: 35
- No declared Android permissions.
- No internet permission.
- No location permission.
- No notification permission.
- No storage permission.
- No Play Billing permission.
- No live ad SDK.
- No analytics SDK.
- No account/login requirement.
- Local-only pantry, grocery, expiry, scanner review, and meal idea data.
- Demo purchase buttons clearly state they do not charge money and do not contact Google Play.
- Store screenshots were regenerated after removing unsupported `AI` wording.

## Risks Fixed In This Pass

- Removed current-build `AI` marketing wording from the visible in-app tagline.
- Removed current-build `AI` wording from public listing draft docs.
- Updated privacy wording from `ads` to `live ad networks` so it matches the placeholder sponsor surface.
- Regenerated all eight phone screenshots from the updated app so screenshots no longer show the old `AI` tagline.

## Remaining Launch Boundaries

Compliant-looking for internal testing:

- Free app setup.
- Demo purchase flow disclosed to reviewers in App access notes.
- No live ads/IAP claims in public metadata.

Not ready for public monetized launch:

- Real Google Play Billing is not integrated.
- Real purchase restore is not integrated.
- Real AdMob banner rendering is not integrated.
- Data Safety and privacy policy need updates before real ads/billing are enabled.

## Manual Play Console Declarations

Only the account owner should tick these:

- Developer Programme Policies.
- Play app signing Terms of Service.
- US export laws.

Current technical basis for export declaration: the app has no custom encryption, no internet permission, and no network service integration in this build. This does not replace the developer's own legal/export responsibility.

## Practical Compliance Conclusion

Based on current source, manifest, docs, screenshots, and device smoke tests, I do not see an obvious Google Play policy violation for an internal-test/free candidate.

The main compliance condition is truthful presentation: do not claim automatic OCR, AI, live ads, paid purchases, subscriptions, cloud sync, account services, location reminders, or notifications until those features are actually implemented, disclosed, and tested.
