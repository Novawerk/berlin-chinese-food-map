#!/usr/bin/env bash
#
# Guard against stray backslash escapes in Compose Resources string files.
#
# Compose Multiplatform's resource parser does NOT unescape \' / \" the way
# Android's aapt does — it renders the backslash *literally* in the UI. So a
# string written as "we\'ve" shows up on screen as  we\'ve , slash and all.
# Plain apostrophes and quotes are correct and preferred (e.g. don't, "x").
#
# The ONLY backslash escape these UI strings legitimately use is \n (paragraph
# breaks), so this lint flags every backslash that is not followed by `n`.
# That catches the whole class — \', \", a typo'd \t, etc. — not just the one
# bug we already fixed. If you ever need another genuine escape, add it to the
# allowed set in the pattern below and document why.
#
#   Run locally:   scripts/check-string-escapes.sh
#   CI:            wired into .github/workflows/android-build.yml (fails the
#                  build before the slow assembleRelease step).
#
set -euo pipefail

cd "$(dirname "$0")/.."

files=$(git ls-files 'composeApp/src/commonMain/composeResources/values*/strings.xml')

if [ -z "$files" ]; then
  echo "✘ check-string-escapes: no strings.xml files found — wrong working dir?" >&2
  exit 2
fi

# A backslash followed by anything other than `n`. (\n is the only allowed
# escape; everything else is rendered with a visible backslash by Compose.)
if matches=$(grep -nE '\\[^n]' $files); then
  echo "✘ Stray backslash escape(s) in Compose Resources strings." >&2
  echo "  Compose renders these literally (e.g. \\' shows up as  \\'  on screen)." >&2
  echo "  Use a plain character instead; only \\n is allowed (paragraph break)." >&2
  echo >&2
  echo "$matches" >&2
  exit 1
fi

echo "OK — no stray backslash escapes in string resources."
