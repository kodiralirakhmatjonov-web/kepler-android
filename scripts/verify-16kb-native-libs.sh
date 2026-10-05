#!/usr/bin/env bash
set -euo pipefail

artifact="${1:-}"
if [[ -z "$artifact" || ! -f "$artifact" ]]; then
  echo "Usage: $0 <release.aab|release.apk>"
  exit 2
fi

command -v unzip >/dev/null || { echo 'STOP: unzip is required.'; exit 1; }
command -v readelf >/dev/null || { echo 'STOP: readelf is required to validate native ELF alignment.'; exit 1; }
command -v python3 >/dev/null || { echo 'STOP: python3 is required.'; exit 1; }

tmp="$(mktemp -d)"
trap 'rm -rf "$tmp"' EXIT
unzip -qq "$artifact" -d "$tmp"

python3 - "$tmp" <<'PY'
from pathlib import Path
import re
import subprocess
import sys

root = Path(sys.argv[1])
libs = sorted(root.rglob('*.so'))
if not libs:
    print('16 KB check: no native .so libraries found; Java/Kotlin-only artifact.')
    raise SystemExit(0)

abis = set()
failed = False
for so in libs:
    rel = so.relative_to(root).as_posix()
    m = re.search(r'(?:^|/)lib/([^/]+)/', rel)
    if m:
        abis.add(m.group(1))
    proc = subprocess.run(['readelf', '-lW', str(so)], text=True, capture_output=True)
    if proc.returncode != 0:
        print(f'STOP: readelf failed for {rel}: {proc.stderr.strip()}')
        failed = True
        continue
    aligns = []
    for line in proc.stdout.splitlines():
        parts = line.split()
        if parts and parts[0] == 'LOAD':
            try:
                aligns.append(int(parts[-1], 0))
            except ValueError:
                pass
    if not aligns:
        print(f'STOP: no PT_LOAD segments found in {rel}')
        failed = True
    elif min(aligns) < 16384:
        print(f'STOP: native library is not 16 KB ELF-aligned: {rel}; PT_LOAD alignments={aligns}')
        failed = True

if 'armeabi-v7a' in abis and 'arm64-v8a' not in abis:
    print('STOP: 32-bit ARM native code exists without arm64-v8a counterpart.')
    failed = True
if 'x86' in abis and 'x86_64' not in abis:
    print('STOP: 32-bit x86 native code exists without x86_64 counterpart.')
    failed = True

if failed:
    raise SystemExit(1)
print('16 KB / 64-bit native check passed. ABIs: ' + ', '.join(sorted(abis)))
PY
