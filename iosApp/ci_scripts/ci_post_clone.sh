#!/bin/sh
set -e

# Xcode Cloud post-clone script.
#
# We ONLY install the JDK here. The actual KMP framework build happens
# inside Xcode's "Compile Kotlin Framework" Run Script build phase —
# that phase has the full Xcode build environment ($ARCHS, $CONFIGURATION,
# $PLATFORM_NAME, $SDK_NAME) which Compose Multiplatform's
# syncComposeResourcesForIos task requires. If we tried to run
# embedAndSignAppleFrameworkForXcode here, we'd fail with:
#     Could not infer iOS target architectures
# because those env vars aren't set until xcodebuild starts.
#
# Note: We use `brew install openjdk@17` (not `--cask temurin@17`) because
# the cask path writes to /usr/local/Caskroom which needs sudo, and Xcode
# Cloud runs scripts without a TTY or passwordless sudo. The regular
# `openjdk@17` formula installs into Homebrew's own writable prefix.

echo ">>> Installing OpenJDK 17 via Homebrew..."
brew install openjdk@17

JAVA_HOME="$(brew --prefix openjdk@17)/libexec/openjdk.jdk/Contents/Home"
echo "JAVA_HOME resolved to: $JAVA_HOME"
"$JAVA_HOME/bin/java" -version

echo ">>> JDK ready. The Xcode 'Compile Kotlin Framework' Run Script phase"
echo ">>> will handle the KMP build using this JDK."

# Seed Configuration/Config.xcconfig if absent. The file is gitignored
# (it carries the Maps API key), so Xcode Cloud's clean checkout doesn't
# have it. Without it PRODUCT_NAME is empty and the archive fails with
# "Multiple commands produce '.app'". We copy the template, then
# overwrite the secret/version fields from Xcode Cloud env + the Gradle
# version catalog (the single source of truth for app version).
REPO_ROOT="${CI_PRIMARY_REPOSITORY_PATH:-$(cd "$(dirname "$0")/../.." && pwd)}"
CONFIG_DIR="$REPO_ROOT/iosApp/Configuration"
CONFIG_FILE="$CONFIG_DIR/Config.xcconfig"
TEMPLATE_FILE="$CONFIG_DIR/Config.xcconfig.template"

if [ ! -f "$CONFIG_FILE" ]; then
  echo ">>> Seeding Config.xcconfig from template..."
  cp "$TEMPLATE_FILE" "$CONFIG_FILE"

  # MAPS_API_KEY must be set in App Store Connect → Xcode Cloud → Workflow
  # → Environment Variables (mark as Secret). Without it the map shows the
  # Google "for development purposes only" watermark, but the build still
  # succeeds — so we don't hard-fail here.
  if [ -n "${MAPS_API_KEY:-}" ]; then
    echo ">>> Injecting MAPS_API_KEY from env."
    sed -i '' "s|^MAPS_API_KEY=.*|MAPS_API_KEY=$MAPS_API_KEY|" "$CONFIG_FILE"
  else
    echo ">>> WARNING: MAPS_API_KEY env var not set; map will show watermark."
  fi

  # Pull versionCode + versionName from gradle/libs.versions.toml so the
  # iOS build matches whatever the Android side advertises. Lines look like:
  #   app-versionCode = "15"
  #   app-versionName = "1.0.0"
  TOML="$REPO_ROOT/gradle/libs.versions.toml"
  if [ -f "$TOML" ]; then
    VERSION_CODE=$(grep '^app-versionCode' "$TOML" | head -n1 | sed 's/.*= *"\([^"]*\)".*/\1/')
    VERSION_NAME=$(grep '^app-versionName' "$TOML" | head -n1 | sed 's/.*= *"\([^"]*\)".*/\1/')
    if [ -n "$VERSION_CODE" ]; then
      echo ">>> CURRENT_PROJECT_VERSION = $VERSION_CODE (from libs.versions.toml)"
      sed -i '' "s|^CURRENT_PROJECT_VERSION=.*|CURRENT_PROJECT_VERSION=$VERSION_CODE|" "$CONFIG_FILE"
    fi
    if [ -n "$VERSION_NAME" ]; then
      echo ">>> MARKETING_VERSION = $VERSION_NAME (from libs.versions.toml)"
      sed -i '' "s|^MARKETING_VERSION=.*|MARKETING_VERSION=$VERSION_NAME|" "$CONFIG_FILE"
    fi
  fi

  echo ">>> Config.xcconfig ready:"
  # Echo non-secret lines for sanity. Skip the MAPS_API_KEY line.
  grep -v '^MAPS_API_KEY' "$CONFIG_FILE" || true
fi
