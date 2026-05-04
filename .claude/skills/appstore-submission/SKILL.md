---
name: appstore-submission
description: End-to-end iOS App Store submission workflow — App Store Connect form-fill (App Information, App Privacy questionnaire, Pricing, version metadata, App Review Information), iPhone screenshot capture from Simulator, build attach, and submission. Use this skill whenever the user is preparing an iOS app for App Store review, says they want to "submit", "ship", "publish", or "release" an iOS app, asks about App Store Connect, asks about the App Privacy questionnaire, asks about screenshot dimensions or capture, or hits any "Add for Review" / "Submit for Review" workflow — even if they only describe one piece of it (e.g. "fill in the privacy form", "take App Store screenshots"), check if any of the other steps in the workflow are also missing and pull this skill in.
---

# iOS App Store Submission

Apple's App Store Connect submission flow is a wide checklist of unrelated forms that all have to be green before you can hit Submit. It's deceptively easy to fill in 9 of 10 fields and waste hours hunting the 10th. This skill is the checklist + the gotchas, written by someone who just did it.

## When in this workflow

The user is preparing a single version of an iOS app (e.g. 1.0, 1.1) for review. They likely have:
- An app already created in App Store Connect (Apple ID assigned)
- At least one TestFlight build uploaded
- An Apple Developer account and team configured

If they don't yet, point them at `references/prerequisites.md` first. Don't try to bootstrap the developer account from scratch — Apple's web flow is the only path.

## The shape of the work

There are roughly **four phases**:

1. **Pre-flight** — pages and code that need to exist *before* you fill any forms (privacy policy URL, support URL, Info.plist hygiene). Apple verifies URLs at submission time, so deploy first.
2. **App Store Connect form-fill** — App Information, App Privacy, Pricing & Availability, version metadata in each locale, App Review Information.
3. **Assets** — screenshots (per locale, per device size).
4. **Build attach + submit** — pick a TestFlight build, answer export compliance, click Submit.

The phases are roughly sequential but you can parallelise pre-flight with App Store Connect form-fill — they touch different systems. Save assets for last because they require a working simulator build.

## Phase 1 — Pre-flight

### Privacy Policy + Support URLs

Apple **requires** both. They must be publicly reachable (HTTP 200) at submission time — Apple's review system fetches them. Common gotchas:

- A Vercel/Netlify deploy that's still building won't return 200 yet.
- A GitHub Pages site behind a custom domain with stale DNS will fail silently.
- Deploying a static `/privacy.html` is fine — they don't have to be in your main app's tech stack.

The privacy policy must accurately describe **what data the app actually sends off-device**. Don't copy a generic template — it has to match what you'll declare in the App Privacy questionnaire (Phase 2). If the app uses Firebase Anonymous Auth + writes any per-user record to Firestore, that's a User ID being collected — say so.

### Info.plist hygiene

Three things to set before uploading the build that goes to review:

- `ITSAppUsesNonExemptEncryption = false` — declares the app uses only standard system crypto (HTTPS). Without this, every build prompts you for export compliance manually in App Store Connect. Skip this only if the app uses non-standard crypto (proprietary algorithms, exotic ciphers).
- `NSLocationWhenInUseUsageDescription` — required if the app shows the user's location. Even if the app *only* requests "when in use", iOS may demand `NSLocationAlwaysAndWhenInUseUsageDescription` and `NSLocationAlwaysUsageDescription` too, because static analysis flags transitive deps that touch CLLocationManager's always-flow APIs (Google Maps SDK, Compass, etc.). Add all three with the same wording — see ITMS-90683.
- Any other `NS*UsageDescription` keys for camera, photos, contacts, microphone, etc. — even if your code path is dead, if the linker can reach the API.

## Phase 2 — App Store Connect form-fill

Detailed walkthrough is in `references/form-fields.md` — character limits, what to put where, and which fields silently reject specific characters.

