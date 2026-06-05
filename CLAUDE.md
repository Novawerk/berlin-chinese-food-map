# CLAUDE.md

Guidance for AI assistants (Claude Code etc.) working in this repository.
Be opinionated; keep this file accurate. If you change architecture in a way
that contradicts what's here, update this file in the same commit.

## Project Overview

**Berlin Chinese Food Map (柏林中餐地图)** is a community-driven, non-profit
digital guide to Chinese restaurants in Berlin. Built with Kotlin Multiplatform
targeting Android and iOS, with Compose Multiplatform for shared UI.

**Status:** MVP development. POC for the four pillars (mobile app, data
pipeline, landing page, admin panel) shipped in Q1 2026 — current focus is
polishing the map screen, restaurant detail screen, and the data pipeline
around the 22-tag taxonomy + opening-hours signals landed in May 2026.

## Deliverables

1. **Native Mobile App** (iOS + Android) — Kotlin Multiplatform + Compose
2. **Landing Page** (https://berlinfoodmap.novawerk.io/) — `web-apps/landing-page/`
3. **Admin Panel** — react-admin CRUD over Firestore — `web-apps/admin/`
4. **Data Pipeline** — YAML in `data/restaurants/` → GitHub CI → Firestore

## Tech Stack

- **Kotlin** 2.2 — `kotlin.code.style=official`
- **Compose Multiplatform** 1.10, **Material Design 3 Expressive**
  (`languageSettings.optIn("androidx.compose.material3.ExperimentalMaterial3ExpressiveApi")`)
- **Navigation** — None. `MainShell` renders the Map and Settings tabs as
  overlays in a single `Box`; the cross-platform `BackHandler` from
  `org.jetbrains.compose.ui:ui-backhandler` returns from Settings to Map.
  Compose Navigation + `@Serializable` routes were removed when the
  shell collapsed to a single composable so the map keeps its state
  across tab switches.
- **State** — `androidx.lifecycle.ViewModel` + Compose snapshot state
  (`mutableStateOf` / `derivedStateOf` / `mutableStateMapOf`). No StateFlow/LiveData boilerplate.
  ViewModels for the map (`MapViewModel`, `MapControlViewModel`) are
  **`@AppScope` DI singletons** resolved by `AppComponent` — touched at
  the top of `App()` so their `init` blocks run during the splash hold.
- **Backend** — Firebase via `dev.gitlive:firebase-*` (Anonymous Auth, Firestore
  with persistent cache, Analytics, Crashlytics). Crashlytics auto-collects on
  both platforms once the SDK is on the classpath; Analytics events are routed
  through the `AnalyticsService` interface (see DI section).
- **Local storage** — Jetpack DataStore (settings + favorites)
- **Maps** — `eu.buney.maps:kmp-maps-compose` (Google Maps on Android, MapKit on iOS)
- **Images** — Coil 3 (`io.coil-kt.coil3:coil-compose` + `coil-network-ktor3`)
- **Geolocation** — Compass (`dev.jordond.compass:geolocation` + `geolocation-mobile`).
  Handles runtime permission flow internally — no `rememberLauncherForActivityResult` needed.
- **DI** — kotlin-inject (`me.tatarka.inject:kotlin-inject-runtime-kmp`) with KSP and `@KmpComponentCreate`
- **Networking** — Ktor Client (OkHttp on Android, Darwin on iOS)
- **Date/Time** — kotlinx-datetime + `io.github.adrcotfas:kotlinx-datetime-names`
- **Android** — minSdk 24, compile/target 36, JVM 17
- **iOS** — Swift wrapper in `iosApp/`, KMP framework named `ComposeApp`

## Project Structure

```
composeApp/src/
├── commonMain/kotlin/com/novawerk/berlinfoodmap/
│   ├── App.kt                      # Root composable, splash → onboarding → MainShell, overlay-based UI
│   ├── Platform.kt                 # expect declarations
│   ├── di/                         # kotlin-inject AppComponent (@AppScope process-singletons)
│   ├── domain/
│   │   ├── analytics/              # AnalyticsService interface (events + crash logs)
│   │   ├── auth/                   # AuthService interface
│   │   ├── common/                 # Localizable + preferred(locale) helper
│   │   ├── favorites/              # FavoritesRepository
│   │   ├── restaurant/             # Restaurant, Tag, GooglePlaceData, OpeningStatus
│   │   └── settings/               # SettingsRepository (DataStore-backed)
│   ├── data/
│   │   ├── remote/                 # FirebaseAuthService, FirestoreRestaurantRepository, FirebaseAnalyticsService
│   │   └── store/                  # RestaurantStore (app-scoped data layer for the map)
│   └── ui/
│       ├── theme/                  # Pinwo brand palette + Source Sans 3 typography
│       ├── locale/                 # LocalAppLocale (expect/actual)
│       ├── components/             # Shared UI (TagChips, OpeningStatusBadge, EmptyState, …)
│       └── pages/
│           ├── map/                # ★ See docs/MAP_PIPELINE.md — non-trivial pipeline
│           ├── detail/             # DetailScreen modal sheet + photo pager
│           ├── search/             # SearchScreen
│           └── settings/           # SettingsScreen
├── androidMain/                    # MainActivity, Platform.android.kt, location dot icon
└── iosMain/                        # MainViewController, Platform.ios.kt
iosApp/                             # Native Xcode project (Swift)
web-apps/
├── landing-page/                   # Next.js, bilingual
└── admin/                          # React + Vite + react-admin
data/restaurants/                   # YAML restaurant data (canonical source)
data/_tags.yaml                     # Tag taxonomy (single source of truth)
docs/MAP_PIPELINE.md                # Map architecture deep-dive
scripts/sync-to-firestore/          # CI sync + tag validator + ad-hoc tools
```

There is no `ui/navigation/` directory — `Routes.kt` was removed when
the app shell collapsed to a single overlay-based `MainShell`.

## Build Commands

```bash
./gradlew :composeApp:assembleDebug        # Build debug APK
./gradlew :composeApp:installDebug         # Install on connected device/emulator
./gradlew :composeApp:compileDebugKotlinAndroid  # Quick type-check
./gradlew clean build                      # Full clean build (slow)
```

**Do not build iOS locally.** The repo's CI/dev story is Android-first; iOS
sources must compile (commonMain compiles for all targets) but you should
not attempt to run `xcodebuild` or open Xcode for verification. Trust the
type system.

To launch on a connected device after install:

```bash
~/Library/Android/sdk/platform-tools/adb shell monkey \
  -p com.novawerk.berlinfoodmap -c android.intent.category.LAUNCHER 1
```

## Testing

```bash
./gradlew :composeApp:testDebugUnitTest     # All unit + UI tests (JVM, headless)
```

`testDebugUnitTest` runs the whole suite on the JVM — no emulator. The
Android Build workflow runs it on every push (it gates the APK build).

Two layers, by source set:

- **`commonTest/`** — pure logic, shared across all targets (`kotlin.test` +
  `kotlinx-coroutines-test`). Covers opening-hours (`OpeningStatus`),
  localization (`Localizable.preferred`), the tag taxonomy, restaurant
  helpers, the **map filter logic** (`filterRestaurants` in
  `ui/pages/map/MapFilters.kt`), and the repo's **geo/search helpers**
  (`haversineKm` / `nameMatchesQuery` in `data/remote/RestaurantQuery.kt`).
- **`androidUnitTest/`** — Compose UI ("e2e") tests via Robolectric +
  `createComposeRule()`, run headless on the JVM (`@RunWith(RobolectricTestRunner)`,
  `@GraphicsMode(NATIVE)`, `@Config(sdk = [34])`). Target the non-map
  list/detail **components** (`RestaurantCard`, `TagChips`,
  `OpeningStatusBadge`).

Two deliberate seams exist so the meaty logic is testable without standing up
DI/Firestore: `MapFilters.kt` (the VM's `restaurantsFiltered` is a thin
`derivedStateOf` over it) and `RestaurantQuery.kt` (the Firestore repo can't be
constructed in a test — it touches `Firebase.firestore` at field init). Keep
new business logic in pure functions like these rather than inline in VMs/repos.

**The map screen has no UI test** — it embeds a native Google Maps view that
can't render under Robolectric (and a worktree build has no `secrets.properties`,
so the map is blank anyway). Its filter/cluster logic is covered by the unit
tests instead. UI tests are Android-only because common `runComposeUiTest` on
iOS/desktop needs Compose Multiplatform 1.11 (this repo is on 1.10.3); the
non-map components are platform-agnostic, so that's no real coverage loss.

