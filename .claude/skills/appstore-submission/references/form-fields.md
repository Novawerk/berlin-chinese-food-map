# App Store Connect form fields

Every field that has to be filled before you can submit, in the order App Store Connect lays them out. Character limits, what to put, and what silently rejects what.

## App Information (`/distribution/info`)

App-level metadata, shared across all versions.

### Localizable Information

Per-locale. Click the language picker (top-right of "Localizable Information") to switch between locales.

| Field | Limit | Notes |
|---|---|---|
| Name | 30 chars | App name as it appears on the home screen and in search. Must be unique across the App Store. |
| Subtitle | **30 chars** | The hard limit catches people; "Berlin's Chinese restaurants, by locals" is 39 — too long. |

### General Information

App-level (not per-locale).

- **Bundle ID** — read-only after creation. Must match `PRODUCT_BUNDLE_IDENTIFIER`.
- **SKU** — internal identifier, you set it once. Same as bundle ID is fine.
- **Apple ID** — auto-assigned 10-digit number. Save it; needed for some SDKs (e.g. SmartAppBanner).
- **Primary Language** — the locale that's used as a fallback when other locales are missing. Cannot be changed casually after submission. Pick the language of your largest audience.
- **Category** — Primary required, Secondary optional. Reviewers care: a directory app like ours is "Food & Drink" primary, "Travel" secondary, not "Lifestyle".
- **Content Rights** — does the app contain third-party content you don't own? For most directory/utility apps the answer is "No". User-generated content from your own users counts as yours-with-license, not third-party.
- **License Agreement** — Apple's Standard EULA is fine unless you have legal-mandated terms.

### Age Ratings

7-step questionnaire. Click "Set Up Age Ratings" / "Edit". Don't abandon midway — half-completed wizards leave the field unsaved.

For a typical info/directory/utility app: every category gets "None" or "No". The Calculated Rating ends up at **4+**.

The "Age Categories and Override" radio at the end:
- "Not Applicable" — what you want if the app isn't specifically a kids' app and you have no compliance reason to override.
- "Made for Kids" — a different review track. Don't pick this unless you mean it.
- "Override to Higher Age Rating" — only if you specifically want to gate the app harder than Apple's calc would.

### App Encryption Documentation

If you set `ITSAppUsesNonExemptEncryption=false` in Info.plist, this section can be left alone — Apple reads the build's plist and treats it as exempt. Otherwise you have to upload export-compliance docs *before* you submit a build.

### Digital Services Act / Vietnam Game License / Regulated Medical Devices

- **DSA**: Click "Edit", pick Trader or Non-Trader. Most non-commercial / community apps are Non-Trader (you're not selling and not in EU commerce).
- **Vietnam Game License**: only if your app is a game distributed in Vietnam.
- **Regulated Medical Devices**: only if Health/Medical category and the app's a regulated device.

## App Privacy (`/distribution/privacy`)

See `references/privacy-questionnaire.md` — this section has its own walkthrough.

After answering, click **Publish** at the top right. Until you publish, the App Privacy block on the version page stays yellow.

## Pricing and Availability (`/distribution/pricing`)

### Price Schedule

Click "Add Pricing":
1. **Base Country or Region** — usually United States (USD).
2. **Price** — pick a tier. **$0.00** for free.
3. Confirm prices for all 175 countries (Apple auto-converts and shows the table).
4. Click Confirm.

### App Availability

Click "Set Up Availability" → "All Countries or Regions" → Next → Confirm. If you need to exclude specific countries (e.g. China-specific compliance issues), use "Specific Countries or Regions" instead.

Pre-orders: leave alone unless you're explicitly running a pre-order campaign.

### Tax Category

Default "App Store software" is right for free apps. Paid apps with subscriptions or specific tax treatments need "Manage" → adjust.

### iPhone and iPad Apps on Apple Silicon Macs

The "Make this app available" checkbox defaults to on with "Automatic (macOS 11.0)" — leave it. If your app uses iOS-only APIs that can't run on macOS, uncheck it.

### iPhone and iPad Apps on Apple Vision Pro

Defaults to on. If your app version isn't visionOS-compatible (most aren't yet), App Store Connect notes "Version 1.0 is not compatible and not available on the App Store on Apple Vision Pro". The checkbox staying on is fine — it just means future visionOS-compatible builds will auto-list.

## Version page (`/distribution/ios/version/inflight`)

This is the meat of every release.

### Previews and Screenshots

See `references/screenshots.md`.

### Per-locale text fields

Switch locale via the dropdown to the right of the "The assets and metadata below..." note.

| Field | Limit | Notes |
|---|---|---|
| Promotional Text | **170 chars** | Shows above the description on the App Store. Editable without re-review. **Rejects emoji and curly punctuation** — strip "📍", "—", "…" if you see "invalid characters". |
| Description | 4000 chars | The main pitch. URLs are clickable. |
| Keywords | **100 chars** total | Comma-separated, **no spaces** between words (Apple counts spaces). e.g. `pizza,delivery,fast` not `pizza, delivery, fast`. |
| Support URL | (URL) | Required. Must be reachable. |
| Marketing URL | (URL) | Optional but recommended. |
| Version | (read) | Pulled from the build's `CFBundleShortVersionString`. |
| Copyright | 200 chars | "© 2026 YourCompany". The "©" character is fine. |

### Routing App Coverage File

Only relevant for navigation apps. Skip otherwise.

### Build

Click "Add Build" → pick the TestFlight build that's marked "Ready to Submit" or has internal testing metrics. If you set `ITSAppUsesNonExemptEncryption=false` in the Info.plist, the export-compliance question disappears.

### Game Center

Now configured per-component in the **Game Center** sidebar section, not on the version page. The version page just shows a notice — the checkbox there is read-only.

### App Review Information

The reviewer's contact and demo info. Easy to forget — without contact info, submission fails.

| Field | What to put |
|---|---|
| Sign-in required (checkbox) | **Uncheck** if your app doesn't require login. Default is checked. Reviewers stuck behind a login they can't pass = rejection. |
| User name / Password | Only if Sign-in required. Use a sandbox account, not a real user's. |
| Contact: First name, Last name | A real human at your team. |
| Contact: Phone number | Reachable during review. International format (+countrycode) is safest. |
| Contact: Email | Reachable during review. |
| Notes | A 200-500-word demo walkthrough. See `references/review-notes-template.md`. Cover: how to launch the app, what the reviewer should see first, any quirks (e.g. "location is requested only on tap; you can deny"), where the data comes from, that there's no login. |
| Attachment | Optional. PDFs of screen flows or walkthrough videos sometimes help; usually unnecessary for simple apps. |

### App Store Version Release

- **Manually release this version** — version sits in "Pending Developer Release" after approval until you click release.
- **Automatically release this version** — released the moment Apple approves.
- **Automatically release after App Review, no earlier than [date]** — useful for coordinated launches.

For a v1.0 launch, manual is usually safer (you control the announcement timing).

## Final dry-read

Before "Add for Review":

- All sections show green checkmarks (no yellow dots).
- Both locales (or however many you added) have screenshots, description, keywords.
- Build is attached and shows "Used in iOS App 1.0".
- App Review Information has contact info AND notes (notes are easy to forget).
- App Privacy is **Published**, not just filled.
- Age Ratings shows a badge (e.g. "4+ in 173 countries"), not "Set Up Age Ratings".

If everything's green, "Add for Review" → "Submit to App Review".
