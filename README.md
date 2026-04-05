# Berlin Chinese Food Map | 柏林中餐地图

[中文版本](README_ZH.md)

An open-source, community-driven guide to Chinese restaurants in Berlin. Built with Kotlin Multiplatform and Compose Multiplatform for Android and iOS.

**No login required. Pure browsing experience.**

**By [Novawerk](https://github.com/Novawerk)** — Open-source apps, made with care.

## Features

### Dual View Modes

- **Map Mode** (default) — Interactive Google Map centered on Berlin with restaurant pins. Tap a pin to preview, tap again for full details.
- **List Mode** — Scrollable restaurant cards sorted by visit count or name, with cuisine-type chip filters.

### Smart Search & Filtering

- **Full-text Search** — Search by Chinese, English, or German restaurant names with 300ms debounced input
- **Cuisine Type** — Sichuan, Cantonese, Hotpot, BBQ, Dim Sum, Noodles, General, Other
- **District** — Filter by Berlin district (Mitte, Charlottenburg, Neukölln, etc.)
- **Combined Filters** — Stack multiple filter conditions for precise results

### Restaurant Details

- **Basic Info** — Address, phone, price range
- **Photo Gallery** — Swipeable image gallery with page indicators
- **Visit & View Stats** — Community-driven visit count and view tracking
- **Cuisine Tags** — Cuisine type classification

### Favorites & Visit Tracking

- **Favorites** — Save restaurants locally via DataStore for quick access
- **Visit Marking** — Mark restaurants as visited, synced to Firebase
- **Personal Food Diary** — Track your Chinese food journey across Berlin

### More

- **Offline Support** — Browse cached restaurants without internet
- **Bilingual** — Full English and Chinese (simplified) UI, with German restaurant name support
- **Dark Mode** — System default, light, and dark theme options
- **Privacy-First** — Anonymous auth only, no tracking, no ads, open source

## Project Structure

This is a multi-platform project with three components:

| Component | Tech | Location |
|-----------|------|----------|
| Mobile App | Kotlin Multiplatform + Compose | `composeApp/` |
| Landing Page | Next.js 16 + Tailwind CSS 4 | `web/` |
| Admin Panel | React + Vite | `admin/` |

### App Architecture

Single module (`:composeApp`) with clean separation:

```
composeApp/src/commonMain/kotlin/com/novawerk/berlinfoodmap/
├── App.kt                          # NavHost + startup logic
├── domain/
│   ├── restaurant/                  # Restaurant models, repository interface
│   ├── favorites/                   # Favorites repository
│   └── search/                      # Search/filter logic
├── data/
│   ├── local/                       # DataStore (favorites, settings)
│   └── remote/                      # Firebase Auth + Firestore
├── di/                              # kotlin-inject AppComponent
└── ui/
    ├── theme/                       # Material Design 3 (Expressive)
    ├── navigation/                  # Type-safe @Serializable routes
    ├── components/                  # RestaurantCard, CuisineChips, EmptyState
    └── pages/
        ├── map/                     # Map view with restaurant pins
        ├── list/                    # Restaurant list view
        ├── detail/                  # Restaurant detail + gallery
        ├── search/                  # Search & multi-filter
        ├── favorites/               # Saved restaurants
        └── settings/                # Theme, language, about
```

## Tech Stack

| Layer | Technology |
|-------|-----------|
| Language | Kotlin 2.2 |
| UI | Compose Multiplatform (Material Design 3 Expressive) |
| Navigation | Compose Navigation (type-safe `@Serializable` routes) |
| Backend | Firebase (Anonymous Auth + Firestore) |
| Local Storage | Jetpack DataStore |
| Maps | Google Maps SDK (Android) / MapKit (iOS) |
| DI | kotlin-inject (KSP) |
| Networking | Ktor Client |
| Date/Time | kotlinx-datetime |
| Targets | Android (SDK 24+) / iOS |
| Build | Gradle with version catalog |

## Quick Start

```bash
# Prerequisites: JDK 17+, Android SDK

# Build Android
./gradlew :composeApp:assembleDebug

# Install on device/emulator
./gradlew :composeApp:installDebug
```

For iOS, open `iosApp/` in Xcode and build normally.

For the landing page:

```bash
cd web && npm install && npm run dev
```

## Data Sources

Restaurant data is community-contributed via JSON files in `data/restaurants/` and synced to Firebase Firestore. If you know a great Chinese restaurant in Berlin, please open an issue or submit a PR!

## Contributing

We welcome contributions! Whether it's adding a restaurant, fixing a bug, or improving the UI — every bit helps.

1. Fork the repo
2. Create your feature branch (`git checkout -b feature/amazing-restaurant`)
3. Commit your changes (we use [conventional commits](https://www.conventionalcommits.org/))
4. Push to the branch
5. Open a Pull Request

For iOS development, copy `iosApp/Configuration/Config.xcconfig.template` to `Config.xcconfig` and fill in your Team ID.

## License

This project is licensed under the [MIT License](LICENSE).

Copyright (c) 2025-2026 [Novawerk](https://github.com/Novawerk). You are free to use, modify, and distribute this software, as long as the original copyright notice is included.
