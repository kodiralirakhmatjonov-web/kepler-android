#!/usr/bin/env bash
set -euo pipefail
ROOT="$(cd "$(dirname "$0")/.." && pwd)"
cd "$ROOT"

# These files are inert stubs left by an older migration. Remove the files
# themselves so the production repository contains no legacy Billing package.
legacy_files=(
  app/src/main/java/com/iumrah/beta/core/billing/LegacyPlayCard.kt
  app/src/main/java/com/iumrah/beta/core/billing/LegacyPlayPurchases.kt
  docs/google-play/legacy/plus_page.dart.txt
  docs/google-play/legacy/premium_service.dart.txt
)
for path in "${legacy_files[@]}"; do
  if [[ -e "$path" ]]; then
    rm -f "$path"
    echo "Removed legacy Play Billing artifact: $path"
  fi
done

rmdir app/src/main/java/com/iumrah/beta/core/billing 2>/dev/null || true
rmdir docs/google-play/legacy 2>/dev/null || true
