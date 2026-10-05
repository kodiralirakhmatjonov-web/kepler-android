# iumrah Android v35 — Google Play non-Billing production audit

Date: 2026-10-05  
Scope: cumulative v34 + v35 non-Billing cleanup and production release gates.

## Result summary

- Release applicationId remains `com.iumrah.app`.
- Namespace remains `com.iumrah.beta`.
- compileSdk/targetSdk remain 36; minSdk remains 26.
- Default repository version is versionCode `15`, versionName `2.0.3`; Play workflow can override both with explicit release inputs.
- No direct `com.android.billingclient:billing` or `billing-ktx` dependency is declared in the app or root Gradle files.
- No active BillingClient API code, purchase-token logic, restore-purchases logic, paywall route, Billing permission/service/metadata, Billing ProGuard rule, or Google Play Billing UI remains in active production source.
- The old `core/billing` compatibility stubs and old Plus/IAP documentation remnants are deleted by the v35 release cleanup.
- Stage 17 validates source declarations and then requires the resolved release dependency graph, merged release manifest and final AAB to be Billing-free.

## A. Billing dependencies found

No direct Google Play Billing dependency was found in the current v34 production Gradle declarations. The only Billing remnants found in the repository were two inert legacy Kotlin stubs and two legacy Plus/IAP documentation files. They contained no usable Billing implementation and are removed in v35.

## B. Files where Billing/IAP remnants were found

Removed:

- `app/src/main/java/com/iumrah/beta/core/billing/LegacyPlayCard.kt`
- `app/src/main/java/com/iumrah/beta/core/billing/LegacyPlayPurchases.kt`
- `docs/google-play/legacy/plus_page.dart.txt`
- `docs/google-play/legacy/premium_service.dart.txt`

Billing strings that remain are only negative release guards in verifier/build scripts; they are intentionally used to fail the build if Billing reappears.

## C. Billing-specific files changed/added

- `app/build.gradle.kts`
- `scripts/remove-legacy-play-billing.sh`
- `scripts/verify_no_play_billing.py`
- `scripts/verify-stage10.sh`
- `scripts/verify-stage16.sh`
- `scripts/verify-stage17.sh`
- `scripts/build-play-release.sh`
- `docs/release/iumrah-play-release.yml`
- `docs/release/GOOGLE_PLAY_RELEASE.md`
- `docs/release/GOOGLE_PLAY_NON_BILLING_AUDIT_V35.md`
- `docs/google-play/README-RU.md`
- `README.md`
- `ANDROID_GOOGLE_PLAY_NON_BILLING_20261005_V35_APPLIED.txt`
- `parity/google-play-non-billing-production-audit-20261005-v35.json`
- `V35_DELETE_PATHS.txt`

The v35 ZIP is cumulative and also contains every file from v34, so it can be applied directly to the clean source repository.

## D. Subscription / IAP code removed

There was no active BillingClient purchase implementation in v34 active source. v35 physically removes the old `core/billing` compatibility remnants and old Plus/IAP docs. Searches of active production Kotlin/XML found no `BillingClient`, `BillingClientStateListener`, `PurchasesUpdatedListener`, `ProductDetails`, `QueryProductDetailsParams`, `acknowledgePurchase`, `consumeAsync`, `launchBillingFlow`, `queryPurchasesAsync`, `queryProductDetailsAsync`, `purchaseToken`, `restorePurchases`, `PurchaseManager`, or `BillingManager` implementation.

Generic travel uses of words such as “Premium hotels” and “Premium economy” remain because they are unrelated to Google Play Billing.

## E. Transitive Billing dependency

A static audit found no monetization SDK or direct Billing declaration in the project. The definitive transitive check is now a mandatory release gate: Gradle must resolve `releaseRuntimeClasspath`; the build fails if any resolved component belongs to `com.android.billingclient` or contains `billingclient` in its module name. No dependency exclusion is used to hide Billing.

This execution environment does not contain Gradle/Android SDK and cannot access the repository's private GitHub signing secrets, so the resolved Gradle graph could not be executed locally here. The GitHub release workflow runs the required resolved-graph checks before compilation and stores the raw reports as release artifacts.

## F. `dependencyInsight billingclient` result

Required command in the production workflow:

```bash
app:dependencyInsight --dependency billingclient --configuration releaseRuntimeClasspath
```

The workflow rejects the release if the output or resolved Gradle component graph contains `com.android.billingclient`. A successful clean run is expected to report no matching dependency. A local command result is not fabricated here because no Gradle executable or wrapper is present in the supplied repository/runtime.

## G. Production build result

Static/repository release audit: **PASS** (`Stage 2` through `Stage 17`).

Clean-apply test: **PASS**. A clean `kepler-android-main 2(1)` repository was overlaid with only v35; the existing Stage-10 CI entry point delegated to Stage 17, removed all four legacy Billing/IAP remnants, and Stage 2–17 passed.

Local production compile/AAB: **not executable in this runtime**. A controlled probe reached the build script and stopped exactly at:

`STOP: Gradle executable not found (Gradle 9.6 is expected by CI).`

No replacement signing key was used to create a publishable artifact. The GitHub workflow is the production build environment because it provisions Gradle 9.6/API 36 and restores the existing Play upload key from repository secrets.

The workflow then runs:

```text
app:dependencies --configuration releaseRuntimeClasspath
app:dependencyInsight --dependency billing --configuration releaseRuntimeClasspath
app:dependencyInsight --dependency billingclient --configuration releaseRuntimeClasspath
:app:verifyNoGooglePlayBilling
clean
:app:testDebugUnitTest
:app:lintRelease
:app:assembleRelease
:app:bundleRelease
```

It subsequently checks the merged release manifest and scans the final AAB for Billing SDK markers.

## H. Final AAB path

After a successful GitHub production release run:

`artifacts/play-release/iumrah-<versionName>-vc<versionCode>-release.aab`

For the repository defaults `2.0.3` / `15`, the path is:

`artifacts/play-release/iumrah-2.0.3-vc15-release.aab`

A signed publishable AAB is not attached to this audit because the accepted Google Play upload keystore is not present in the supplied files/runtime. Creating a new key would break update signing and was intentionally not done.

## I. Release applicationId

`com.iumrah.app`

Unchanged. The debug applicationId remains `com.iumrah.beta`; this does not change the Play release identity.

## J. Version

Repository defaults:

- versionCode: `15`
- versionName: `2.0.3`

The production workflow requires an explicit versionCode input and it must be higher than every version already uploaded to Play Console.

## Manifest / R8 / navigation audit

- Source `AndroidManifest.xml`: no Billing permission/service/metadata.
- Production builder inspects the AGP merged release manifest and fails on Billing markers.
- `proguard-rules.pro` / `consumer-rules.pro`: no Billing-specific rules found.
- Active Compose/navigation source: no paywall, purchase, subscription or Billing routes found.
- Auth, backend API, Firebase production identity inputs, push notifications, deep links/App Links, package identity and signing configuration were not changed by the Billing cleanup.

## Release acceptance rule

A v35 production release is allowed only if all of these are true:

1. `releaseRuntimeClasspath` has no `com.android.billingclient` component.
2. `dependencyInsight billing` and `dependencyInsight billingclient` contain no BillingClient module.
3. merged release manifest contains no Billing permission/service/metadata.
4. final AAB binary scan contains no Google Play Billing SDK package markers.
5. release is signed using the existing accepted Google Play upload key.
6. applicationId is exactly `com.iumrah.app`.