The privacy questionnaire is its own beast — see `references/privacy-questionnaire.md`.

**A few things that bit us in this exact repo, surfaced here so they're not buried in references:**

- **"Promotional Text" rejects emoji and em-dashes.** "📍" and "—" both produce *"This field contains one or more invalid characters"* with no indication of which character. If you get that error, strip non-ASCII punctuation first. Plain unicode (Chinese characters) is fine — it's specifically emoji and curly punctuation.
- **Subtitle is hard-capped at 30 characters.** Not 50, not 60. Plan around it.
- **The "Sign-in required" checkbox in App Review Information defaults to checked.** If your app doesn't require login, **uncheck it**. Reviewers who can't log in to a passwordless app waste a review cycle and you lose 24-48h.
- **Manual vs Automatic release** is set on the version page, not at submit time. Decide before submitting.

## Phase 3 — Screenshots

Apple's iPhone 6.5" Display slot accepts **1242×2688** or **1284×2778**. Newer Pro Max simulators (iPhone 15 Pro Max, 16 Pro Max) render at 1290×2796 or 1320×2868 — those go in the **6.7"/6.9" slot**, not the 6.5" slot. If you only have a Pro Max simulator, `sips -z 2778 1284` resizes to the 6.5" spec; the small geometric distortion is invisible.

Minimum 3 screenshots per locale. Apple uses the first 3 in installation sheets, so put your strongest material first.

Full capture workflow (which simulator, how to drive it, how to switch locales mid-app, why the Mac keyboard fights with iOS predictive text) is in `references/screenshots.md`.

## Phase 4 — Build attach + submit

1. **Wait for TestFlight processing.** A freshly-uploaded build shows as "Processing" for 5-30 min. You can't attach it until it's "Ready to Submit" (yellow) or "Internal Testing" (green).
2. **Attach via "Add Build"** on the version page → pick the build → answer the export compliance question. If you set `ITSAppUsesNonExemptEncryption=false` in Phase 1, this step is auto-resolved for builds going forward.
3. **Run a final dry-read of the page.** Every section should have a green checkmark. Yellow dots = unfilled. Common last-minute miss: App Information's age rating wizard (7 steps, easy to abandon halfway).
4. **Add for Review → Submit to App Review.** Manual release means the version sits in "Pending Developer Release" until you click release in App Store Connect. Automatic releases the moment Apple approves.

Typical review SLA is 24-48h. Rejections come with reviewer notes — read them before re-submitting; the same reviewer often re-reviews and remembers prior context.

## Limits I keep forgetting

| Field | Limit |
|---|---|
| Subtitle | 30 chars |
| Promotional Text | 170 chars |
| Description | 4000 chars |
| Keywords | 100 chars (comma-separated, no spaces between) |
| App name | 30 chars |
| What's New | 4000 chars |

## What this skill cannot do

- Drive Apple's web file picker. Browser MCPs can't upload files via `<input type=file>` to apps.apple.com. The user has to drag-drop the screenshot files into the upload area themselves. Get the files staged on disk and tell them where they are.
- Take iOS Simulator screenshots while it's on a non-primary display without `switch_display`-style monitor selection — a no-display environment will fail at the screenshot step.
- Submit on the user's behalf without explicit confirmation. Submission is irreversible-ish (you can withdraw, but it's a manual step in App Store Connect). Always confirm before clicking Submit.

## Where to look next

- `references/prerequisites.md` — Developer account, certs, App ID, first TestFlight upload
- `references/form-fields.md` — Every field in App Store Connect, what to put, character limits
- `references/privacy-questionnaire.md` — How to map common app behaviour (Firebase Anonymous Auth, view counters, location) to Apple's data-type tree
- `references/screenshots.md` — Simulator selection, capture, locale switching, resize, upload
- `references/review-notes-template.md` — Template for the App Review Information notes field
- `references/gotchas.md` — The growing list of things that wasted an hour
