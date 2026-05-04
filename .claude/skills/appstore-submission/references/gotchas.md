# Gotchas

The growing list of things that have wasted at least an hour. Add to this file as new ones surface.

## Promotional Text rejects emoji and curly punctuation

App Store Connect's "Promotional Text" field returns *"This field contains one or more invalid characters"* with no indication of which character. Common offenders:

- 📍 (and any other emoji)
- — (em-dash)
- … (horizontal ellipsis)
- " " (curly double quotes)
- ' ' (curly single quotes / apostrophes)

Plain CJK / Cyrillic / Greek / Arabic letters are fine. The Description field accepts emoji and curly punctuation, just Promotional Text is fussy.

## Subtitle is hard-capped at 30 characters

Not "approximately 30", not "up to 50". Exactly 30. Plan your tagline accordingly.

## Subtitle for new locales inherits the primary locale's value

When you add a new locale (e.g. add English to a Chinese-primary app), the Subtitle field shows the *primary* locale's subtitle as the visible value. It looks like a placeholder but it's the actual stored value. You have to triple-click and re-type to enter a locale-specific subtitle, or use the DOM directly to clear it. React's controlled input doesn't always pick up programmatic value setters — `triple_click` + type works reliably.

## "Sign-in required" defaults to checked

App Review Information's "Sign-in required" checkbox is **on** by default, which is wrong for any app that doesn't actually require login. If left checked, the reviewer will look for a username/password form, not find one, and reject the app for missing demo credentials.

**Always uncheck it for passwordless apps** before submitting.

## Apple's data-collection definitions don't match common usage

- "Tracking" specifically means **cross-app/cross-website**. Plain in-app analytics is **not** tracking.
- "Linked to User" requires identity linkage (name, email, account, etc.). An anonymous random ID alone is **not** linked.
- "Collected" requires off-device transmission. Location used only on-device for a marker pin is **not** collected.

Misanswering these is the #1 cause of "Privacy" rejections.

## App Privacy questionnaire requires Publish, not just Save

After filling all data types, the App Privacy section will show your selections — but the version page still says App Privacy is yellow (incomplete). You have to click **Publish** at the top right of `/distribution/privacy` for it to be acknowledged. Saving inside the wizard is not enough.

## Game Center moved out of the version page

Game Center components used to be selectable from the version page. As of late 2024 / early 2025, they're configured in the dedicated Game Center sidebar section and the version page has a read-only notice. If you need leaderboards/achievements, set them up there.

## ITSAppUsesNonExemptEncryption affects already-uploaded builds too

If you forgot to add `<key>ITSAppUsesNonExemptEncryption</key><false/>` to Info.plist before uploading the build, App Store Connect will prompt for export compliance when you attach the build at submit time. You can answer "No, my app doesn't use encryption that requires documentation" and proceed. It's not a blocker, but adding the plist key for future builds saves the prompt.

## Apple's "always location" string is required even for "when in use" apps

If your app, or *any of its transitive dependencies*, references CLLocationManager's always-flow APIs (`requestAlwaysAuthorization`, etc.), Apple's static analysis flags it and demands `NSLocationAlwaysAndWhenInUseUsageDescription` and `NSLocationAlwaysUsageDescription` in Info.plist. The Google Maps iOS SDK and the Compass library both touch these APIs even if you never call them. Symptom: ITMS-90683 warning in TestFlight.

Fix: add the same wording for all three keys (`NSLocationWhenInUseUsageDescription`, `NSLocationAlwaysAndWhenInUseUsageDescription`, `NSLocationAlwaysUsageDescription`), since the runtime never invokes always-flow.

## Vercel deploy from a monorepo subdirectory needs careful linking

When the Vercel project has rootDirectory set to `web-apps/landing-page`, running `vercel deploy` from `web-apps/landing-page/` resolves to `web-apps/landing-page/web-apps/landing-page` (the configured root applied on top of the cwd). The fix: deploy from the repo root with `.vercel/` linked there, or just push to the GitHub branch that triggers the Vercel deploy hook.

## Pro Max simulators output dimensions outside the 6.5" slot

iPhone 15 Pro Max → 1290×2796. iPhone 16/17 Pro Max → 1320×2868. Apple's iPhone 6.5" Display slot only accepts 1242×2688 or 1284×2778. iPhone 15 Plus is the cleanest 6.5" option, but on iOS 26 it sometimes renders at 1290×2796 anyway. Always verify with `file output.png` and `sips -z` if needed.

## macOS keyboard fights with iOS Simulator predictive text

Typing into the simulator from the Mac keyboard sends keystrokes that the iOS predictive text bar interprets as accent variants (`à á â ä ǎ æ ã å ā ă ą`). This corrupts any "search" or "form fill" screenshot that requires typed input. Workarounds:

- Skip those screenshots if not critical.
- Use `xcrun simctl pasteboard` to set clipboard, then paste in the simulator.
- Click iOS keyboard letters one at a time (slow but reliable).

## Chrome MCP can't upload files to App Store Connect

`mcp__Claude_in_Chrome__file_upload` returns *"Not allowed"* on `<input type=file>` elements that target App Store Connect. The OS file picker that opens when you click "Choose File" is owned by Chrome (read-tier in computer-use), so you can't drive it via OS automation either.

The only reliable path is the user dragging the files in. Stage the files, open Finder there (`open <dir>`), and tell the user explicitly.

## Privacy Policy URL must be reachable at submission time

Apple's review system fetches the URL when you click Submit. If your deploy is still building (Vercel preview building from the same commit) or DNS is stale, the URL will 404 and submission will fail with a generic "URL not reachable" error. Verify with `curl -sI` before submitting.

## "Add for Review" doesn't submit — it just enters the review-prep flow

The button at the top right that says **"Add for Review"** does NOT submit. It opens a summary screen where you can review what you're about to submit. The actual submission is the **"Submit to App Review"** button on that screen. People sometimes click "Add for Review", see the summary screen, navigate away, and assume they submitted.

## Manual release requires you to actually release

If you pick "Manually release this version" and Apple approves, the version sits in **"Pending Developer Release"** in App Store Connect indefinitely. You have to log in and click "Release This Version" to make it live. Easy to forget if you submit and walk away.

## Some categories require additional permits

Vietnam Game License, Korea Game Rating Board, Russia age-restricted classifications — country-specific. App Store Connect won't always block submission, but it will reject country availability silently. Check **App Store Regulations & Permits** at the bottom of App Information if you're targeting those markets.
