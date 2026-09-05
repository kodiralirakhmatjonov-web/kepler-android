#!/usr/bin/env bash
set -euo pipefail

test -s app/build.gradle.kts

grep -Eq 'compileSdk[[:space:]]*=[[:space:]]*36' app/build.gradle.kts || {
  echo 'STOP: compileSdk must be 36 (latest stable Android SDK platform).'
  exit 1
}

grep -Eq 'targetSdk[[:space:]]*=[[:space:]]*36' app/build.gradle.kts || {
  echo 'STOP: targetSdk must be 36 for the current stable Android 16 target.'
  exit 1
}

grep -Eq 'versionCode[[:space:]]*=[[:space:]]*9' app/build.gradle.kts || {
  echo 'STOP: expected build-hotfix versionCode 9.'
  exit 1
}

echo 'Android build environment checks passed: compileSdk=36 targetSdk=36.'
