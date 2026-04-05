# CLAUDE.md

## Project Overview

**Berlin Chinese Food Map (柏林中餐地图)** is a Kotlin Multiplatform (KMP) community-driven Chinese restaurant guide for Berlin, targeting Android and iOS using Compose Multiplatform for shared UI.

## Tech Stack

- **Language**: Kotlin 2.2
- **UI**: Compose Multiplatform (Material Design 3)
- **Navigation**: Compose Navigation (type-safe @Serializable routes)
- **Storage**: Jetpack DataStore + Room (local DB)
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
│   │   ├── review/                  # Review models
│   │   └── search/                  # Search/filter logic
│   ├── data/
│   │   ├── local/                   # Local DB, favorites
│   │   └── remote/                  # API client
│   └── ui/                          # All screens, components, theme, navigation
├── androidMain/                     # MainActivity, Platform.android.kt
└── iosMain/                         # MainViewController, Platform.ios.kt
iosApp/                              # Native Xcode project
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

1. Restaurant data is community-contributed (JSON/local DB)
2. Reviews and ratings stored locally or via community API
3. Favorites stored locally via DataStore
4. Map pins colored by cuisine type
5. Search supports both Chinese and English restaurant names
6. Dual language (EN/ZH)
7. Offline-capable for saved/cached data
