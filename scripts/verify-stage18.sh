#!/usr/bin/env bash
set -euo pipefail
ROOT="$(cd "$(dirname "$0")/.." && pwd)"
cd "$ROOT"
export IUMRAH_VERIFY_LATEST=1
bash scripts/verify-stage17.sh
WF=docs/release/iumrah-play-release.yml
test -s "$WF" || { echo 'STOP: Play release workflow missing'; exit 1; }
grep -Fq 'android-actions/setup-android@v4' "$WF" || { echo 'STOP: release workflow must use setup-android@v4'; exit 1; }
if grep -Fq 'android-actions/setup-android@v3' "$WF"; then
  echo 'STOP: obsolete setup-android@v3 is still present'; exit 1
fi
grep -Fq "packages: ''" "$WF" || { echo 'STOP: setup-android v4 must not install legacy extra packages'; exit 1; }
grep -Fq 'sdkmanager --install "platform-tools" "platforms;android-36" "build-tools;36.0.0"' "$WF" || { echo 'STOP: API 36 install gate missing'; exit 1; }
grep -Fq 'secrets.PLAY_UPLOAD_KEYSTORE_BASE64 || secrets.IUMRAH_UPLOAD_KEYSTORE_BASE64' "$WF" || { echo 'STOP: upload keystore secret fallback missing'; exit 1; }
grep -Fq 'secrets.PLAY_STORE_PASSWORD || secrets.IUMRAH_UPLOAD_STORE_PASSWORD' "$WF" || { echo 'STOP: store password fallback missing'; exit 1; }
grep -Fq 'secrets.PLAY_KEY_ALIAS || secrets.IUMRAH_UPLOAD_KEY_ALIAS' "$WF" || { echo 'STOP: key alias fallback missing'; exit 1; }
grep -Fq 'secrets.PLAY_KEY_PASSWORD || secrets.IUMRAH_UPLOAD_KEY_PASSWORD' "$WF" || { echo 'STOP: key password fallback missing'; exit 1; }
echo 'Stage 018 Google Play CI SDK/signing compatibility checks passed.'
