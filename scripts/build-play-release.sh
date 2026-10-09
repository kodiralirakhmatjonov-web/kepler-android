#!/usr/bin/env bash
set -euo pipefail

ROOT="$(cd "$(dirname "$0")/.." && pwd)"
cd "$ROOT"

VERSION_CODE="${1:-${PLAY_VERSION_CODE:-}}"
VERSION_NAME="${2:-${PLAY_VERSION_NAME:-2.0.3}}"
if [[ -z "$VERSION_CODE" || ! "$VERSION_CODE" =~ ^[0-9]+$ ]]; then
  echo "Usage: $0 <PLAY_VERSION_CODE> [PLAY_VERSION_NAME]"
  echo "Example: $0 15 2.0.3"
  exit 2
fi
if (( VERSION_CODE < 15 )); then
  echo 'STOP: versionCode must be greater than the archived Google Play version 14.'
  exit 1
fi

required=(PLAY_KEYSTORE_PATH PLAY_STORE_PASSWORD PLAY_KEY_ALIAS PLAY_KEY_PASSWORD)
for name in "${required[@]}"; do
  [[ -n "${!name:-}" ]] || { echo "STOP: missing $name"; exit 1; }
done
[[ -f "$PLAY_KEYSTORE_PATH" ]] || { echo "STOP: upload keystore not found: $PLAY_KEYSTORE_PATH"; exit 1; }

ACTUAL_CERT="$(keytool -exportcert -alias "$PLAY_KEY_ALIAS" -keystore "$PLAY_KEYSTORE_PATH" -storepass "$PLAY_STORE_PASSWORD" 2>/dev/null | sha256sum | awk '{print toupper($1)}')"
if [[ -n "${PLAY_UPLOAD_CERT_SHA256:-}" ]]; then
  EXPECTED_CERT="$(printf '%s' "$PLAY_UPLOAD_CERT_SHA256" | tr -d '[:space:]:' | tr '[:lower:]' '[:upper:]')"
  [[ "$ACTUAL_CERT" == "$EXPECTED_CERT" ]] || {
    echo "STOP: upload-key SHA-256 does not match PLAY_UPLOAD_CERT_SHA256."
    echo "Expected: $EXPECTED_CERT"
    echo "Actual:   $ACTUAL_CERT"
    exit 1
  }
fi
printf '%s\n' "$ACTUAL_CERT" > artifacts-upload-cert.tmp

if [[ -x ./gradlew ]]; then
  GRADLE="$ROOT/gradlew"
else
  GRADLE="$(command -v gradle || true)"
fi
[[ -n "$GRADLE" && -x "$GRADLE" ]] || { echo 'STOP: Gradle executable not found (Gradle 9.6 is expected by CI).'; exit 1; }

bash scripts/verify-stage25.sh
bash scripts/verify-build-env.sh

rm -rf artifacts/play-release
mkdir -p artifacts/play-release
mv artifacts-upload-cert.tmp artifacts/play-release/upload-key-sha256.txt

# User-requested production dependency audit. Resolve the exact release graph before compiling.
"$GRADLE" --no-daemon --console=plain \
  app:dependencies --configuration releaseRuntimeClasspath \
  2>&1 | tee artifacts/play-release/releaseRuntimeClasspath.txt

"$GRADLE" --no-daemon --console=plain \
  app:dependencyInsight --dependency billing --configuration releaseRuntimeClasspath \
  2>&1 | tee artifacts/play-release/dependencyInsight-billing.txt

"$GRADLE" --no-daemon --console=plain \
  app:dependencyInsight --dependency billingclient --configuration releaseRuntimeClasspath \
  2>&1 | tee artifacts/play-release/dependencyInsight-billingclient.txt

if grep -R -nF 'com.android.billingclient' \
  artifacts/play-release/releaseRuntimeClasspath.txt \
  artifacts/play-release/dependencyInsight-billing.txt \
  artifacts/play-release/dependencyInsight-billingclient.txt; then
  echo 'STOP: com.android.billingclient resolved into releaseRuntimeClasspath.'
  exit 1
fi

# Resolve the graph through the Gradle API too, so a formatting change in reports cannot bypass the gate.
"$GRADLE" --no-daemon --console=plain :app:verifyNoGooglePlayBilling \
  2>&1 | tee artifacts/play-release/verify-no-google-play-billing.log

# Exact production build sequence requested for Play release validation.
"$GRADLE" --no-daemon --console=plain clean \
  2>&1 | tee artifacts/play-release/clean.log

