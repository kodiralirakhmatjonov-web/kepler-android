#!/usr/bin/env bash
set -euo pipefail
bash scripts/verify-stage5.sh
required=(
 ANDROID_STAGE_006_PACKAGE_GENERATOR_APPLIED.txt
 app/src/main/java/com/iumrah/beta/data/pricing/LocalFXRateService.kt
 app/src/main/java/com/iumrah/beta/domain/pricing/PackageGenerator.kt
 app/src/main/java/com/iumrah/beta/domain/booking/BookingDraftBuilder.kt
 app/src/main/java/com/iumrah/beta/ui/packageflow/FinalPackageScreen.kt
)
for f in "${required[@]}"; do test -s "$f" || { echo "STOP: missing Stage 006 file $f"; exit 1; }; done
grep -q 'makkahRoomCategory' app/src/main/java/com/iumrah/beta/domain/journey/JourneyStore.kt
grep -q 'madinahRoomCategory' app/src/main/java/com/iumrah/beta/domain/journey/JourneyStore.kt
grep -q 'journeyFareUsd = fx.usd(journey.totalFare, journey.currency)' app/src/main/java/com/iumrah/beta/domain/pricing/PackageGenerator.kt
grep -q 'journeyFareUsd = journeyFareUsd' app/src/main/java/com/iumrah/beta/domain/pricing/PackageGenerator.kt
grep -q 'makkahRoomCategory = state.makkahRoomCategory' app/src/main/java/com/iumrah/beta/domain/booking/BookingDraftBuilder.kt
grep -q 'generator = packageGenerator' app/src/main/java/com/iumrah/beta/ui/shell/AppShell.kt
grep -q 'BigDecimal("0.20")' app/src/main/java/com/iumrah/beta/domain/pricing/LocalPackagePricingEngine.kt
echo 'Stage 006 package generator checks passed.'
