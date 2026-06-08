# App Store screenshots

The screenshot pipeline is the messiest part of the workflow. The web upload UI doesn't accept files via Chrome MCP automation, the macOS keyboard fights with the iOS Simulator's predictive text, and Apple's "iPhone 6.5" Display" slot has a narrower set of accepted dimensions than the simulator output.

## Required dimensions

The 6.5" Display slot accepts:

- **1242 × 2688** (iPhone XS Max, 11 Pro Max)
- **1284 × 2778** (iPhone 12-15 Plus)

Newer Pro Max sims output:

- **1290 × 2796** (iPhone 15 Pro Max) — goes in the **6.7"** slot
- **1320 × 2868** (iPhone 16/17 Pro Max) — goes in the **6.9"** slot

If you only have a Pro Max simulator, just resize down to 6.5" with `sips`:

```bash
sips -z 2778 1284 input.png --out output.png
```

The slight aspect-ratio change (1290:2796 vs 1284:2778) is undetectable visually.

## Picking the simulator

For the cleanest 1284×2778 output, create an iPhone 15 Plus simulator:

```bash
xcrun simctl create "iPhone 15 Plus AppStore" \
  com.apple.CoreSimulator.SimDeviceType.iPhone-15-Plus \
  com.apple.CoreSimulator.SimRuntime.iOS-26-3
xcrun simctl boot <UUID-from-above>
open -a Simulator
```

Note: on iOS 26, the iPhone 15 Plus may render at 1290×2796 anyway (Apple normalised some screen sizes). If the screenshot file's `file output.png` reports something other than 1284×2778, just `sips` it.

## Capturing

```bash
xcrun simctl io <UUID> screenshot output.png
```

This captures the device-pixel image (full Retina), not what's visible in the macOS Simulator window.

## Driving the simulator

The simulator window is controllable through computer-use MCP, but two pitfalls:

### macOS keyboard input goes through predictive text

When you `type "noodle"`, the macOS keyboard sends characters that the iOS predictive text bar interprets as accent variants (`à á â ä ǎ æ ã å ā ă ą`). This breaks search-with-text screenshots. Workarounds:

- Skip the search-with-text screenshot. The plain map screenshot already shows the search bar prominently.
- If you really need text in the search field, type it via `simctl pasteboard` and paste into the field, or use an iOS keyboard click-by-click (tedious).

### The simulator may be on a non-primary display

If the user has multiple monitors, the Simulator window often opens on the secondary one. Computer-use's screenshot defaults to the primary monitor. Check:

```bash
# In the request_access response, look at windowLocations
# If displays[0].label is not the primary, switch:
```

Then call `switch_display` with the named monitor before clicking — coordinates relate to the captured monitor.

## Recommended shot list (4-6 per locale)

For a typical map/directory app:

1. **Map overview** — show clusters, brand POI markers, "list of nearby" cards. The "hero" screenshot.
2. **Filter sheet** — taxonomy / tags / categories. Demonstrates the breadth of content.
3. **Detail sheet** — restaurant detail with photos, address, opening hours, action buttons.
4. **Detail expanded** — hours / contact / actions visible (drag the modal up).
5. **Settings** — language, theme, About, Version. Low-key but reassuring.
6. **(optional) Search active** — search bar with results.

Apple uses the first 3 in installation sheets, so put your strongest material first.

## Switching locale mid-app for the second set

Most apps that support multiple languages have an in-app locale toggle. For ours: Settings → Language → 中文/English. After switching, the app re-renders in the new locale and you can re-capture all the same screens.

If the app uses system locale only:

```bash
# Set the simulator's preferred language and region
xcrun simctl spawn <UUID> defaults write -g AppleLanguages '("zh-Hans")'
xcrun simctl spawn <UUID> defaults write -g AppleLocale 'zh_CN'
xcrun simctl terminate <UUID> com.your.bundle.id
xcrun simctl launch <UUID> com.your.bundle.id
```

## Uploading — the manual handoff

The Chrome MCP `file_upload` tool returns "Not allowed" when targeting App Store Connect's `<input type=file>`. Even after `style.display='block'` to make the input "real", the browser still rejects programmatic uploads. The OS file picker that opens when you click "Choose File" is owned by Chrome (read-tier in computer-use), so you can't drive it either.

**The workaround**: stage the screenshot files in a known location, open Finder there, and tell the user to drag-drop them into the App Store Connect upload box. e.g.:

```bash
mkdir -p art/appstore-screenshots
# (capture files into that directory)
open art/appstore-screenshots/  # Reveals folder in Finder
```

Then guide the user: "Drag the 4 `*-zh.png` files from the Finder window into the 'Drag up to 3 app previews and 10 screenshots here' box on App Store Connect. Then switch the locale dropdown to English (U.S.) and drag the `*-en.png` files."

## Naming convention that works

`<order>-<screen>-<locale>.png` — easy to upload in order, easy to spot the locale.

```
01-map-en.png        01-map-zh.png
02-filter-en.png     02-filter-zh.png
03-detail-en.png     03-detail-zh.png
04-settings-en.png   04-settings-zh.png
```

When sips-resizing, keep the unresized originals around (e.g. in a `raw/` subdirectory) — if Apple rejects the dimensions later, you have the source pixels.