# Fail fast on Kotlin source errors before lint/R8/bundling. Gradle reuses these outputs later.
"$GRADLE" --no-daemon --console=plain --stacktrace \
  -PPLAY_VERSION_CODE="$VERSION_CODE" \
  -PPLAY_VERSION_NAME="$VERSION_NAME" \
  :app:compileDebugKotlin \
  :app:compileReleaseKotlin \
  2>&1 | tee artifacts/play-release/compile-kotlin.log

# Keep the release lint gate strict. If lint fails, Gradle normally prints only
# the *first* of several errors. Expand the complete report into Actions logs.
if ! "$GRADLE" --no-daemon --console=plain --stacktrace \
  -PPLAY_VERSION_CODE="$VERSION_CODE" \
  -PPLAY_VERSION_NAME="$VERSION_NAME" \
  :app:testDebugUnitTest \
  :app:lintRelease \
  2>&1 | tee artifacts/play-release/tests-lint.log; then
  echo '===== FULL ANDROID LINT ERROR SUMMARY ====='
  python3 scripts/print-all-release-lint-errors.py || true
  echo '===== END ANDROID LINT ERROR SUMMARY ====='
  echo 'STOP: release lint failed; AAB not produced. All detected errors are listed above.'
  exit 1
fi

"$GRADLE" --no-daemon --console=plain --stacktrace \
  -PPLAY_VERSION_CODE="$VERSION_CODE" \
  -PPLAY_VERSION_NAME="$VERSION_NAME" \
  :app:assembleRelease \
  2>&1 | tee artifacts/play-release/assembleRelease.log

"$GRADLE" --no-daemon --console=plain --stacktrace \
  -PPLAY_VERSION_CODE="$VERSION_CODE" \
  -PPLAY_VERSION_NAME="$VERSION_NAME" \
  :app:bundleRelease \
  2>&1 | tee artifacts/play-release/bundleRelease.log

AAB="$(find app/build/outputs/bundle/release -maxdepth 1 -type f -name '*.aab' -print -quit)"
[[ -n "$AAB" && -s "$AAB" ]] || { echo 'STOP: release AAB was not produced.'; exit 1; }
APK="$(find app/build/outputs/apk/release -maxdepth 1 -type f -name '*.apk' -print -quit || true)"

unzip -t "$AAB" >/dev/null
if command -v jarsigner >/dev/null; then
  jarsigner -verify "$AAB" >/dev/null || { echo 'STOP: AAB JAR signature verification failed.'; exit 1; }
fi
bash scripts/verify-16kb-native-libs.sh "$AAB"
python3 scripts/verify_no_play_billing.py --aab "$AAB" | tee artifacts/play-release/aab-no-billing.log

# Inspect the AGP merged release manifest, not only the source manifest.
MERGED_MANIFEST="$(find app/build/intermediates -type f -name AndroidManifest.xml \
  \( -path '*merged_manifest*release*' -o -path '*merged_manifests*release*' -o -path '*processReleaseManifest*' \) \
  -print | head -n 1 || true)"
if [[ -z "$MERGED_MANIFEST" ]]; then
  echo 'STOP: merged release AndroidManifest.xml not found.'
  exit 1
fi
cp "$MERGED_MANIFEST" artifacts/play-release/AndroidManifest.merged-release.xml
if grep -nEi 'com\.android\.vending\.BILLING|com\.android\.billingclient|billingclient' "$MERGED_MANIFEST"; then
  echo 'STOP: Billing marker found in merged release manifest.'
  exit 1
fi

# If bundletool is available, also decode the AAB manifest exactly as Play sees it.
if [[ -n "${BUNDLETOOL_JAR:-}" && -f "$BUNDLETOOL_JAR" ]]; then
  MANIFEST_XML="artifacts/play-release/AndroidManifest.bundletool-release.xml"
  java -jar "$BUNDLETOOL_JAR" dump manifest --bundle="$AAB" > "$MANIFEST_XML"
  python3 scripts/verify_play_manifest.py "$MANIFEST_XML" "$VERSION_CODE"
fi

OUT="artifacts/play-release/iumrah-${VERSION_NAME}-vc${VERSION_CODE}-release.aab"
cp "$AAB" "$OUT"
sha256sum "$OUT" | tee "${OUT}.sha256"
if [[ -n "$APK" && -s "$APK" ]]; then
  cp "$APK" "artifacts/play-release/iumrah-${VERSION_NAME}-vc${VERSION_CODE}-release.apk"
fi

cat > artifacts/play-release/RELEASE.txt <<TXT
iumrah Android Google Play release
applicationId: com.iumrah.app
namespace: com.iumrah.beta
versionName: $VERSION_NAME
versionCode: $VERSION_CODE
targetSdk: 36
Google Play Billing SDK: absent (source + resolved releaseRuntimeClasspath + merged manifest + AAB scan)
artifact: $(basename "$OUT")
TXT

echo "READY: $OUT"
