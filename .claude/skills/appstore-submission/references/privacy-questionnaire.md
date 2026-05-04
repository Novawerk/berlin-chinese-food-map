# App Privacy questionnaire

Apple's privacy questionnaire is the part of submission that surprises people the most. The vocabulary is theirs, not yours — "Linked to User", "Used for Tracking", "App Functionality" all have specific meanings, and getting them wrong means re-publishing the questionnaire (which delays review).

## The Apple definitions you have to internalise

- **Collect** — data is "collected" if it's transmitted off the device in a way that lets you (or a third party) access it for *longer than is strictly necessary to service the request in real time*. Anything written to your backend is collected. Anything that exists only in RAM during one network round-trip is not.
- **Linked to User** — data linked to a real-world user identity (name, email, account, device ID, etc.). An anonymous random ID issued by Firebase Auth that's never combined with PII is **not** linked, even if it persists.
- **Used for Tracking** — sharing the user's data across apps/websites or with data brokers for advertising, marketing, or measurement. **Standard analytics about how *your* app is used is NOT tracking** in Apple's vocabulary. Tracking specifically means cross-context.

## The flow

1. Open `/distribution/privacy`.
2. Click **Edit** next to Privacy Policy URL → paste a publicly reachable URL (e.g. `https://yourdomain.com/privacy`). Save.
3. Click **Get Started** under the data-collection block.
4. **First question**: "Do you or your third-party partners collect data from this app?" Yes/No.
5. If Yes: the data-type checkbox tree appears (Contact Info, Health, Financial, Location, Identifiers, Usage Data, etc.). Tick every category whose data you write off-device.
6. For *each* ticked data type, a per-type wizard runs:
   - **Purposes** — multi-select (Third-Party Advertising / Developer's Advertising / Analytics / Product Personalization / App Functionality / Other Purposes).
   - **Linked to user identity?** — Yes / No.
   - **Used for tracking?** — Yes / No.
7. Click **Save** at each step. After all data types are configured, click **Publish** at the top right.

Until you Publish, the App Privacy section on the version page stays yellow and you can't submit.

## Common app-behaviour → Apple data-type mappings

### Firebase Anonymous Auth + Firestore writes

The auth UID counts as **Identifiers → User ID** (it's an "assigned user ID or other user-level ID").

- Linked to user identity? **No**, if the UID is never combined with PII (no email, no name, no phone).
- Used for tracking? **No**, if you're not sharing it with ad networks or data brokers.
- Purposes? **App Functionality** (the UID is used for security rules / per-user state) and possibly **Analytics** (if you use it to count distinct visits).

### Per-user view records / favourites / history written to Firestore

If the user opens a restaurant and you write `restaurants/{id}/views/{uid}` with timestamps and counts, that's **Usage Data → Product Interaction** (which restaurant the user opened).

- Linked? **No** (it's keyed by an anonymous UID — see above).
- Tracking? **No**.
- Purposes? **App Functionality** + **Analytics**.

### Location

Tricky. Apple's "Collect" definition is the deciding factor:

- App requests location, draws a "you are here" dot, **does not** transmit coordinates → **NOT collected**. Don't tick Location.
- App requests location, sends coordinates to your backend (e.g. for proximity search, geofencing, or analytics) → **collected**. Tick Precise Location, fill in the wizard.

Even though location feels like a "data collection" thing intuitively, the user-facing privacy policy must align: if you tick Location, your privacy policy must explain why.

### Crash reports / Firebase Crashlytics / Sentry / Bugsnag

These collect **Diagnostics → Crash Data** and often **Performance Data**.

- Linked? Usually No (anonymised by the SDK).
- Tracking? No.
- Purposes? **Analytics** (App Functionality is also defensible).

### Email signup / contact form / account creation

If you ever take a user's email (account creation, newsletter, support form), that's **Contact Info → Email Address**, almost always Linked to User, App Functionality.

### Photo upload (user-submitted)

If users upload photos to your backend, that's **User Content → Photos or Videos**, Linked, App Functionality.

## Aligning the privacy policy

The privacy policy at your URL must accurately describe everything you ticked. Mismatches are the #1 reason apps get sent back from review for privacy reasons.

A minimum viable mapping:

| App Privacy answer | Privacy policy must mention |
|---|---|
| User ID — collected, not linked, App Functionality | "We assign an anonymous random user ID via [Firebase Auth] when the app starts. It is not linked to your name, email, or device." |
| Product Interaction — collected, not linked, Analytics + App Functionality | "When you open content X, we record the action under your anonymous user ID for usage statistics." |
| Crash Data — collected, not linked, Analytics | "We collect crash logs via [SDK] to fix bugs." |

Don't write generic "we may collect any data" boilerplate — Apple compares specifics.

## "Optional Disclosure" categories

Apple lets you skip declaring some data types that meet *all* of these criteria:

- Not used for tracking.
- Not used for advertising / marketing / personalization purposes.
- Collected only infrequently, not part of the app's primary functionality.
- The user is presented with the data being collected at the time of collection AND can choose not to provide it.

This is rare. Most "infrequent" data collection (e.g. an opt-in feedback form) actually does need to be declared. When in doubt, declare.
