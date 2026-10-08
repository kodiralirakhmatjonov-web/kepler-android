#!/usr/bin/env bash
set -euo pipefail
ROOT="$(cd "$(dirname "$0")/.." && pwd)"
cd "$ROOT"
export IUMRAH_VERIFY_LATEST=1
bash scripts/verify-stage19.sh

S16=scripts/verify-stage16.sh
S10=scripts/verify-stage10.sh
for f in "$S16" "$S10" scripts/build-play-release.sh scripts/verify-16kb-native-libs.sh; do
  test -s "$f" || { echo "STOP: required release script missing or empty: $f"; exit 1; }
done
if grep -Fq 'test -x scripts/build-play-release.sh' "$S16"; then
  echo 'STOP: release verifier still depends on ZIP-unstable execute bits'; exit 1
fi
grep -Fq 'test -s scripts/build-play-release.sh' "$S16" || { echo 'STOP: permission-neutral AAB builder gate missing'; exit 1; }
grep -Fq -- '-s scripts/verify-stage20.sh' "$S10" || { echo 'STOP: Stage 10 bridge is not permission-neutral/latest'; exit 1; }
echo 'Stage 020 ZIP-safe release-script permission checks passed.'
