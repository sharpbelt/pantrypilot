# PantryPilot Closed Testing Guide

Use this guide if Play Console requires a closed test before production access.
Google currently requires eligible new personal developer accounts to keep at
least 12 testers opted in continuously for 14 days.

## Recommended Target

- Recruit 15 to 20 genuine testers before starting.
- Keep at least 12 opted in continuously for the entire 14-day period.
- Ask testers to install, use, and provide feedback on PantryPilot.
- Do not rely on Internal testing testers; use the Closed testing track.
- Do not ask testers to leave public reviews or offer rewards for positive
  reviews.

## Play Console Setup

1. Complete the store listing, declarations, and one-time products.
2. Open **Testing > Closed testing** and create or manage a track.
3. Upload the current production-candidate app bundle.
4. Open the track's **Testers** tab.
5. Create an email list and add each tester's Google Account email address, or
   add a Google Group containing the testers.
6. Set the feedback address to `tarkovchains@gmail.com`.
7. Save the tester configuration and publish the closed-test release.
8. Copy the tester opt-in link.
9. Send the opt-in link and the message below to testers.

## Recruitment Message

```text
I am preparing PantryPilot, a local pantry inventory and grocery-list Android
app, for Google Play.

I need Android testers who can remain opted into its closed test for at least
14 continuous days. Testing is free. Please install it from the Google Play
test link, try its pantry, expiry-date, scan-helper, grocery-list, and meal-idea
features, then send me honest feedback.

You need an Android phone and a Google Account. Please do not leave the test
during the 14-day period.

Opt-in link: [PASTE CLOSED-TEST LINK]
Feedback email: tarkovchains@gmail.com
```

Suitable places to recruit include friends, family, coworkers, relevant
community groups, and Android developer testing communities. Avoid services
that provide fake installs, automated activity, or guaranteed positive reviews.

## Tester Instructions

Send these instructions after a person agrees to test:

1. Open the opt-in link while signed into the same Google Account that was
   added to the test.
2. Select **Become a tester**.
3. Install PantryPilot from the Google Play link shown on the opt-in page.
4. Keep the app installed and remain opted in for at least 14 continuous days.
5. During the test, try:
   - adding and editing pantry items;
   - selecting and typing expiry dates;
   - using the sample label parser;
   - adding and clearing grocery-list entries;
   - generating meal ideas;
   - reopening the app to confirm saved data remains.
6. Send feedback using the questions below.

## Feedback Questions

```text
Device model and Android version:

Were you able to install and open PantryPilot?

Which features did you try?

Did anything fail, freeze, overlap, or behave unexpectedly?

Was any wording or workflow confusing?

What is the single most important improvement you would suggest?
```

## Tracking The 14 Days

Use `CLOSED_TESTER_TRACKER.csv`. Identify testers by an internal number or
nickname rather than copying their full email address into the tracker.

- Record the date each tester confirms they selected **Become a tester**.
- Treat the test start as the date when the twelfth active tester opted in.
- Keep 15 to 20 people opted in so one departure does not drop the count below
  12.
- Check Play Console regularly and contact anyone who accidentally opted out.
- Do not apply for production until Play Console shows the requirement as
  satisfied.

## Production Access Application

When the requirement is satisfied, Play Console asks about the closed test.
Answer truthfully using the feedback you collected. Explain:

- how testers were recruited;
- which PantryPilot workflows they tested;
- what feedback they provided;
- which issues or usability improvements were addressed;
- why the app is ready for production.

## Official References

- Testing requirement:
  `https://support.google.com/googleplay/android-developer/answer/14151465`
- Closed-test setup:
  `https://support.google.com/googleplay/android-developer/answer/9845334`
