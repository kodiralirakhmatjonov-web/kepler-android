#!/usr/bin/env bash
set -euo pipefail
bash scripts/verify-stage5.sh
required=(
 ANDROID_STAGE_006_PACKAGE_GENERATOR_APPLIED.txt
 app/src/main/java/com/iumrah/beta/data/hotel/RemotePackageEngineClient.kt
 app/src/main/java/com/iumrah/beta/domain/pricing/PackageGenerator.kt
 app/src/main/java/com/iumrah/beta/domain/booking/BookingDraftBuilder.kt
 app/src/main/java/com/iumrah/beta/data/booking/BookingStore.kt
 app/src/main/java/com/iumrah/beta/ui/packageflow/FinalPackageScreen.kt
)
for f in "${required[@]}"; do test -s "$f" || { echo "STOP: missing Stage 006 file $f"; exit 1; }; done
grep -q 'makkahRoomCategory' app/src/main/java/com/iumrah/beta/domain/journey/JourneyStore.kt
grep -q 'madinahRoomCategory' app/src/main/java/com/iumrah/beta/domain/journey/JourneyStore.kt
grep -q 'packageEngine.packageQuote(state)' app/src/main/java/com/iumrah/beta/domain/pricing/PackageGenerator.kt
grep -q '"/api/package/quote"' app/src/main/java/com/iumrah/beta/data/hotel/RemotePackageEngineClient.kt
grep -q 'quoteProof' app/src/main/java/com/iumrah/beta/domain/pricing/PricingModels.kt
grep -q 'commitPricingReport' app/src/main/java/com/iumrah/beta/data/booking/BookingStore.kt
grep -q 'TransferSelectionScreen(language, journey, packageGenerator, chrome)' app/src/main/java/com/iumrah/beta/ui/shell/AppShell.kt
if grep -q 'LocalPackagePricingEngine(' app/src/main/java/com/iumrah/beta/core/di/IumrahAppContainer.kt; then
  echo 'STOP: active app container must use server PackageEngine, not local markup'; exit 1
fi
echo 'Stage 006 server-authoritative package generator checks passed.'
