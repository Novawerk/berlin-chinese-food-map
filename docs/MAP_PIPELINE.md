# Map screen pipeline

The map screen is the most architecturally dense surface in the app —
~14 files in `composeApp/src/commonMain/kotlin/.../ui/pages/map/`, plus
two `actual` implementations. This doc explains *why* it's structured
this way and what each file owns. Read this before changing anything
under `ui/pages/map/`.

## TL;DR

- **Two ViewModels** because they have different lifecycles and
  dependencies: `MapViewModel` (restaurant pipeline, painters,
  dense-id set) and `MapControlViewModel` (location + freshness).
- **Camera state lives in the composable** because
  `rememberCameraPositionState` is composable-bound to the
  `GoogleMap` widget; hoisting it would cause sync drift.
- **No clustering.** Every restaurant gets its own marker. When a pill
  would visually overlap another, it collapses to a compact dot
  (regular = red dot, favourite = red heart, featured = gold star). The
  user always sees individual points-of-interest; zooming in expands
  the dots back into pills.
- **Three layered caches** for marker rendering:
  1. Coil's disk cache (cover JPEGs)
  2. `MapViewModel.markerCovers` — decoded `coil3.Image` per restaurant
  3. `MarkerDescriptorCache` — rasterised pill `BitmapDescriptor` per (restaurant, coverReady) pair
  Plus a tiny 3-entry `rememberMarkerDotDescriptorCache` for the dot variants.
- **A custom `rememberStableComposeBitmapDescriptor`** because the
  upstream library's Android impl has a subtle bug (content lambda in
  `remember` keys) that defeats its own caching.
- **Dense detection is two-phase** — projection on Main (SDK isn't
  documented thread-safe), grid math on `Dispatchers.Default`.
- **Pan does no work.** Pairwise screen distances are translation-
  invariant, so the dense-id set is invariant under pan; only zoom-idle
  triggers a recompute, and even that short-circuits if the resulting
  set is unchanged.

## File map

