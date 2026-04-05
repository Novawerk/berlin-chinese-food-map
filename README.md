# Berlin Chinese Food Map | 柏林中餐地图

[中文版本](README_ZH.md)

An open-source, community-driven guide to Chinese restaurants in Berlin. Built with Kotlin Multiplatform and Compose Multiplatform for Android and iOS.

**No login required. Pure browsing experience.**

**By [Novawerk](https://github.com/Novawerk)** — Open-source apps, made with care.

## Features

### Dual View Modes

- **Map Mode** (default) — Interactive map with restaurant pins colored by cuisine type. Tap a pin to preview, tap again for full details.
- **List Mode** — Scrollable restaurant cards sorted by distance, rating, or name. Switch seamlessly between map and list.

### Smart Filtering

Filter restaurants by any combination of:

| Filter | Description |
|--------|-------------|
| Name | Search by restaurant name (Chinese & English) |
| Cuisine Type | Sichuan, Cantonese, Hotpot, BBQ, Dim Sum, Noodles, etc. |
| District | Mitte, Charlottenburg, Prenzlauer Berg, Neukölln, etc. |
| Distance | Sort or filter by proximity to your current location |

### Restaurant Details

- **Basic Info** — Address, phone, opening hours, price range
- **Cuisine & Tags** — Cuisine style, dietary options, specialties
- **Dish Menu** — Featured dishes with photos, descriptions, and prices
- **Photos** — Restaurant interior, exterior, and food photos
- **Community Reviews** — Ratings and reviews from the Berlin Chinese community

### More

- **Favorites** — Save restaurants for quick access (stored locally)
- **Offline Support** — Browse cached restaurants without internet
- **Bilingual** — Full English and Chinese (简体中文) support
- **Privacy-First** — No login, no tracking, no ads, open source

## Tech Stack

| Layer | Technology |
|-------|-----------|
| Language | Kotlin 2.2 |
| UI | Compose Multiplatform (Material Design 3) |
| Navigation | Compose Navigation (type-safe routes) |
| Storage | Jetpack DataStore / Room |
| Maps | Google Maps SDK / MapKit |
| DI | kotlin-inject (KSP) |
| Networking | Ktor Client |
| Targets | Android / iOS |
| Build | Gradle with version catalog |

## Architecture

Single module (`:composeApp`) with clean separation:

```
composeApp/src/commonMain/kotlin/com/novawerk/berlinfoodmap/
├── App.kt                          # NavHost + startup logic
├── domain/
│   ├── restaurant/                  # Restaurant & Dish models, repository
│   ├── review/                      # Review models
│   └── search/                      # Search/filter logic
├── data/
│   ├── local/                       # Local database, favorites
│   └── remote/                      # API client
└── ui/
    ├── theme/                       # M3 theme
    ├── navigation/                  # Type-safe routes
    ├── components/                  # Shared composables (filter bar, cards)
    └── pages/
        ├── map/                     # Map view with restaurant pins
        ├── list/                    # Restaurant list view
        ├── detail/                  # Restaurant detail + dish menu
        ├── search/                  # Search & filter
        ├── favorites/               # Saved restaurants
        └── settings/                # Language, about
```

## Quick Start

```bash
# Prerequisites: JDK 17+, Android SDK

# Build Android
./gradlew :composeApp:assembleDebug

# Install on device/emulator
./gradlew :composeApp:installDebug
```

For iOS, open `iosApp/` in Xcode and build normally.

## Data Sources

Restaurant data is community-contributed. If you know a great Chinese restaurant in Berlin, please open an issue or submit a PR!

## Contributing

We welcome contributions! Whether it's adding a restaurant, fixing a bug, or improving the UI — every bit helps.

1. Fork the repo
2. Create your feature branch (`git checkout -b feature/amazing-restaurant`)
3. Commit your changes
4. Push to the branch
5. Open a Pull Request

For iOS development, copy `iosApp/Configuration/Config.xcconfig.template` to `Config.xcconfig` and fill in your Team ID.

## License

This project is licensed under the [MIT License](LICENSE).

Copyright (c) 2025-2026 [Novawerk](https://github.com/Novawerk). You are free to use, modify, and distribute this software, as long as the original copyright notice is included.
