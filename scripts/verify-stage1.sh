#!/usr/bin/env bash
set -euo pipefail

require_file() {
  test -f "$1" || { echo "STOP: required file is missing: $1"; exit 2; }
}
require_script() {
  test -f "$1" || { echo "STOP: prerequisite verifier is missing: $1"; exit 2; }
}

require_file ANDROID_BOOTSTRAP_001_APPLIED.txt
require_file app/build.gradle.kts
require_file app/src/main/java/com/iumrah/beta/core/config/AppConfig.kt
require_file app/src/main/java/com/iumrah/beta/domain/pricing/LocalPackagePricingEngine.kt
grep -F 'const val API_BASE_URL = "https://iumrah.app"' app/src/main/java/com/iumrah/beta/core/config/AppConfig.kt >/dev/null
grep -F 'BigDecimal("0.20")' app/src/main/java/com/iumrah/beta/domain/pricing/LocalPackagePricingEngine.kt >/dev/null
if grep -R -nE 'jdbc:|sqlite:|RoomDatabase|createFromAsset' app/src/main/java >/dev/null; then
  echo 'STOP: Android-local business database implementation detected.'
  exit 1
fi

echo 'Stage 1 structural parity checks passed.'

if [[ -f ANDROID_STAGE_005_TRIP_FLIGHTS_APPLIED.txt ]]; then
  require_script scripts/verify-stage5.sh
  bash scripts/verify-stage5.sh
elif [[ -f ANDROID_STAGE_004_HOTELS_APPLIED.txt ]]; then
  require_script scripts/verify-stage4.sh
  bash scripts/verify-stage4.sh
elif [[ -f ANDROID_STAGE_003_SHELL_MOTION_APPLIED.txt ]]; then
  require_script scripts/verify-stage3.sh
  bash scripts/verify-stage3.sh
elif [[ -f ANDROID_STAGE_002_ACCOUNT_SECURITY_APPLIED.txt ]]; then
  require_script scripts/verify-stage2.sh
  bash scripts/verify-stage2.sh
fi
