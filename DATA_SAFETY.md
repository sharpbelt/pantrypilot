# Google Play Data Safety Draft - AdMob Build

Applies to PantryPilot `1.0.2` / version code `102`.

The final Play Console answers remain the developer's responsibility. Recheck
Google Mobile Ads SDK disclosures whenever its dependency version changes.

## Initial Question

Does the app collect or share any required user data types?

**Yes.**

Pantry contents, grocery lists, images, and label text remain local, but Google
Mobile Ads SDK 24.9.0 automatically collects and shares advertising data.

## Data Types To Declare

### Location

- Approximate location
  - Collected: Yes
  - Shared: Yes
  - Source: IP address used to estimate general location
  - Purposes: Advertising or marketing; Analytics; Fraud prevention, security,
    and compliance

### App Activity

- App interactions
  - Collected: Yes
  - Shared: Yes
  - Examples: App launches, taps, ad interactions, and video views
  - Purposes: Advertising or marketing; Analytics; Fraud prevention, security,
    and compliance

### App Info And Performance

- Diagnostics
  - Collected: Yes
  - Shared: Yes
  - Examples: App launch time, hang rate, SDK performance, and energy use
  - Purposes: Analytics; Fraud prevention, security, and compliance

### Device Or Other IDs

- Device or other IDs
  - Collected: Yes
  - Shared: Yes
  - Examples: Android advertising ID, app set ID, and applicable device/account
    identifiers
  - Purposes: Advertising or marketing; Analytics; Fraud prevention, security,
    and compliance

## Handling Answers

- Data is encrypted in transit: Yes
- Users can request deletion: Not applicable for PantryPilot server data because
  PantryPilot has no accounts or server-side user records. Describe Google's
  identifier and privacy controls through the privacy policy and consent UI.
- Data collection required or optional: Answer according to the choices shown by
  Google Play for each data type and the UMP consent configuration. Do not claim
  all advertising data is optional without checking the active AdMob message and
  serving mode.

## Data Not Sent To PantryPilot Servers

- Pantry and grocery item data
- Expiry dates and storage locations
- Package images
- Pasted label text
- Local meal ideas
- Local demo purchase and ad-free entitlement state

## Permissions And SDKs

- Internet and network-state access are used for AdMob.
- Google Mobile Ads SDK: `24.9.0`
- Google User Messaging Platform SDK: `4.0.0`
- No account, precise location, contacts, storage, notification, or Play Billing
  permission is currently used.

Official SDK disclosure:
https://developers.google.com/admob/android/privacy/play-data-disclosure
