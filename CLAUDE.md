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
- **Navigation** — Compose Navigation, type-safe `@Serializable` routes in `ui/navigation/Routes.kt`
- **State** — `androidx.lifecycle.ViewModel` + Compose snapshot state
  (`mutableStateOf` / `derivedStateOf` / `mutableStateMapOf`). No StateFlow/LiveData boilerplate.
- **Backend** — Firebase via `dev.gitlive:firebase-*` (Anonymous Auth + Firestore with persistent cache)
- **Local storage** — Jetpack DataStore (currently for theme + language settings only)
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
│   ├── App.kt                      # Root composable, NavHost, theme + locale wiring
│   ├── Platform.kt                 # expect declarations
│   ├── di/                         # kotlin-inject AppComponent (process-singleton)
│   ├── domain/
│   │   ├── auth/                   # AuthService interface
│   │   ├── common/                 # Localizable + preferred(locale) helper
│   │   ├── restaurant/             # Restaurant, Tag, GooglePlaceData, OpeningStatus
│   │   └── settings/               # SettingsRepository (DataStore-backed)
│   ├── data/
│   │   └── remote/                 # FirebaseAuthService, FirestoreRestaurantRepository
│   └── ui/
│       ├── theme/                  # Pinwo brand palette + Source Sans 3 typography
│       ├── locale/                 # LocalAppLocale (expect/actual)
│       ├── navigation/             # @Serializable Routes
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

## Development Guidelines

- **Code style:** Kotlin official style.
- **Localisation:** All UI strings in `composeResources/values/strings.xml`
  (EN) and `values-zh/strings.xml` (ZH). Access via
  `stringResource(Res.string.*)`. **No hardcoded UI text.**
- **Theming:** Use `MaterialTheme.colorScheme.*` everywhere — never
  hardcode colours in UI files. Pinwo palette is defined in
  `ui/theme/AppTheme.kt`. Brand-red `primary` is reserved for map
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
- **Navigation:** Type-safe routes with `@Serializable data object` /
  `data class` in `ui/navigation/Routes.kt`. Detail is **not** a route —
  it's a `ModalBottomSheet` overlaid on the current screen, so the map
  state survives detail dismissal.
- **Date formatting:** Use `kotlinx-datetime-names` for localised
  weekday/month names. Never hardcode date formats.

## Architecture Notes

- Single Gradle module (`:composeApp`).
- **Domain layer:** models + repository interfaces. No Compose, no
  platform deps.
- **Data layer:** local DataStore + remote Firestore implementations.
- **UI layer:** Compose screens, each non-trivial screen has a `ViewModel`
  next to it (e.g., `MapViewModel`, `MapControlViewModel`).
- **Two-VM pattern on the map:** `MapViewModel` owns the restaurant
  pipeline (filters, derived lists, clusters, cover cache).
  `MapControlViewModel` owns the user's location + freshness.
  Camera state stays in the composable because
  `rememberCameraPositionState` is composable-bound. See
  `docs/MAP_PIPELINE.md` for the full story.
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
- Clustering is screen-distance-based and **invariant under pan**, so
  recompute fires only on zoom-idle, with a structural-equality short
  circuit to skip writes when the grouping didn't actually change.
- Marker bitmap descriptors are cached in `MarkerDescriptorCache` so
  cluster regroupings don't trigger re-rasterisation. Removing this
  cache is measurable (~100–300 ms hitch on big zoom changes).

## Data & Tag Taxonomy

- 22 tags = 10 regional (川/粤/京等) + 12 format (烤肉/火锅/小吃等).
- Single source of truth: `data/_tags.yaml`.
- Mirrored in **five** places:
  - `composeApp/src/commonMain/kotlin/com/novawerk/berlinfoodmap/domain/restaurant/Tag.kt`
  - `composeApp/src/commonMain/composeResources/values{,-zh}/strings.xml`
  - `composeApp/src/commonMain/kotlin/com/novawerk/berlinfoodmap/ui/components/TagChips.kt` (`tagDisplayName`)
  - `scripts/sync-to-firestore/index.js` (`KNOWN_TAGS`)
  - `web-apps/admin/src/types/restaurant.ts` (`REGIONAL_TAGS` / `FORMAT_TAGS`)
- CI fails the workflow if these drift (`scripts/sync-to-firestore/check-tags.mjs`).
- See `data/README.md` for the full pipeline reference (add a restaurant,
  add a tag, audit Firestore, etc.).

## Key Business Rules

1. Restaurant data is community-contributed (YAML → Firestore via CI).
2. Submissions can come as CSV / Excel / Google Sheets — dev team
   converts to YAML.
3. The 22-tag taxonomy is **canonical**. Don't extend it on a one-off
   basis; if a new tag is needed, follow the five-place update in
   `data/README.md`.
4. Visit tracking and favourites are **not yet** wired into UI. The
   `Visit` data class exists; the Firestore visit-count subcollection
   exists. UI surfacing is on the roadmap.
5. Anonymous Firebase auth is fire-and-forget at startup — Firestore's
   persistent cache serves the map and lists while auth completes, so
   first paint never waits on the network.
6. Map pin visuals: cover photo + ZH name + tag chip in a pill, brand-red
   POI dot. Closed restaurants render with a moon icon and faded text.
7. Search supports Chinese, English, and German names.
8. Dual-language UI (EN + ZH). German is restaurant-name-only.
9. Non-profit, community-driven — no ads, no analytics, no third-party
   tracking SDKs.

## Collaboration Model

- **Weekly sync** (30 min) during development with dev / product /
  design / beta testers
- **Pre-launch review** (30–60 min) with all stakeholders before release
- **Monthly ops review** after launch for user feedback, data updates,
  system stability
- **Async:** WeChat group for urgent items, GitHub Issues for bugs and
  feature requests