When adding a UI test, give the component a `testTag` (see `TAG_CHIP_ALL`,
`tagChipTestTag`, `OPENING_STATUS_BADGE_TAG`) rather than asserting on
localized strings, which differ by locale. Build fixtures with
`commonTest/.../testutil/Fixtures.kt`.

## Development Guidelines

- **Code style:** Kotlin official style.
- **Localisation:** All UI strings in `composeResources/values/strings.xml`
  (EN) and `values-zh/strings.xml` (ZH). Access via
  `stringResource(Res.string.*)`. **No hardcoded UI text.** Write
  apostrophes/quotes raw (`don't`, `"x"`) — **never escape them** as `\'`
  / `\"`. Compose Resources, unlike Android's aapt, renders the backslash
  literally on screen. `\n` (paragraph break) is the only allowed escape;
  `scripts/check-string-escapes.sh` enforces this and fails the Android
  Build workflow on any other stray backslash.
- **Theming:** Use `MaterialTheme.colorScheme.*` everywhere — never
  hardcode colours in UI files. Pinwo palette is defined in
  `ui/theme/AppTheme.kt`, which also pins explicit warm-neutral
  `surfaceBright/Dim/surfaceContainer*` ramps on both schemes — M3
  leaves those tonal tokens unspecified, so the bottom nav, sheets,
  and chips were falling back to the baseline violet tint. Keep the
  ramps cream/ink-warm; don't let surfaces drift lavender. Brand-red
  `primary` is reserved for map
  content — POI marker pins, cluster badges, brand emphasis on cards.
  Map FABs (locate, filter) use neutral `surfaceContainerHigh` +
  `onSurface` for quiet chrome that won't compete with markers; an
  earlier iteration used `secondary` (too heavy) and then `tertiary`
  (olive read as off-brand). The DetailScreen StickyActionBar's
  primary action still uses `secondary` (PinwoWine) for visual
  weight inside a content surface. The "browse by district"
  shortcut lives in the search bar trailing icon — districts are a
  navigational primitive, same family as searching by name.