| File | Role |
|------|------|
| `MapScreen.kt` | UI orchestration only — layout, camera state, snapshotFlow → VM bindings. No business logic. |
| `MapViewModel.kt` | Restaurant pipeline: filters, derived lists, dense-id set, cover image cache, viewport bounds. |
| `MapControlViewModel.kt` | Map-control state: user location + freshness via Compass. |
| `MapStyle.kt` | `BERLIN_MAP_STYLE_JSON`, `DEFAULT_BERLIN_CAMERA`, `rememberBerlinMapConfig()`. |
| `MarkerColors.kt` | `myLocationDotIcon()` expect/actual for the user-location blue dot. |
| `Clustering.kt` | `projectAll`, `detectDenseRestaurants`. Pure functions. (File name historical — there's no clustering anymore, just AABB dense detection.) |
| `MarkerCover.kt` | `sealed interface MarkerCover { NoUrl, Failed, Loaded(Image) }`. |
| `MarkerDescriptorCache.kt` | `SnapshotStateMap<String, CachedMarkerDescriptor>` for pills + 3-entry `SnapshotStateMap<MarkerDotKind, BitmapDescriptor>` for dots. |
| `StableMarkerIcon.kt` (+ `.android.kt` / `.ios.kt`) | Custom `rememberStableComposeBitmapDescriptor` — replaces the buggy library function. |
| `RestaurantMarker.kt` | Single-restaurant pill marker composable (cover state gate, descriptor cache lookup, `Marker` emission). |
| `MarkerDot.kt` | Compact dot/heart/star marker for restaurants in the dense set. |
| `MiniRestaurantCard.kt` | The pill content (cover thumb + name + tag) composed *inside* the bitmap descriptor. |
| `NearbyCard.kt` | Bottom card-row card for restaurants in the visible viewport. |
| `FilterSheet.kt` | Tabbed filter sheet (Cuisine / Style / Neighbourhood). |
| `CardTags.kt` | `Restaurant.cardTags()` helper — regional tags first, fall back to format. |

## VM split

### Why two

| | `MapViewModel` | `MapControlViewModel` |
|---|---|---|
| **Owns** | filters, restaurants, dense ids, marker covers | user location, freshness flag |
| **Depends on** | `RestaurantStore`, Coil `PlatformContext` | Compass `Geolocator` |
| **Lifecycle** | `@AppScope` DI singleton (process-lifetime) | same |
| **Could be tested without the other?** | Yes | Yes |

Both are `@AppScope @Inject` classes provided by `AppComponent`, so
each has a single instance for the whole process. They are touched at
the top of `App()` — `val mapViewModel = component.mapViewModel` —
which forces kotlin-inject to construct them during the splash hold
and kicks their `init` blocks off in parallel: `MapViewModel` starts
its Firestore + favourites observers and the cover prefetch via
`RestaurantStore`; `MapControlViewModel` fires a best-effort first
location fix.

This used to be per-`NavBackStackEntry` `viewModel { ... }` scoping
when the app had a `NavHost`. With the move to a single-`MainShell`
overlay UI (no navigation graph), per-entry scoping no longer made
sense — and instantiating in composition meant we couldn't run
`init` ahead of first paint, so the map waited on a cold Firestore
fetch every cold launch. App-scope singletons fix both. If location
features grow (continuous tracking, follow mode, last-visited
bookmark), they grow inside `MapControlViewModel` without polluting
the restaurant pipeline.

### What stays in the composable

- `cameraPositionState = rememberCameraPositionState { ... }` —
  composable-bound to `GoogleMap`. Moving it to a VM would either drift
  out of sync with the live map (composition recreates a fresh state on
  config change while VM holds the old reference) or require a custom
  factory dance. Don't try.
- `mapLoaded`, `filterSheetOpen` — pure UI flags.
- `myLocationIcon` — uses `myLocationDotIcon()` (Android: BitmapDescriptor)
  which needs the maps SDK loaded.
- `descriptorCache` — Compose-to-bitmap rasterisation requires a
  composition context (Android does it via a hidden `ComposeView`),
  so the cache itself stays in composition even though its eviction
  logic is trivial.
- `LaunchedEffect`s that bridge `cameraPositionState` (composable-bound)
  to VM state (e.g., `viewModel.updateVisibleBounds(it)`).
- `cameraPositionState.animate(...)` calls in click handlers — animation
  command targets the live camera state.

## Marker rendering

### The library bug

`eu.buney.maps:kmp-maps-compose` provides `MarkerComposable { ... }`,
which under the hood calls `rememberComposeBitmapDescriptor` to render
arbitrary Compose content into a bitmap and use it as the marker icon.
The Android `actual` looks like:

```kotlin
// Library code (paraphrased)
val currentContent by rememberUpdatedState(content)
return remember(parent, compositionContext, currentContent, *keys) {
    renderToBitmap(content)
}
```

`currentContent` — the latest content lambda — is in the `remember`
keys. **Each recomposition produces a new lambda instance**, so
`remember` invalidates and the bitmap is re-rendered every recompose.
The `keys` parameter is effectively ignored.

Visible symptom: any time the GoogleMap content lambda recomposes
(camera reads, dense-set updates, etc.), every marker's bitmap is
re-rendered, and the marker briefly flickers because the Maps SDK has
to swap its icon. iOS doesn't have this bug.

### Our fix

`StableMarkerIcon.kt` (commonMain `expect` + `androidMain` `actual` +
`iosMain` `actual` that delegates to the library) provides
`rememberStableComposeBitmapDescriptor(*keys, content)` with **only
the user-supplied keys** in the `remember` slot. The content lambda is
captured via `rememberUpdatedState` so renders use the latest closure
values, but its identity doesn't invalidate the cache.

**Don't replace this with the library function.** It's literally the
load-bearing reason markers don't flicker.

### The three caches

```
                 ┌──────────────────────────────────────┐
                 │  Coil disk cache                     │  Survives app restarts
                 │  raw JPEG bytes per cover URL        │  Owned by Coil
                 └──────────────────┬───────────────────┘
                                    │
                                    ▼
                 ┌──────────────────────────────────────┐
                 │  MapViewModel.markerCovers           │  Survives the app process
                 │  SnapshotStateMap<id, MarkerCover>   │  Survives dot ↔ pill cycles
                 │  decoded coil3.Image per restaurant  │  Owned by the @AppScope VM
                 └──────────────────┬───────────────────┘
                                    │
                                    ▼
                 ┌──────────────────────────────────────┐
                 │  MarkerDescriptorCache               │  Survives dot ↔ pill cycles
                 │  Map<id, CachedMarkerDescriptor>     │  Owned by the composable (needs
                 │  rasterised pill BitmapDescriptor    │  ComposeView for rendering)
                 └──────────────────────────────────────┘
```

Each layer solves a different problem:

1. **Coil disk cache** — avoid network round-trip across sessions.
2. **`markerCovers` (VM)** — avoid Coil's load + decode pass when a
   restaurant flips between dot and pill on a zoom step. Loaded
   directly via `imageLoader.execute()`, not via `AsyncImagePainter`,
   so there's no composable-bound state machine and the cache lives
   wherever we want.
3. **`MarkerDescriptorCache`** — avoid the Compose-to-bitmap render
   (build off-screen `ComposeView`, measure, layout, draw to a
   `Bitmap`) on dot ↔ pill transitions. ~3–5 ms per marker,
   multiplied by however many restaurants flip variant on a zoom
   step.

`MiniRestaurantCard` wraps `coil3.Image.asPainter(ctx)` at rasterise
time — cheap struct adapter, no decoding, no async work.

### Marker emission gate

`RestaurantMarker` doesn't emit anything until the cover decision is
final:

```kotlin
if (cover == null) return // load still in flight
val coverReady = cover is MarkerCover.Loaded
```

This avoids a "placeholder pill → photo pill" re-rasterisation that
used to flash on first paint. Once the marker emits, the descriptor
cache's `coverReady` key matches; subsequent compositions are pure
cache hits.

## Dense detection (no clustering)

### Why no clusters

We tried numbered cluster badges and discarded them. The replacement:
**every restaurant gets its own marker, but markers whose pill would
overlap collapse to a small dot/heart/star**. The user always sees
individual points-of-interest — zooming in expands the dots back into
pills as room opens up. Trade-off accepted: extreme density (e.g., 20
restaurants in one block) shows 20 stacked dots instead of one "20+"
badge; "zoom in for detail" is the implicit instruction.

### Algorithm

`Clustering.kt` implements a screen-AABB grid overlap detector. (File
name is historical; there's no clustering anymore.)

1. Project every restaurant's lat/lng to screen pixels via the SDK's
   `Projection.toScreenLocation(...)`.
2. Estimate each pill's width from its restaurant (name length + tag
   row, capped at `MARKER_MAX_WIDTH`). Height is constant
   (`MARKER_BODY_HEIGHT`).
3. Bucket all anchors into a uniform grid of `maxOverlapPx`-side
   cells, where `maxOverlapPx = MARKER_MAX_WIDTH + paddingPx` is the
   worst-case centre-to-centre distance at which two pills can still
   overlap. (Two-pass: index everything before checking, since dense
   is pairwise — a single-pass match-against-existing approach would
   miss the case where i is checked before j but j ends up
   overlapping i.)
4. For each point, scan only the 9 cells in its 3×3 neighbourhood.
5. Predicate: AABB intersection on the (anchor 0.5, 1.0) box. Two
   pills are dense iff `|dx| < (Wa+Wb)/2 + padding` AND
   `|dy| < heightPx + padding`. Width-aware overlap (rather than a
   uniform circular radius) is what makes 短名 + 长名 pairs collapse
   when they actually visually overlap, not before.

Returns `Set<String>` of dense restaurant ids. Average-case O(n). For
our scale (~200 restaurants) the projection calls dominate.

### Why pan does nothing

Pan is a uniform screen translation. Pairwise screen distances between
any two restaurants are invariant under translation. So **the dense
set is invariant under pan**. Zoom is the only camera change that can
move a restaurant into or out of the dense set.

The `LaunchedEffect` that wires the pipeline subscribes only to zoom:

```kotlin
snapshotFlow {
    cameraPositionState.isMoving to cameraPositionState.position.zoom
}
    .filter { (isMoving, _) -> !isMoving }
    .map { (_, zoom) -> zoom }
    .distinctUntilChanged()
    .collect { ... }
```

We previously had a viewport pre-filter (only check restaurants within
visible bounds + 20% pad) to save projection calls. It caused
edge-marker flicker — restaurants crossing the pad boundary on every
pan got destroyed and recreated. Removed in favour of checking all
filtered restaurants every recompute. Cost: ~200 extra projection
calls per recompute. Imperceptible.

### Two-phase off-thread

`MapViewModel.recomputeDenseIds` is suspending:

```kotlin
suspend fun recomputeDenseIds(
    projection: Projection,
    widthEstimator: (Restaurant) -> Float,
    heightPx: Float,
    paddingPx: Float,
    maxOverlapPx: Float,
) {
    val current = restaurantsFiltered
    val projected = projectAll(current, projection)              // Main
    val widths = FloatArray(current.size) { widthEstimator(current[it]) }
    val next = withContext(Dispatchers.Default) {
        detectDenseRestaurants(current, projected, widths,        // Default
            heightPx, paddingPx, maxOverlapPx)
    }
    if (next != denseIds) {
        denseIds = next
    }
}
```

Projection is on Main because the Maps SDK isn't documented as
thread-safe. The grid math is pure and runs on `Dispatchers.Default`
so the UI thread isn't blocked.

The `next != denseIds` check short-circuits the state write when the
new dense set is structurally identical. Sets compare by membership,
so even if the underlying iteration order differs the comparison
returns true — without this check we'd cascade into marker
re-emissions on every zoom-idle.

## Location

`MapControlViewModel` owns user location via Compass:

```kotlin
private val geolocator: Geolocator = Geolocator.mobile()

init {
    viewModelScope.launch { ensureFreshLocation() }  // best-effort on VM birth
}

suspend fun ensureFreshLocation(): LocationOutcome {
    val cached = myLocation
    if (cached != null && isLocationFresh()) return LocationOutcome.Available(cached)
    isLocating = true
    try { ... return geolocator.current() ... } finally { isLocating = false }
}
```

- **`isLocating`** flips only on the slow path (cache miss + actual
  sensor call), so the FAB doesn't flash a spinner on rapid re-taps.
- **`isLocationFresh()`** compares `TimeSource.Monotonic.markNow()`
  against the last fetch. 60 s window — that's about 50 m of walking
  drift in the worst case, imperceptible at the FAB's animate-to zoom
  of 15.
- **`LocationOutcome`** collapses Compass's full sealed hierarchy to
  the three cases the UI branches on: `Available(coords)`,
  `PermissionDenied`, `Unavailable`.

Why Compass and not a hand-rolled `LocationRequester`: Compass handles
the runtime permission flow internally on Android (no
`rememberLauncherForActivityResult`, so no composable-bound code), and
gives us iOS for free with no platform-specific work in our codebase.

## Filter UX

The filter sheet (`FilterSheet.kt`) is a tabbed bottom sheet:

| Tab | Single-select? | What tapping does |
|-----|---------------|-------------------|
| Cuisine (regional) | One at a time | Filters the map; auto-dismisses |
| Style (format) | One at a time, independent of cuisine | Filters the map; auto-dismisses |
| Neighbourhood | N/A | Pans the camera to the district centroid; auto-dismisses |

Counts on each row factor in the *other* family's active selection
("if you tap this and you'd see N places"). Districts are sorted by
count desc so the densest neighbourhoods surface first.

The VM exposes `selectedCuisine: Tag?` and `selectedFormat: Tag?` (each
nullable). District is **not** a filter on the VM — the composable
handles the camera pan in its own coroutine scope.

## When you change something here

- **Touching `RestaurantMarker.kt` / `MiniRestaurantCard.kt`**:
  remember the bitmap is rasterised inside an off-screen `ComposeView`
  (Android) / detached `renderComposeScene` (iOS). Don't read
  composition locals that don't survive that context, and avoid
  expensive layouts — every change rasterises a fresh bitmap per
  restaurant. **In particular, the app locale does NOT reach inside the
  bitmap** — the off-screen view re-establishes its own platform
  `LocalConfiguration`, so `stringResource` / `tagDisplayName` /
  `Localizable.preferred(...)` called inside the content render the
  wrong language (the pill name + tags stayed Chinese in English mode).
  Resolve locale-dependent strings in the live map composition
  (`RestaurantMarker`) and pass them into `MiniRestaurantCard` as plain
  strings, and add them to the descriptor cache key + the
  `rememberStableComposeBitmapDescriptor` keys so a language switch
  re-rasterises. Same trick the walking-radius ring labels use.
- **Adding a new VM property**: decide if it belongs in
  `MapViewModel` (restaurant data) or `MapControlViewModel` (map
  control). If you find yourself touching both VMs from one feature,
  consider whether a third VM is appropriate before piling onto either.
- **Considering removing `MarkerDescriptorCache`**: don't, unless you've
  measured. We tried — the regression was visible (dot ↔ pill
  transitions cost ~3–5 ms × N markers per zoom step, which is enough
  to feel hitchy on big zoom changes).
- **Considering replacing `rememberStableComposeBitmapDescriptor` with
  the library function**: don't. The bug is in the library's Android
  impl; iOS is fine. If the library fixes it upstream, we can delete
  our wrapper. Until then, our wrapper is doing real work.
- **Adding a new pin colour / style**: `MarkerColors.kt` is the place
  for `expect`/`actual` BitmapDescriptors that don't go through the
  Compose-to-bitmap path (they're cheap pure SDK constructions).
