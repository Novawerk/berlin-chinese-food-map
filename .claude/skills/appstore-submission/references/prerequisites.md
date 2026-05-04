# Prerequisites for App Store submission

Things that must exist before you start filling forms. None of this is automatable — Apple owns these flows.

## Apple Developer Program membership

- $99/year, paid by the legal entity that will own the app.
- Personal account vs Organization (D-U-N-S required for org). If the app is going under a company name on the App Store, it must be an Organization account.
- Enrollment can take days to weeks for orgs (D-U-N-S verification + Apple manual review).

## App ID and Bundle Identifier

- Created in [Apple Developer → Certificates, Identifiers & Profiles → Identifiers](https://developer.apple.com/account/resources/identifiers/list).
- Must match the `PRODUCT_BUNDLE_IDENTIFIER` in `Configuration/Config.xcconfig` (or in your Xcode project's build settings).
- Once an App ID is associated with an App Store Connect record, it's effectively permanent.

## Signing

Xcode Cloud handles this for most teams; if not:

- A Distribution certificate (Apple Distribution).
- An App Store provisioning profile bound to the Bundle Identifier.
- The Team ID (10-char alphanumeric) goes into `TEAM_ID` in `Configuration/Config.xcconfig` for KMP/Compose Multiplatform projects.

## App Store Connect record

- Created at [App Store Connect → My Apps → "+"](https://appstoreconnect.apple.com/apps).
- You pick: Bundle ID, SKU (free-form internal identifier — typically the bundle ID), Primary Language, name. Name has to be unique across the whole App Store.
- The "Apple ID" (a 10-digit number) is auto-assigned. Save it — it's used in deep links and a few SDKs.

## At least one TestFlight build uploaded

- Upload via Xcode (Product → Archive → Distribute), Xcode Cloud, or `xcrun altool`.
- Build number must be unique across all uploads for that version. Apple rejects duplicate `CFBundleVersion` strings.
- Builds enter "Processing" then become available 5-30 min later. Until processed, they don't appear in the version page's build picker.

## Once those exist

You can start filling forms (back to `SKILL.md`).