- **Compose-state-first VMs:** ViewModels expose `mutableStateOf` /
  `mutableStateListOf` / `derivedStateOf` properties directly. UI reads
  them in composition — Compose's snapshot system subscribes
  automatically. **Do not introduce `StateFlow` + `collectAsState` for
  UI state.** Reserve Flow for things that genuinely need it (data
  sources, snapshotFlow bridges into Compose state).
- **Localised text:** for `Localizable` fields (description, etc.) use
  `desc.preferred(LocalAppLocale.current)` rather than stacking zh + en.
  Names are bilingual on cards (zh primary, en secondary) by convention.
- **DI:** kotlin-inject with `@KmpComponentCreate`. Add new injectable
  classes by annotating with `@Inject`; expose them on `AppComponent` if
  consumers need them.
- **Platform differences:** `expect`/`actual` via `Platform.kt` and per-feature
  files (`LocationRequester` → deleted in favour of Compass; `MyLocationDotIcon`
  is still expect/actual; `StableMarkerIcon` is expect/actual to work around a
  library bug — see `docs/MAP_PIPELINE.md`).
- **Navigation:** None. `MainShell` shows the Map full-screen and
  slides the Settings panel over the top when the bottom-nav Settings
  tab is active; `BackHandler` returns to the Map. Detail is also a
  `ModalBottomSheet` overlaid on the live map, so map state survives
  detail dismissal. Don't reintroduce `NavHost` or `@Serializable`
  routes — keeping the map permanently mounted is the whole point.
- **Date formatting:** Use `kotlinx-datetime-names` for localised
  weekday/month names. Never hardcode date formats.

## Architecture Notes

- Single Gradle module (`:composeApp`).
- **Domain layer:** models + repository interfaces. No Compose, no
  platform deps.
- **Data layer:** local DataStore + remote Firestore implementations,
  plus an app-scoped `RestaurantStore` (in `data/store/`) that holds
  the live restaurant list, favorites set, and Coil-loaded cover
  bitmaps so the map and detail surface read from one source.
- **UI layer:** Compose screens, each non-trivial screen has a `ViewModel`
  next to it (e.g., `MapViewModel`, `MapControlViewModel`).
- **Two-VM pattern on the map:** `MapViewModel` owns the restaurant
  pipeline (filters, derived lists, dense-marker ids, cover cache
  reads). `MapControlViewModel` owns the user's location + freshness.
  Both are `@AppScope` DI singletons resolved by `AppComponent`, not
  per-NavBackStackEntry instances — they're touched at the top of
  `App()` so their data observers warm up during the splash hold.
  Camera state still stays in the composable because
  `rememberCameraPositionState` is composable-bound. See
  `docs/MAP_PIPELINE.md` for the full story.
- **Splash + map render in parallel:** `MainShell` mounts during the
  Splash phase too, hidden behind an opaque `SplashScreen` overlay.
  Tile fetch, `onMapLoaded`, and the first `recomputeDenseIds` all
  happen while the splash is still on-screen.
- **Privacy-first:** local storage preferred, no unnecessary cloud sync.
  Anonymous Firebase auth gives view-counter docs a stable id; that's
  the only personal data we hold.

## Map Screen Pipeline (TL;DR)

The map screen is the most architecturally dense part of the codebase.
**Read [`docs/MAP_PIPELINE.md`](docs/MAP_PIPELINE.md) before changing any
file under `ui/pages/map/`.** A handful of non-obvious things to know:

