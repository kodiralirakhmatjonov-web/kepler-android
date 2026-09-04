#!/usr/bin/env bash
set -euo pipefail

required=(
  app/src/main/java/com/iumrah/beta/core/network/APIClient.kt
  app/src/main/java/com/iumrah/beta/core/security/SecureJsonStore.kt
  app/src/main/java/com/iumrah/beta/core/security/IumrahAccountDeviceIdentity.kt
  app/src/main/java/com/iumrah/beta/models/account/IumrahAccountModels.kt
  app/src/main/java/com/iumrah/beta/models/account/IumrahSecurityConfirmationModels.kt
  app/src/main/java/com/iumrah/beta/models/account/IumrahFriendsModels.kt
  app/src/main/java/com/iumrah/beta/models/booking/BookingSharedModels.kt
  app/src/main/java/com/iumrah/beta/data/account/IumrahAccountRoutes.kt
  app/src/main/java/com/iumrah/beta/data/account/IumrahAccountService.kt
  app/src/main/java/com/iumrah/beta/data/account/IumrahAccountStore.kt
)
for f in "${required[@]}"; do test -s "$f" || { echo "Missing $f"; exit 1; }; done

grep -q 'https://iumrah.app' app/src/main/java/com/iumrah/beta/core/config/AppConfig.kt
grep -q 'BigDecimal("0.20")' app/src/main/java/com/iumrah/beta/domain/pricing/LocalPackagePricingEngine.kt
grep -q '/api/package/client/account/login' app/src/main/java/com/iumrah/beta/data/account/IumrahAccountRoutes.kt
grep -q 'x-iumrah-device-secret' app/src/main/java/com/iumrah/beta/core/security/IumrahAccountDeviceIdentity.kt
grep -q 'AndroidKeyStore' app/src/main/java/com/iumrah/beta/core/security/SecureJsonStore.kt

echo 'Stage 2 structural verification passed.'
