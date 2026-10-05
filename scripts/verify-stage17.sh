#!/usr/bin/env bash
set -euo pipefail
ROOT="$(cd "$(dirname "$0")/.." && pwd)"
cd "$ROOT"

# The cumulative ZIP overlays files, while the historical repo may still contain
# two inert Billing stubs. Delete those exact obsolete artifacts first so the
# committed production tree is genuinely Billing-free.
bash scripts/remove-legacy-play-billing.sh

export IUMRAH_VERIFY_LATEST=1
bash scripts/verify-stage16.sh
python3 scripts/verify_no_play_billing.py --source .

# The Gradle build must carry an executable resolved-graph gate, not only a text grep.
grep -q 'val verifyNoGooglePlayBilling by tasks.registering' app/build.gradle.kts || { echo 'STOP: Gradle Billing dependency gate missing'; exit 1; }
grep -q 'getByName("releaseRuntimeClasspath")' app/build.gradle.kts || { echo 'STOP: releaseRuntimeClasspath is not resolved by Billing gate'; exit 1; }
grep -q 'module.group == "com.android.billingclient"' app/build.gradle.kts || { echo 'STOP: Billing group is not checked in resolved components'; exit 1; }
grep -q 'dependsOn(verifyNoGooglePlayBilling)' app/build.gradle.kts || { echo 'STOP: release build is not wired to Billing dependency gate'; exit 1; }

test ! -d app/src/main/java/com/iumrah/beta/core/billing || { echo 'STOP: legacy core/billing package still exists'; exit 1; }
test ! -d docs/google-play/legacy || { echo 'STOP: obsolete Google Play IAP docs still exist'; exit 1; }

# Release builder must run the exact dependency audits and scan the final AAB.
for expected in \
  'app:dependencies --configuration releaseRuntimeClasspath' \
  'app:dependencyInsight --dependency billing --configuration releaseRuntimeClasspath' \
  'app:dependencyInsight --dependency billingclient --configuration releaseRuntimeClasspath' \
  'verify_no_play_billing.py --aab'; do
  grep -Fq "$expected" scripts/build-play-release.sh || { echo "STOP: release builder missing: $expected"; exit 1; }
done

echo 'Stage 017 Google Play non-Billing production audit checks passed.'