- Marker bitmap rendering goes through a **custom**
  `rememberStableComposeBitmapDescriptor` (in `StableMarkerIcon.kt`)
  because the upstream `eu.buney.maps` library's Android impl puts the
  content lambda into the `remember` keys, which makes its caching
  ineffective and produces visible flicker. Don't replace it with the
  library function.
- Cover photos are loaded via Coil's `imageLoader.execute()` directly,
  not via `AsyncImagePainter`. The cache lives in the VM
  (`MapViewModel.markerCovers: SnapshotStateMap<String, MarkerCover>`).
- **No clustering.** Pills that would visually overlap collapse to
  small circular markers (red dot / red heart / gold star) — see
  `MarkerDot.kt`. The detection is AABB-based, screen-distance, and
  **invariant under pan**, so recompute fires only on zoom-idle with a
  set-equality short circuit. There is no `ClusterMarker` and no
  numbered cluster badge.
- Marker bitmap descriptors are cached in `MarkerDescriptorCache` so
  the dot ↔ pill transitions during zoom don't trigger re-rasterisation.
  The dot variant has its own 3-entry shared cache
  (`rememberMarkerDotDescriptorCache`).

## Data & Tag Taxonomy

- 22 tags = 10 regional (川/粤/京等) + 12 format (烤肉/火锅/小吃等).
- Single source of truth: `data/_tags.yaml`. After editing it, run
  `cd scripts/sync-to-firestore && npm run gen:tags` to regenerate the mirrors.
- **Generated** from `_tags.yaml` (rewritten between `@gen:tags` fences by
  `scripts/sync-to-firestore/gen-tags.mjs` — don't hand-edit the fenced regions):
  - `scripts/sync-to-firestore/index.js` (`KNOWN_TAGS`)
  - `web-apps/admin/src/types/restaurant.ts` (`REGIONAL_TAGS` / `FORMAT_TAGS`)
  - `composeApp/src/commonMain/composeResources/values{,-zh}/strings.xml`
    (tag display names — the per-tag `*_desc` editorial strings stay hand-written)
- **Hand-written** (the compiler keeps these honest — both are exhaustive
  `when (tag)` over the enum, and `check-tags.mjs` guards the enum itself):
  - `composeApp/src/commonMain/kotlin/com/novawerk/berlinfoodmap/domain/restaurant/Tag.kt`
  - `composeApp/src/commonMain/kotlin/com/novawerk/berlinfoodmap/ui/components/TagChips.kt` (`tagDisplayName`)
- CI fails the workflow if anything drifts (`npm run check:tags` =
  `gen-tags --check` + `check-tags.mjs`).
- See `data/README.md` for the full pipeline reference (add a restaurant,
  add a tag, audit Firestore, etc.).

## Key Business Rules

1. Restaurant data is community-contributed (YAML → Firestore via CI).
2. Submissions can come as CSV / Excel / Google Sheets — dev team
   converts to YAML.
3. The 22-tag taxonomy is **canonical**. Don't extend it on a one-off
   basis; if a new tag is needed, edit `data/_tags.yaml`, run
   `npm run gen:tags`, add the Kotlin enum + `when` branches, then
   `npm run check:tags` (see `data/README.md`).
4. Favourites are wired through the UI (heart toggle on detail screen,
   floating heart badge on the map pill, "Favourites only" filter
   toggle, dedicated heart variant in the dense-marker dot). Visit
   tracking still has a `Visit` data class + Firestore subcollection
   but no UI surface yet.
5. Anonymous Firebase auth is fire-and-forget at startup — Firestore's
   persistent cache serves the map and lists while auth completes, so
   first paint never waits on the network.
6. Map pin visuals: cover photo + ZH name + tag chip in a pill, brand-red
   POI dot. Favourited and editor-picked venues get a floating
   heart / star badge overhanging the pill's top-left corner. Closed
   restaurants render with a moon icon and faded text. When a pill
   would overlap another at the current zoom, both collapse to a
   small circular marker (`MarkerDot`).
7. Search supports Chinese, English, and German names.
8. Dual-language UI (EN + ZH). German is restaurant-name-only.
9. Non-profit, community-driven — no ads, no third-party tracking SDKs.
   Firebase Analytics + Crashlytics are the only first-party telemetry,
   used to size the user base and triage crashes. Don't log restaurant
   names, user-typed search queries, GPS coordinates, or any other PII —
   restaurant ids and short tag/route strings only. The anonymous Firebase
   uid is set as the Analytics user id and Crashlytics user id at startup
   so reports group per install.

## Collaboration Model

- **Weekly sync** (30 min) during development with dev / product /
  design / beta testers
- **Pre-launch review** (30–60 min) with all stakeholders before release
- **Monthly ops review** after launch for user feedback, data updates,
  system stability
- **Async:** WeChat group for urgent items, GitHub Issues for bugs and
  feature requests
