#!/usr/bin/env bash
set -euo pipefail

bash scripts/verify-stage8.sh

root="$(cd "$(dirname "$0")/.." && pwd)"
cd "$root"

required=(
  "ANDROID_STAGE_009_GALAXY_UI_APPLIED.txt"
  "docs/IUMRAH_GALAXY_UI_STANDARD.md"
  "parity/STAGE_009_GALAXY_UI.md"
  "app/src/main/java/com/iumrah/beta/core/design/IumrahTheme.kt"
  "app/src/main/java/com/iumrah/beta/core/design/IumrahMotion.kt"
  "app/src/main/java/com/iumrah/beta/ui/components/IumrahComponents.kt"
  "app/src/main/java/com/iumrah/beta/ui/components/IumrahRootPageHeader.kt"
  "app/src/main/java/com/iumrah/beta/ui/trip/TripBuilderScreen.kt"
)

for file in "${required[@]}"; do
  test -f "$file" || { echo "Missing Stage 009 file: $file" >&2; exit 1; }
done

grep -q 'ScreenHorizontal = 24.dp' app/src/main/java/com/iumrah/beta/core/design/IumrahTheme.kt
grep -q 'CubicBezierEasing(0.22f, 0.25f, 0.00f, 1.00f)' app/src/main/java/com/iumrah/beta/core/design/IumrahMotion.kt
grep -q 'statusBarsPadding()' app/src/main/java/com/iumrah/beta/ui/trip/TripBuilderScreen.kt
grep -q 'navigationBarsPadding()' app/src/main/java/com/iumrah/beta/ui/trip/TripBuilderScreen.kt
grep -q 'PackageTier.entries.chunked(2)' app/src/main/java/com/iumrah/beta/ui/trip/TripBuilderScreen.kt
grep -q 'https://iumrah.app' README.md

echo "Stage 009 Galaxy UI structural verification passed."
