# CLAUDE.md

## Project Overview

**Berlin Chinese Food Map (柏林中餐地图)** is a community-driven, non-profit digital guide to Chinese restaurants in Berlin. Built with Kotlin Multiplatform (KMP) targeting Android and iOS using Compose Multiplatform for shared UI.

**Current Status: POC Complete on Staging** — Cross-platform map, search, custom markers, data pipeline, landing page, and admin panel are all validated. The project is entering the Kick-off phase with community stakeholders who provide restaurant data and marketing channels.

## Deliverables

1. **Native Mobile App** (iOS + Android) — Kotlin Multiplatform + Compose
2. **Landing Page** (https://berlinfoodmap.novawerk.io/) — SEO-optimized bilingual page in `web-apps/landing-page/`
3. **Control Center** — Admin panel for non-technical team members in `web-apps/admin/`
4. **Data Pipeline** — YAML files in `data/restaurants/` → GitHub CI → Firestore auto-sync

## Roadmap

- **Phase 1 (Kick-off):** Data handoff from physical map, visual direction, schema & interaction alignment
- **Phase 2 (Week 1):** MVP core development — polished map, restaurant profiles, trilingual search, favorites, internal beta
- **Phase 3 (Week 2):** Community beta (WeChat, Xiaohongshu), ASO prep, GTM coordination
- **Phase 4:** App Store + Play Store launch, feedback loop, UGC mechanism, curated collections

## Tech Stack

- **Language**: Kotlin 2.2
- **UI**: Compose Multiplatform (Material Design 3 Expressive)
- **Navigation**: Compose Navigation (type-safe @Serializable routes)
- **Storage**: Jetpack DataStore
- **Backend**: Firebase (Anonymous Auth + Firestore)
- **Maps**: Google Maps SDK (Android) / MapKit (iOS)
- **DI**: kotlin-inject with KSP
- **Networking**: Ktor Client
- **Date/Time**: kotlinx-datetime
- **Build**: Gradle with version catalog (`gradle/libs.versions.toml`)
- **Android**: Min SDK 24, Compile/Target SDK 36
- **iOS**: Swift/SwiftUI wrapper in `iosApp/`, KMP framework named `ComposeApp`

## Project Structure

```
composeApp/src/
├── commonMain/kotlin/com/novawerk/berlinfoodmap/
│   ├── App.kt                      # Root Composable + NavHost
│   ├── Platform.kt                  # expect declarations
│   ├── di/                          # DI root
│   ├── domain/
│   │   ├── restaurant/              # Restaurant models, repository
│   │   ├── favorites/               # Favorites repository
│   │   └── search/                  # Search/filter logic
│   ├── data/
│   │   ├── local/                   # DataStore (favorites, settings)
│   │   └── remote/                  # Firebase Auth + Firestore
│   └── ui/                          # All screens, components, theme, navigation
├── androidMain/                     # MainActivity, Platform.android.kt
└── iosMain/                         # MainViewController, Platform.ios.kt
iosApp/                              # Native Xcode project
web-apps/
├── landing-page/                    # Landing page (Next.js, bilingual)
└── admin/                           # Admin panel (React + Vite)
data/restaurants/                    # YAML restaurant data files
```

## Build Commands

```bash
./gradlew :composeApp:assembleDebug        # Build debug APK
./gradlew :composeApp:installDebug         # Install on device/emulator
./gradlew clean build                      # Full clean build
```

**Do not build iOS locally** — only verify Android and shared logic.

## Development Guidelines

- **Code style**: Kotlin official style (`kotlin.code.style=official`)
- **Localization**: All UI strings in `composeResources/values/strings.xml` (EN) and `values-zh/strings.xml` (ZH). Access via `stringResource(Res.string.*)`. No hardcoded text.
- **Theming**: Use `MaterialTheme.colorScheme.*` everywhere — never hardcode colors in UI files.
- **DI**: kotlin-inject with `@KmpComponentCreate`.
- **Platform differences**: Use `expect`/`actual` pattern via `Platform.kt`
- **Navigation**: Type-safe routes with `@Serializable data object` in `ui/navigation/Routes.kt`
- **Date formatting**: Use `kotlinx-datetime-names` for localized date/weekday names. Never hardcode date formats.

## Architecture

- Single module (`:composeApp`)
- Domain layer: models + repository interfaces
- Data layer: local DB + remote API implementations
- UI: Compose screens with ViewModels
- Privacy-first: local storage preferred, no unnecessary cloud sync

## Key Business Rules

1. Restaurant data is community-contributed (YAML files → Firestore via CI pipeline)
2. Data can be submitted via CSV/Excel/Google Sheets — dev team converts to YAML
3. Favorites stored locally via DataStore (privacy-first, no cloud sync)
4. Visit tracking synced to Firebase with anonymous auth
5. Map pins colored by cuisine type (Sichuan, Cantonese, Hotpot, BBQ, Dim Sum, Noodles, General, Other)
6. Search supports Chinese, English, and German restaurant names
7. Dual language UI (EN/ZH), German for restaurant names
8. Offline-capable for saved/cached data
9. Non-profit, community-driven project — no ads, no tracking
10. Admin panel designed for non-technical team members

## Collaboration Model

- **Weekly sync** (30min) during development with dev/product/design/beta testers
- **Pre-launch review** (30min-1h) with all stakeholders before release
- **Monthly ops review** after launch for user feedback, data updates, system stability
- **Async**: WeChat group for urgent items, GitHub Issues for bugs and feature requests
