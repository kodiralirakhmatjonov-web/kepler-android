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

# Google Play's page-size compatibility requirement applies to 64-bit devices.
# 32-bit armeabi-v7a/x86 libraries may legitimately keep 4 KB PT_LOAD alignment;
# when the bundle also ships their 64-bit counterparts, those 32-bit binaries are
# not selected on 64-bit-only 16 KB devices. Enforce 16 KB ELF alignment on the
# 64-bit ABIs that can execute on those devices: arm64-v8a and x86_64.
required_16kb_abis = {'arm64-v8a', 'x86_64'}
abis = set()
failed = False
checked_64 = []
info_32 = []

for so in libs:
    rel = so.relative_to(root).as_posix()
    m = re.search(r'(?:^|/)lib/([^/]+)/', rel)
    abi = m.group(1) if m else None
    if abi:
        abis.add(abi)

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
        continue

    if abi in required_16kb_abis:
        checked_64.append((rel, aligns))
        if min(aligns) < 16384:
            print(f'STOP: 64-bit native library is not 16 KB ELF-aligned: {rel}; PT_LOAD alignments={aligns}')
            failed = True
    elif abi in {'armeabi-v7a', 'x86'}:
        info_32.append((rel, aligns))

# Preserve Google Play 64-bit pairing requirements for any shipped 32-bit ABI.
if 'armeabi-v7a' in abis and 'arm64-v8a' not in abis:
    print('STOP: 32-bit ARM native code exists without arm64-v8a counterpart.')
    failed = True
if 'x86' in abis and 'x86_64' not in abis:
    print('STOP: 32-bit x86 native code exists without x86_64 counterpart.')
    failed = True

if not any(abi in abis for abi in required_16kb_abis):
    print('STOP: native libraries exist but no supported 64-bit ABI (arm64-v8a/x86_64) was found.')
    failed = True

for rel, aligns in info_32:
    if min(aligns) < 16384:
        print(f'INFO: 32-bit library uses 4 KB ELF alignment (allowed by this Play 64-bit gate): {rel}; PT_LOAD alignments={aligns}')

if failed:
    raise SystemExit(1)

print('16 KB / 64-bit native check passed.')
print('ABIs in artifact: ' + ', '.join(sorted(abis)))
print('64-bit libraries checked: ' + str(len(checked_64)))
PY
