#!/usr/bin/env bash
set -euo pipefail
ROOT="$(cd "$(dirname "$0")/.." && pwd)"
cd "$ROOT"
bash scripts/verify-stage12.sh

test -f app/src/main/java/com/iumrah/beta/ui/components/PaymentMethodsMarquee.kt
test -f app/src/main/res/drawable-nodpi/payment_visa.png
test -f app/src/main/res/drawable-nodpi/payment_payme.png
test -f app/src/main/res/drawable-nodpi/payment_humo.png
test -f app/src/main/res/drawable-nodpi/payment_uzcard.png
test -f app/src/main/res/drawable-nodpi/payment_click.png

grep -q 'private fun ServicesSection' app/src/main/java/com/iumrah/beta/ui/account/AccountScreens.kt
grep -q '"Airmora Flights Status"' app/src/main/java/com/iumrah/beta/ui/account/AccountScreens.kt
grep -q 'IumrahPaymentMethodsMarquee' app/src/main/java/com/iumrah/beta/ui/account/AccountScreens.kt
grep -q 'IumrahPaymentMethodsMarquee' app/src/main/java/com/iumrah/beta/ui/booking/PilgrimCheckoutScreen.kt
grep -q 'data object BookingZiyarats' app/src/main/java/com/iumrah/beta/core/navigation/AppChromeStore.kt
grep -q 'openBookingZiyarats' app/src/main/java/com/iumrah/beta/core/navigation/AppChromeStore.kt
grep -q 'BookingZiyaratCatalogScreen' app/src/main/java/com/iumrah/beta/ui/shell/AppShell.kt
grep -q 'fun BookingZiyaratCatalogScreen' app/src/main/java/com/iumrah/beta/ui/ziyarats/ZiyaratJourneyScreen.kt
grep -q 'chrome.openBookingZiyarats()' app/src/main/java/com/iumrah/beta/ui/booking/BookingsHomeScreen.kt
grep -q 'fontSize = 25.sp' app/src/main/java/com/iumrah/beta/ui/booking/JourneyTravelInfoPanel.kt

echo "Stage 013 Ziyarats / payments / account v4 parity checks passed."
