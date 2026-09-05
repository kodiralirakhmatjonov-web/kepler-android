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
echo 'Stage 008 care/notification checks passed.'
