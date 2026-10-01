#!/usr/bin/env bash
set -euo pipefail
bash scripts/verify-stage9.sh
required=(
  app/src/main/java/com/iumrah/beta/ui/generator/GeneratorChrome.kt
  app/src/main/java/com/iumrah/beta/ui/trip/TripBuilderScreen.kt
  app/src/main/java/com/iumrah/beta/ui/trip/HotelSelectionScreen.kt
  app/src/main/java/com/iumrah/beta/ui/flights/FlightSearchScreen.kt
  app/src/main/java/com/iumrah/beta/ui/packageflow/TransferSelectionScreen.kt
  app/src/main/java/com/iumrah/beta/ui/packageflow/FinalPackageScreen.kt
  app/src/main/java/com/iumrah/beta/ui/booking/BookingCheckoutScreen.kt
  app/src/main/java/com/iumrah/beta/domain/trip/TransferModels.kt
  app/src/main/res/drawable-nodpi/transfer_malibu.png
  app/src/main/res/drawable-nodpi/transfer_carnival.png
  app/src/main/res/drawable-nodpi/transfer_yukon.png
  app/src/main/res/drawable-nodpi/haramain_mark.png
  app/src/main/res/drawable-nodpi/haramain_gallery_train.png
  app/src/main/res/drawable-nodpi/haramain_gallery_station.jpg
  app/src/main/res/drawable-nodpi/haramain_gallery_interior.jpg
)
for f in "${required[@]}"; do test -s "$f" || { echo "STOP: missing Configurator parity file $f"; exit 1; }; done
grep -q 'data object ReturnFlights' app/src/main/java/com/iumrah/beta/core/navigation/AppChromeStore.kt
grep -q 'data object TransferSelection' app/src/main/java/com/iumrah/beta/core/navigation/AppChromeStore.kt
grep -q 'GeneratorStage.TRIP' app/src/main/java/com/iumrah/beta/ui/trip/TripBuilderScreen.kt
grep -q 'GeneratorStage.HOTEL' app/src/main/java/com/iumrah/beta/ui/trip/HotelSelectionScreen.kt
grep -q 'GeneratorStage.FLIGHT' app/src/main/java/com/iumrah/beta/ui/flights/FlightSearchScreen.kt
grep -q 'GeneratorStage.TRANSFER' app/src/main/java/com/iumrah/beta/ui/packageflow/TransferSelectionScreen.kt
grep -q 'GeneratorStage.READY' app/src/main/java/com/iumrah/beta/ui/packageflow/FinalPackageScreen.kt
grep -q 'TransferVehicleKind.CARNIVAL' app/src/main/java/com/iumrah/beta/ui/packageflow/TransferSelectionScreen.kt
grep -q 'HaramainFareClass' app/src/main/java/com/iumrah/beta/ui/packageflow/TransferSelectionScreen.kt
grep -q 'packageEngine.packageQuote(state)' app/src/main/java/com/iumrah/beta/domain/pricing/PackageGenerator.kt
grep -q 'Random.nextInt(20, 41)' app/src/main/java/com/iumrah/beta/ui/packageflow/TransferSelectionScreen.kt
grep -q 'quoteProof' app/src/main/java/com/iumrah/beta/ui/packageflow/TransferSelectionScreen.kt
grep -q 'hasFinalGeneratorQuote' app/src/main/java/com/iumrah/beta/domain/journey/JourneyStore.kt
grep -q 'packageMarkupRate: BigDecimal = BigDecimal("0.50")' app/src/main/java/com/iumrah/beta/domain/pricing/LocalPackagePricingEngine.kt
if grep -q 'PUBLISHED_DIRECT' app/src/main/java/com/iumrah/beta/ui/trip/TripBuilderScreen.kt; then
  echo 'STOP: manual configurator must not expose the unrelated curated storefront lane'; exit 1
fi
if grep -qE 'HaramainFareClass\.BUSINESS|Business ·' app/src/main/java/com/iumrah/beta/ui/packageflow/TransferSelectionScreen.kt; then
  echo 'STOP: current iOS Transfer flow exposes Standard Haramain only'; exit 1
fi
if grep -R -n '^import androidx.compose.foundation.layout.weight$' app/src/main/java/com/iumrah/beta; then
  echo 'STOP: explicit Compose weight import is incompatible with this project version'; exit 1
fi
# Material3 ModalBottomSheet is experimental in the Compose version used by this project.
while IFS= read -r f; do
  if ! grep -q 'ExperimentalMaterial3Api' "$f"; then
    echo "STOP: ModalBottomSheet requires ExperimentalMaterial3Api opt-in: $f"; exit 1
  fi
done < <(grep -R -lE 'ModalBottomSheet|rememberModalBottomSheetState' app/src/main/java/com/iumrah/beta --include='*.kt' || true)
if grep -R -nE 'material\.icons|Icons\.' \
  app/src/main/java/com/iumrah/beta/ui/generator \
  app/src/main/java/com/iumrah/beta/ui/trip/TripBuilderScreen.kt \
  app/src/main/java/com/iumrah/beta/ui/trip/HotelSelectionScreen.kt \
  app/src/main/java/com/iumrah/beta/ui/flights/FlightSearchScreen.kt \
  app/src/main/java/com/iumrah/beta/ui/packageflow/TransferSelectionScreen.kt \
  app/src/main/java/com/iumrah/beta/ui/booking/BookingCheckoutScreen.kt; then
  echo 'STOP: Configurator flow must use Cupertino renderer, not Material icons'; exit 1
fi
echo 'Stage 010 iOS Configurator parity checks passed.'
