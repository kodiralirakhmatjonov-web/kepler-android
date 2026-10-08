#!/usr/bin/env bash
set -euo pipefail
ROOT="$(cd "$(dirname "$0")/.." && pwd)"
cd "$ROOT"
export IUMRAH_VERIFY_LATEST=1
bash scripts/verify-stage23.sh
python3 scripts/verify-android-resource-images.py
python3 - <<'PY'
from pathlib import Path
p=Path('app/src/main/res/drawable-nodpi/iumrah_esim_home_card.png')
raw=p.read_bytes()[:8]
assert raw == b'\x89PNG\r\n\x1a\n', 'iumrah_esim_home_card.png must be a genuine PNG for AAPT2'
print('Stage 024: Android/AAPT image resource integrity checks passed.')
PY
