#!/usr/bin/env bash
set -euo pipefail
bash scripts/verify-stage7.sh
required=(
 ANDROID_STAGE_008_CARE_NOTIFICATIONS_APPLIED.txt
 app/src/main/java/com/iumrah/beta/data/chat/ChatService.kt
 app/src/main/java/com/iumrah/beta/data/notification/ClientNotificationStore.kt
 app/src/main/java/com/iumrah/beta/models/notification/NotificationModels.kt
 app/src/main/java/com/iumrah/beta/ui/care/CareHomeScreen.kt
 app/src/main/java/com/iumrah/beta/ui/chat/BookingChatScreen.kt
 app/src/main/java/com/iumrah/beta/ui/notifications/NotificationsScreen.kt
)
for f in "${required[@]}"; do test -s "$f" || { echo "STOP: missing Stage 008 file $f"; exit 1; }; done
grep -q '/client/chats/\$bookingID/messages' app/src/main/java/com/iumrah/beta/data/chat/ChatService.kt
grep -q 'delay(6_000)' app/src/main/java/com/iumrah/beta/ui/chat/BookingChatScreen.kt
grep -q '/client/notifications/devices' app/src/main/java/com/iumrah/beta/data/notification/ClientNotificationStore.kt
grep -q 'deviceToken = null' app/src/main/java/com/iumrah/beta/ui/notifications/NotificationsScreen.kt
grep -q 'POST_NOTIFICATIONS' app/src/main/AndroidManifest.xml
if find . -iname 'google-services.json' -o -iname '*keystore*' | grep -q .; then
 echo 'STOP: credentials/signing material must not be shipped in update'; exit 1
fi
grep -q 'BigDecimal("0.20")' app/src/main/java/com/iumrah/beta/domain/pricing/LocalPackagePricingEngine.kt

# Stable API 36 dependency matrix: keep libraries below their API 37 compileSdk cutovers.
grep -q 'compileSdk = 36' app/build.gradle.kts
grep -q 'targetSdk = 36' app/build.gradle.kts
grep -q 'androidx.compose:compose-bom:2026.06.01' app/build.gradle.kts
grep -q 'androidx.core:core-ktx:1.17.0' app/build.gradle.kts
grep -q 'io.coil-kt.coil3:coil-compose:3.5.0' app/build.gradle.kts
grep -q 'io.coil-kt.coil3:coil-network-okhttp:3.5.0' app/build.gradle.kts
test -s ANDROID_BUILD_HOTFIX_009_STABLE_API36_DEPS_APPLIED.txt

echo 'Stage 008 care/notification + stable API 36 dependency checks passed.'
