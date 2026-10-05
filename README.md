# iumrah Android

Native Android client for the same iumrah platform used by the SwiftUI client and iumrah Business.

## Non-negotiable parity rules

- Backend base: `https://iumrah.app`.
- Android does **not** create or own a separate HOTELS/BOOKINGS database.
- Booking/account/hotel/flight contracts are ported from the current iOS source of truth.
- Production package pricing is server-authoritative. The diagnostic local fallback mirrors the current iOS fallback.
- Package markup fallback is **50%** on full supplier cost; payment fee is **2%**.
- Round-trip/open-jaw fare remains one complete provider journey fare; never sum two independent one-way fares.
- Hotel price unit remains USD per room/night × rooms × actual nights.

## Update workflow

After the initial GitHub workflow is installed manually, GPT patches use root-level:

`iumrah-android-update-*.zip`

The CI workflow safely extracts the patch, removes the ZIP, commits, rebases and pushes. Build is manual via `workflow_dispatch`.

## Current production checkpoint

Current parity/release checkpoint: **Stage 16 / v34**.

- Current iOS feature/UI parity through v33.
- Google Play production identity: `com.iumrah.app`.
- `compileSdk = 36`, `targetSdk = 36`, `minSdk = 26`.
- Play Billing/subscriptions are not part of this build.
- Release AAB is built through `scripts/build-play-release.sh`.
- Native libraries are checked for 16 KB page-size compatibility before release.

Design baseline: `docs/IUMRAH_GALAXY_UI_STANDARD.md`.
Release guide: `docs/release/GOOGLE_PLAY_RELEASE.md`.

## v35 Google Play non-Billing release gate

Google Play Billing is intentionally not part of this application. v35 removes the remaining legacy Billing source/docs stubs and makes Billing absence a production invariant: the resolved `releaseRuntimeClasspath`, merged release manifest and final AAB must all be Billing-free. Release identity remains `com.iumrah.app`; signing configuration is unchanged.

## Google Play CI v36 hotfix
The release workflow staged at `docs/release/iumrah-play-release.yml` now uses `android-actions/setup-android@v4` because Google removed the legacy Android SDK package `tools`. It also accepts the older `IUMRAH_UPLOAD_*` signing secret names used by previous iumrah workflows.
