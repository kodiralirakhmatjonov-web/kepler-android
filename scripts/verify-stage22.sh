#!/usr/bin/env bash
set -euo pipefail
ROOT="$(cd "$(dirname "$0")/.." && pwd)"
cd "$ROOT"
export IUMRAH_VERIFY_LATEST=1
bash scripts/verify-stage21.sh

ACCOUNT=app/src/main/java/com/iumrah/beta/ui/account/AccountScreens.kt
SECURITY=app/src/main/java/com/iumrah/beta/ui/account/AccountSecurityParity.kt
BUILDER=scripts/build-play-release.sh
REPORTER=scripts/print-all-release-lint-errors.py

grep -Fq 'import androidx.activity.compose.LocalActivity' "$ACCOUNT" || { echo 'STOP: Account Login native Activity ambient missing'; exit 1; }
grep -Fq 'val activity = LocalActivity.current' "$ACCOUNT" || { echo 'STOP: Account Login still uses unsafe LocalContext-to-Activity cast'; exit 1; }
if grep -Eq 'LocalContext[.]current[[:space:]]+as[?]?[[:space:]]+Activity' "$ACCOUNT"; then
  echo 'STOP: Compose ContextCastToActivity regression'; exit 1
fi
grep -Fq 'val activity = LocalActivity.current' "$SECURITY" || { echo 'STOP: account security Activity unwrap was not modernized'; exit 1; }
test -s "$REPORTER" || { echo 'STOP: comprehensive lint reporter missing'; exit 1; }
grep -Fq 'python3 scripts/print-all-release-lint-errors.py' "$BUILDER" || { echo 'STOP: lint diagnostics are not exposed in build logs'; exit 1; }
grep -Fq 'STOP: release lint failed; AAB not produced.' "$BUILDER" || { echo 'STOP: lint gate is not fail-closed'; exit 1; }
python3 -m py_compile "$REPORTER"
bash -n "$BUILDER"
echo 'Stage 022 Activity Compose lint fix / full error reporting checks passed.'
