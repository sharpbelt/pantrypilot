# PantryPilot Release Checklist

## Automated

- Run `tools\android_preflight.ps1`.
- Run `tools\android_s20_feature_suite.ps1` with the S20 connected.
- Build and sign `C:\tmp\PantryPilot-release.aab`.
- Verify package `app.pantrypilot.app`, version `1.0.3` / `103`, target SDK 35.
- Verify release enables Billing and debug disables real Billing.
- Verify Free ads load after UMP and paid/ad-free plans hide them.
- Verify no location, contacts, storage, or notification permissions.

## Account-Side

- Back up the upload keystore and password source.
- Create and activate the three one-time products.
- Add license testers.
- Upload AAB to Internal testing.
- Test real Play purchases and restore behavior.
- Complete listing, Data Safety, Ads, App access, Content rating, Target
  audience, Financial features, and Health declarations.
- Upload screenshots 01 through 06; do not upload debug Plans screenshots.
- Run the required closed test and apply for production access if applicable.

## Production Gates

- Public privacy policy and root `app-ads.txt` return HTTP 200.
- Real Billing tests pass for success, decline, pending, restore, refund, and
  revoke.
- Connected release lint passes outside the restricted offline mirror.
- Store copy and screenshots match the release build.

See `store_page\PLAY_CONSOLE_FINAL_SUBMISSION.md` for exact console values.
