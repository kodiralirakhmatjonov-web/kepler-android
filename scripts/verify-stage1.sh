#!/usr/bin/env bash
set -euo pipefail

test -f ANDROID_BOOTSTRAP_001_APPLIED.txt
test -f app/build.gradle.kts
test -f app/src/main/java/com/iumrah/beta/core/config/AppConfig.kt
test -f app/src/main/java/com/iumrah/beta/domain/pricing/LocalPackagePricingEngine.kt
grep -F 'const val API_BASE_URL = "https://iumrah.app"' app/src/main/java/com/iumrah/beta/core/config/AppConfig.kt >/dev/null
grep -F 'BigDecimal("0.20")' app/src/main/java/com/iumrah/beta/domain/pricing/LocalPackagePricingEngine.kt >/dev/null
if grep -R -nE 'jdbc:|sqlite:|RoomDatabase|createFromAsset' app/src/main/java >/dev/null; then
  echo 'STOP: Android-local database implementation detected in Stage 1.'
  exit 1
fi
echo 'Stage 1 structural parity checks passed.'
