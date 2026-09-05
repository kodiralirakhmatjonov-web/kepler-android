#!/usr/bin/env bash
set -euo pipefail

test -s app/build.gradle.kts

grep -Eq 'compileSdk[[:space:]]*=[[:space:]]*36' app/build.gradle.kts || {
  echo 'STOP: compileSdk must be 36 (stable Android SDK platform).'
  exit 1
}

grep -Eq 'targetSdk[[:space:]]*=[[:space:]]*36' app/build.gradle.kts || {
  echo 'STOP: targetSdk must be 36 for the current stable Android target.'
  exit 1
}

# Do not pin a specific versionCode here. Release/update stages are allowed to
# increment it. We only require a valid positive integer.
version_code="$(sed -nE 's/^[[:space:]]*versionCode[[:space:]]*=[[:space:]]*([0-9]+).*/\1/p' app/build.gradle.kts | head -n1)"
if [[ -z "$version_code" || "$version_code" -lt 1 ]]; then
  echo 'STOP: app versionCode must be a positive integer.'
  exit 1
fi

echo "Android build environment checks passed: compileSdk=36 targetSdk=36 versionCode=$version_code."
