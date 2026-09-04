#!/usr/bin/env bash
set -euo pipefail

test -f scripts/verify-stage3.sh || { echo 'STOP: Stage 004 requires Stage 003 verifier.'; exit 2; }
bash scripts/verify-stage3.sh

required=(
  ANDROID_STAGE_004_HOTELS_APPLIED.txt
  app/src/main/java/com/iumrah/beta/models/hotel/HotelModels.kt
  app/src/main/java/com/iumrah/beta/data/hotel/HotelCatalogService.kt
  app/src/main/java/com/iumrah/beta/data/hotel/RemotePackageEngineClient.kt
  app/src/main/java/com/iumrah/beta/ui/hotels/HotelsScreen.kt
  app/src/main/java/com/iumrah/beta/ui/hotels/HotelDetailScreen.kt
)
for f in "${required[@]}"; do test -s "$f" || { echo "STOP: missing Stage 004 file: $f"; exit 1; }; done

grep -q '/api/catalog/hotels' app/src/main/java/com/iumrah/beta/data/hotel/HotelCatalogService.kt
grep -q '/api/package/hotel/\$hotelID/room-categories' app/src/main/java/com/iumrah/beta/data/hotel/RemotePackageEngineClient.kt
grep -q 'providerDisplayName: String get() = "iumrah Hotels"' app/src/main/java/com/iumrah/beta/models/hotel/HotelModels.kt
grep -q 'BigDecimal("0.20")' app/src/main/java/com/iumrah/beta/domain/pricing/LocalPackagePricingEngine.kt

echo 'Stage 004 hotel checks passed.'
