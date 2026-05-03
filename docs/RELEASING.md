# Releasing

End-to-end release pipeline:

```
                  every push to main
gradle/libs.versions.toml             ┌──────────────────┐
   app-versionCode = N      ──────►   │ bump-version.yml │ ──► commit "Bump version code [skip ci]"
                                      └──────────────────┘
                                        + sync iOS xcconfig

                  PR merge to release   (PR carries one of: version:patch / version:minor / version:major)
gradle/libs.versions.toml             ┌──────────────┐
   app-versionName = X.Y.Z   ──────►  │  release.yml │ ──► commit "chore: bump version to X.Y.Z"
                                      └──────────────┘     ──► tag vX.Y.Z + GitHub Release
                                                           ──► dispatch google-play-release.yml
                                                           ──► open backport PR to main

                  tag vX.Y.Z (or manual dispatch)
                                      ┌──────────────────────────┐
                                      │ google-play-release.yml  │ ──► signed AAB
                                      └──────────────────────────┘     ──► uploaded as draft to Play track
```

**Source of truth for both Android and iOS** is `gradle/libs.versions.toml`.
Nothing else should be edited by hand for versioning.

## Branches

- `main` — active development. Every push auto-bumps `app-versionCode`.
- `release` — only release-cuts and the auto-generated version-bump commits live here. Direct pushes are discouraged; merge PRs from `main`.

To create the `release` branch the first time:

```bash
git checkout main && git pull
git checkout -b release
git push -u origin release
```

Set the branch protection rules you want in repo Settings → Branches.

## How to ship a release

1. **Open a PR from `main` → `release`** with whatever you want to ship.
2. **Add exactly one PR label**:
   - `version:patch` — bug fixes only (1.0.0 → 1.0.1)
   - `version:minor` — new features, no breaking changes (1.0.0 → 1.1.0)
   - `version:major` — breaking changes (1.0.0 → 2.0.0)
   - Create these labels in repo Settings → Labels if they don't exist yet.
3. **Merge the PR.** `release.yml` fires on the merge commit:
   - reads the label, bumps `app-versionName` accordingly
   - commits the bump to `release`
   - tags `vX.Y.Z` and creates a GitHub Release with categorised notes
   - dispatches `google-play-release.yml` for the new tag
   - opens a backport PR to sync the version-name change back to `main`
4. **Merge the backport PR** so `main` and `release` agree on `versionName` again.

To force a release manually (e.g., bypass labels), use **Actions → Release → Run workflow** and pick `bump`.

## Required GitHub secrets (one-time)

Set in repo Settings → Secrets and variables → Actions:

| Secret | How to obtain |
|---|---|
| `KEYSTORE_BASE64` | `base64 -i berlinfoodmap-release.jks \| pbcopy` |
| `KEYSTORE_PASSWORD` | The password you set when generating the keystore |
| `KEY_ALIAS` | The alias inside the keystore |
| `KEY_PASSWORD` | The key password (often = `KEYSTORE_PASSWORD`) |
| `GOOGLE_PLAY_SERVICE_ACCOUNT_JSON` | `base64 -i play-service-account.json \| pbcopy` |

## One-time Play Console setup

1. **Create the app listing** at https://play.google.com/console (name, language, app/game, free/paid). Complete the store listing, content rating, and data safety form.
2. **Manually upload the first AAB** so Play Console knows the package name and signing certificate fingerprint. Build it locally:
   ```bash
   KEYSTORE_PATH=/abs/path/to/berlinfoodmap-release.jks \
   KEYSTORE_PASSWORD=… KEY_ALIAS=… KEY_PASSWORD=… \
     ./gradlew :composeApp:bundleRelease
   ```
   Then in Play Console → Internal testing → Create new release → upload the AAB from `composeApp/build/outputs/bundle/release/`. Roll out to internal testing.
3. **Set up API access** in Play Console → Setup → API access:
   - Link a Google Cloud project
   - Create a service account, grant "Editor" role in Cloud Console
   - Back in Play Console, grant the SA "Release manager" permission
   - Download the JSON key, base64-encode, set as `GOOGLE_PLAY_SERVICE_ACCOUNT_JSON` above

After the first AAB is on Play and the secrets are in GitHub, every release tag auto-uploads.

## What lives where

| File | Role |
|---|---|
| `gradle/libs.versions.toml` | Source of truth for `app-versionName` + `app-versionCode` |
| `composeApp/build.gradle.kts` | Reads catalog → Android manifest + BuildKonfig + `syncVersionToIos` task |
| `iosApp/Configuration/Config.xcconfig` | iOS reads `MARKETING_VERSION` + `CURRENT_PROJECT_VERSION` from here (auto-synced by CI) |
| `.github/workflows/bump-version.yml` | versionCode +1 on every push to main |
| `.github/workflows/release.yml` | semver bump + tag + GitHub Release on merge to release |
| `.github/workflows/google-play-release.yml` | Signed AAB → Play Store on `v*` tag |
| `docs/RELEASING.md` | This document |

## Why versionCode and versionName are decoupled

- **versionCode** is monotonic and Play Store rejects re-uploads with the same code. Auto-incrementing on every main push means a freshly-cut release always has a fresh code, no manual coordination needed.
- **versionName** is human-meaningful (`1.0.0`). It changes only at release boundaries, driven by the PR label, so it doesn't churn on every commit.

## Tags

The tag format is `vX.Y.Z` (lowercase v, plain semver). Adding `-prod` (`vX.Y.Z-prod`) routes the upload straight to the production track instead of internal testing — useful only if you're confident enough to skip the staged rollout.
