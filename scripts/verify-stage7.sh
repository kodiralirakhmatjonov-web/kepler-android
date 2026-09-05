#!/usr/bin/env bash
set -euo pipefail
bash scripts/verify-stage6.sh
required=(
 ANDROID_STAGE_007_BOOKING_CHECKOUT_APPLIED.txt
 app/src/main/java/com/iumrah/beta/data/booking/BookingService.kt
 app/src/main/java/com/iumrah/beta/data/booking/BookingStore.kt
 app/src/main/java/com/iumrah/beta/ui/booking/BookingCheckoutScreen.kt
 app/src/main/java/com/iumrah/beta/ui/booking/BookingDetailScreen.kt
 app/src/main/java/com/iumrah/beta/ui/booking/BookingHotelChangeScreen.kt
 app/src/main/java/com/iumrah/beta/ui/booking/IumrahSecurityConfirmationPanel.kt
 app/src/main/java/com/iumrah/beta/ui/booking/IumrahBookingDomeCard.kt
 app/src/main/java/com/iumrah/beta/ui/booking/PilgrimCheckoutScreen.kt
)
for f in "${required[@]}"; do test -s "$f" || { echo "STOP: missing Stage 007 file $f"; exit 1; }; done
grep -q 'api.post(AppConfig.PACKAGE_BOOKING_PATH' app/src/main/java/com/iumrah/beta/data/booking/BookingService.kt
grep -q '/api/catalog/hotels/client/trips/\$id/sync' app/src/main/java/com/iumrah/beta/data/booking/BookingService.kt
grep -q '/security/passport' app/src/main/java/com/iumrah/beta/data/booking/BookingService.kt
grep -q 'passportNumber = ""' app/src/main/java/com/iumrah/beta/ui/booking/IumrahSecurityConfirmationPanel.kt
if grep -n 'passportNumber' app/src/main/java/com/iumrah/beta/models/booking/BookingSharedModels.kt | grep -q StoredBookingSession; then
 echo 'STOP: passport number must not persist in StoredBookingSession'; exit 1
fi
grep -q 'cardFlip' app/src/main/java/com/iumrah/beta/ui/booking/IumrahBookingDomeCard.kt
grep -q 'bookingDisplayNumber: String? = null' app/src/main/java/com/iumrah/beta/models/account/IumrahAccountModels.kt
echo 'Stage 007 booking/security checks passed.'
