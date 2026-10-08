#!/usr/bin/env bash
set -euo pipefail
ROOT="$(cd "$(dirname "$0")/.." && pwd)"
cd "$ROOT"
export IUMRAH_VERIFY_LATEST=1
bash scripts/verify-stage20.sh

THEME=app/src/main/java/com/iumrah/beta/core/design/IumrahTheme.kt
FLIGHTS=app/src/main/java/com/iumrah/beta/ui/flights/FlightDiscoveryPanel.kt
PAIR=app/src/main/java/com/iumrah/beta/ui/hotels/FlightFirstPackageDetailScreen.kt
HOTEL=app/src/main/java/com/iumrah/beta/ui/hotels/HotelDetailScreen.kt
HOTELS=app/src/main/java/com/iumrah/beta/ui/hotels/HotelsScreen.kt
AIRPORTS=app/src/main/java/com/iumrah/beta/ui/trip/ConfiguratorAirportSelector.kt
ZIYARATS=app/src/main/java/com/iumrah/beta/ui/ziyarats/ZiyaratJourneyScreen.kt

if grep -A2 -F 'private val IumrahTypography = Typography(' "$THEME" | grep -Fq 'fontFamily = IumrahFontFamily'; then
  echo 'STOP: unsupported Typography(fontFamily=...) compile regression'; exit 1
fi
grep -Fq 'offer.returnAt?.let { returnAt ->' "$FLIGHTS" || { echo 'STOP: delegated returnAt smart-cast fix missing'; exit 1; }
grep -Fq '?: (if (origin != "TAS") candidates(returnOrigin, "TAS").firstOrNull() else null)' "$PAIR" || { echo 'STOP: nullable inbound flight-pair fix missing'; exit 1; }
grep -Fq 'import androidx.compose.ui.graphics.graphicsLayer' "$HOTEL" || { echo 'STOP: correct graphicsLayer import missing'; exit 1; }
if grep -Fq 'import androidx.compose.ui.draw.graphicsLayer' "$HOTEL"; then echo 'STOP: obsolete graphicsLayer import reintroduced'; exit 1; fi
grep -Fq 'note: String?' "$HOTELS" || { echo 'STOP: Hotels hero optional note fix missing'; exit 1; }
grep -Fq 'import androidx.compose.foundation.layout.offset' "$AIRPORTS" || { echo 'STOP: airport-map offset import missing'; exit 1; }
if grep -Fq 'MapLibreMap = map ?: return' "$AIRPORTS"; then echo 'STOP: prohibited airport-map default return reintroduced'; exit 1; fi
if grep -Fq 'MapLibreMap = map ?: return' "$ZIYARATS"; then echo 'STOP: prohibited Ziyarats-map default return reintroduced'; exit 1; fi

echo 'Stage 021 Kotlin production compile-regression checks passed.'
