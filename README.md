# Berlin Chinese Food Map | 柏林中餐地图

[中文版本](README_ZH.md) | [Project Proposal](PROPOSAL.md)

A community-driven, non-profit digital guide to Chinese restaurants in Berlin. Built with Kotlin Multiplatform and Compose Multiplatform for Android and iOS.

**No login required. Privacy-first. Open source.**

**By [Novawerk](https://github.com/Novawerk)** — Open-source apps, made with care.

## Project Status

**POC Complete** — The proof of concept has been validated across all four pillars:

| Component | Status | What was validated |
|-----------|--------|--------------------|
| Mobile App (iOS + Android) | POC Ready | Cross-platform map & search, custom markers, Google Maps compliance |
| Data Pipeline (GitHub sync) | Automated | YAML → Firestore auto-sync & deployment |
| Landing Page | Live | Bilingual UI framework with feature showcase |
| Admin Panel (Control Center) | Running | Full CRUD management for restaurant data |

## Roadmap

| Phase | Description | Status |
|-------|-------------|--------|
| Phase 1: Kick-off | Data handoff, visual direction, schema & interaction design alignment | Upcoming |
| Phase 2: MVP Development (Week 1) | Polished map UI, restaurant profiles, trilingual search, favorites & visit tracking, internal beta | Upcoming |
| Phase 3: Beta & Launch Prep (Week 2) | Community beta (WeChat, Xiaohongshu), ASO prep, GTM coordination | Upcoming |
| Phase 4: Launch & Growth | App Store + Play Store submission, feedback loop, UGC pipeline, curated collections | Upcoming |

**Kick-off target: Week of April 14, 2026**

## Deliverables

- **Native Mobile App** (iOS + Android) — High-performance cross-platform app built with Kotlin Multiplatform
- **Landing Page** (https://berlinfoodmap.novawerk.io/) — SEO-optimized bilingual page for product showcase and app distribution
- **Control Center** — User-friendly admin panel designed for non-technical team members
- **Data Pipeline** — GitHub-based workflow with automated validation and sync for community-contributed content

## Features

### Dual View Modes

- **Map Mode** (default) — Interactive Google Map centered on Berlin with cuisine-colored restaurant pins. Tap a pin to preview, tap again for full details.
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

This is a multi-platform project with four components:

| Component | Tech | Location |
|-----------|------|----------|
| Mobile App | Kotlin Multiplatform + Compose | `composeApp/` |
| Landing Page | Next.js + Tailwind CSS | `web-apps/landing-page/` |
| Admin Panel | React + Vite | `web-apps/admin/` |
| Data Pipeline | YAML + GitHub CI → Firestore | `data/` |

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

### Data Pipeline

Restaurant data is community-contributed via YAML files:

```
data/restaurants/{district}/{restaurant-id}.yaml
```

| Field | Required | Example |
|-------|----------|---------|
| Name (ZH/EN) | Yes | 川味坊 / Sichuan Folk |
| Cuisine Type | Yes | Sichuan, Cantonese, Hotpot, BBQ, etc. |
| Street Address | Yes | Street name + number |
| GPS Coordinates | Recommended | Lat/Lng for precise map pins |
| Photos | Nice to have | 1-3 high-quality photos |

Data can be submitted via CSV, Excel, or Google Sheets. The dev team handles conversion to YAML. CI validates schema and syncs to Firestore on merge.

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
cd web-apps/landing-page && npm install && npm run dev
```

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
