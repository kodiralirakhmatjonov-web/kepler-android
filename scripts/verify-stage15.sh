#!/usr/bin/env bash
set -euo pipefail
ROOT="$(cd "$(dirname "$0")/.." && pwd)"
cd "$ROOT"
bash scripts/verify-stage14.sh

IDENTITY=app/src/main/java/com/iumrah/beta/ui/account/AccountIdentityWallet.kt
ACCOUNT=app/src/main/java/com/iumrah/beta/ui/account/AccountScreens.kt
TRAVELERS=app/src/main/java/com/iumrah/beta/ui/account/AccountSecondaryParity.kt
BOOKING=app/src/main/java/com/iumrah/beta/ui/booking/BookingDetailScreen.kt
ICON=app/src/main/res/drawable-nodpi/iumrah_app_icon.png

# iumrah ID polish
grep -q 'publicNameSize' "$IDENTITY"
grep -q 'Modifier.size(106.dp)' "$IDENTITY"
! grep -q 'Scan to open your iumrah ID on the web' "$IDENTITY"

# Account structure and passport-first travelers
grep -q '"Payment & privacy"' "$ACCOUNT"
! grep -q '"KYC · iumrah Security"' "$ACCOUNT"
! grep -q 'item { ActiveTripCard' "$ACCOUNT"
grep -q '"Google and active sessions"' "$ACCOUNT"
grep -q '"Passport first"' "$TRAVELERS"
grep -q 'Manual fields are optional' "$TRAVELERS"
grep -q '"Review passport"' "$TRAVELERS"
grep -q '"Attach passport"' "$TRAVELERS"

# Production Care card ordering/content
grep -q 'BookingCareBalanceCard' "$BOOKING"
grep -q 'loadCareProfile' "$BOOKING"
grep -q '"Open iumrah chat"' "$BOOKING"
grep -q 'R.drawable.care_price_support' "$BOOKING"
! grep -q 'BookingCareCard(' "$BOOKING"

# Current world-map launcher artwork
test -f "$ICON"
EXPECTED='c7015e85a99ee6153de206b4ab8ffc76d4aafc6ae99bcc3276e1448174da5403'
ACTUAL="$(sha256sum "$ICON" | awk '{print $1}')"
test "$ACTUAL" = "$EXPECTED"
for density in mdpi hdpi xhdpi xxhdpi xxxhdpi; do
  test -s "app/src/main/res/mipmap-$density/ic_launcher.png"
  test -s "app/src/main/res/mipmap-$density/ic_launcher_round.png"
done

echo "Stage 015 final current-iOS UI delta parity checks passed."
