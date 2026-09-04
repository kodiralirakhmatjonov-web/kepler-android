# iumrah Android

Native Android client for the same iumrah platform used by the SwiftUI client and iumrah Business.

## Non-negotiable parity rules

- Backend base: `https://iumrah.app`.
- Android does **not** create or own a separate HOTELS/BOOKINGS database.
- Booking/account/hotel/flight contracts are ported from the current iOS source of truth.
- Package pricing is a client-side port of `LocalPackagePricingEngine.swift`.
- Temporary Android test-build exception: package markup is **20%**. Current iOS source remains 50%.
- Round-trip/open-jaw fare remains one complete provider journey fare; never sum two independent one-way fares.
- Hotel price unit remains USD per room/night × rooms × actual nights.

## Update workflow

After the initial GitHub workflow is installed manually, all GPT patches use root-level:

`iumrah-android-update-*.zip`

The CI workflow safely extracts the patch, removes the ZIP, commits, rebases and pushes. Build is manual via `workflow_dispatch`.

## Stage

Stage 1/10: foundation + pricing parity core.
