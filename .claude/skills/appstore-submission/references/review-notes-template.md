# App Review Notes — template

The "Notes" field in App Review Information is your chance to make the reviewer's job easy. A good demo walkthrough cuts review time and dramatically reduces "could not test" rejections.

## Template

```
[App Name] is a [one-sentence positioning].

[Sign-in posture: "No login is required." OR "Use the demo credentials above."]

Demo flow:
1. [What the reviewer should see at launch — initial screen, pre-loaded content]
2. [Primary action — tap a marker / search / browse / etc.]
3. [Secondary action — open detail / favourite / share]
4. [Any quirks — permissions requested only on tap, opt-in features, hidden gestures]
5. [Settings or About — where the reviewer can verify what the app collects]

[Where the data comes from — community submissions, public APIs, your own CMS]

[Confirmations — no in-app purchases / no ads / no third-party tracking SDKs]

Privacy policy: https://[your-domain]/privacy
Support: https://[your-domain]/support
```

## Filled example (Berlin Chinese Food Map)

```
Berlin Chinese Food Map is a non-profit, community-driven directory of Chinese restaurants in Berlin.

No login is required. The app uses anonymous Firebase authentication so users can browse the entire map immediately after install.

Demo flow:
1. Open the app. The map opens centered on Berlin and shows red POI markers for community-curated Chinese restaurants.
2. Tap any marker to see a modal detail sheet with photos, opening hours, address, and one-tap Call / Directions actions.
3. Tap the search bar at the top to search restaurants by name (Chinese, English, or German).
4. Tap the filter button to filter by Cuisine, Style (format), and Neighbourhood tags.
5. Tap the "my location" button (top-right of map) to see your position. The app requests "when in use" location permission - you can deny it and the rest of the app works.
6. Settings (gear icon) lets you switch between English, Chinese, and system language, and between light/dark theme.

All restaurant data is community-contributed via our public GitHub repository (https://github.com/Novawerk/berlin-chinese-food-map) and stored in Firebase Firestore.

No in-app purchases. No advertising. No third-party tracking SDKs.

Privacy policy: https://berlinfoodmap.novawerk.io/privacy
Support: https://berlinfoodmap.novawerk.io/support
```

## What to leave OUT

- Marketing copy ("our beautiful, intuitive app").
- Implementation details ("we use Compose Multiplatform"). Reviewers don't care.
- Lists of features that aren't testable from the demo flow.
- Apologies or excuses ("we know X is missing, please don't reject"). Reviewers reject anyway; they don't read prologues.

## When to add an attachment

- Apps with non-obvious gestures (e.g. a hidden long-press for power-user features) — a short screen recording or annotated screenshot helps.
- Apps that require external setup (e.g. you need a paired Bluetooth device to fully test) — explain how to fake it, or send a video showing the paired flow.
- Apps with login flows that have edge cases — a flowchart of which credentials lead where.

For most directory / utility / informational apps, no attachment is needed.
