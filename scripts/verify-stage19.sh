#!/usr/bin/env bash
set -euo pipefail
ROOT="$(cd "$(dirname "$0")/.." && pwd)"
cd "$ROOT"
export IUMRAH_VERIFY_LATEST=1
bash scripts/verify-stage18.sh
S8=scripts/verify-stage8.sh
test -s "$S8" || { echo 'STOP: Stage 8 verifier missing'; exit 1; }
grep -Fq -- "-iname '*.jks'" "$S8" || { echo 'STOP: exact JKS credential gate missing'; exit 1; }
grep -Fq -- "-iname '*.keystore'" "$S8" || { echo 'STOP: exact keystore credential gate missing'; exit 1; }
grep -Fq -- "-iname 'key.properties'" "$S8" || { echo 'STOP: key.properties credential gate missing'; exit 1; }
if grep -Fq -- "-iname '*keystore*'" "$S8"; then
  echo 'STOP: broad *keystore* matcher reintroduced; it blocks safe update ZIP filenames'; exit 1
fi
echo 'Stage 019 release credential-verifier false-positive fix passed.'
