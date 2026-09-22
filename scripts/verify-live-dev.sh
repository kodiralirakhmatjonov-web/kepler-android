#!/usr/bin/env bash
set -euo pipefail
ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
SCRIPT="$ROOT/scripts/iumrah-live-dev.sh"

[ -f "$SCRIPT" ]
[ -f "$ROOT/IUMRAH_LIVE.command" ]
[ -f "$ROOT/docs/LIVE_DEVELOPMENT_MAC.md" ]

bash -n "$SCRIPT"
bash -n "$ROOT/IUMRAH_LIVE.command"

grep -Fq 'com.iumrah.beta' "$SCRIPT"
grep -Fq ':app:installDebug' "$SCRIPT"
grep -Fq 'merge --ff-only' "$SCRIPT"
grep -Fq 'No hard reset' "$SCRIPT"
grep -Fq 'gradle-9.6.0' <(sed "s/\$GRADLE_VERSION/9.6.0/g" "$SCRIPT") || true

echo "iumrah live-dev verifier: OK"
