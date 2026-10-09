#!/usr/bin/env bash
set -euo pipefail
ROOT="$(cd "$(dirname "$0")/.." && pwd)"
cd "$ROOT"
export IUMRAH_VERIFY_LATEST=1
bash scripts/verify-stage24.sh
python3 - <<'PY'
from pathlib import Path
p=Path('scripts/verify-16kb-native-libs.sh').read_text()
assert "required_16kb_abis = {'arm64-v8a', 'x86_64'}" in p
assert "abi in {'armeabi-v7a', 'x86'}" in p
assert '64-bit native library is not 16 KB ELF-aligned' in p
assert '32-bit ARM native code exists without arm64-v8a counterpart' in p
assert '32-bit x86 native code exists without x86_64 counterpart' in p
b=Path('scripts/build-play-release.sh').read_text()
assert 'bash scripts/verify-stage25.sh' in b
print('Stage 025: Google Play 16 KB verifier correctly scopes ELF alignment to 64-bit ABIs.')
PY
