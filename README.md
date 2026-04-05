# Berlin Chinese Food Map | 柏林中餐地图

[中文版本](README_ZH.md)

An open-source, community-driven guide to Chinese restaurants in Berlin. Built with Kotlin Multiplatform and Compose Multiplatform for Android and iOS.

**By [Novawerk](https://github.com/Novawerk)** — Open-source apps, made with care.

## Features

- **Restaurant Map** — Interactive map showing Chinese restaurants across Berlin
- **Restaurant Profiles** — Detailed info: cuisine type, price range, hours, photos, and reviews
- **Community Reviews** — Ratings and reviews from the Chinese community in Berlin
- **Search & Filter** — Find by cuisine style (Sichuan, Cantonese, Hotpot, BBQ, etc.), district, price range
- **Favorites** — Save your go-to spots for quick access
- **Offline Support** — Browse saved restaurants without internet
- **Bilingual** — Full English and Chinese (简体中文) support
- **Privacy-First** — No tracking, no ads, open source

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
│   ├── restaurant/                  # Restaurant models, repository
│   ├── review/                      # Review models
│   └── search/                      # Search/filter logic
├── data/
│   ├── local/                       # Local database, favorites
│   └── remote/                      # API client (if applicable)
└── ui/
    ├── theme/                       # M3 theme
    ├── navigation/                  # Type-safe routes
    ├── components/                  # Shared composables
    └── pages/
        ├── map/                     # Map view with restaurant pins
        ├── list/                    # Restaurant list view
        ├── detail/                  # Restaurant detail page
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
