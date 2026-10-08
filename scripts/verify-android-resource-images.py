#!/usr/bin/env python3
from pathlib import Path
import sys

ROOT = Path(__file__).resolve().parents[1]
RES = ROOT / 'app' / 'src' / 'main' / 'res'

EXPECTED = {
    '.png': b'\x89PNG\r\n\x1a\n',
    '.jpg': b'\xff\xd8\xff',
    '.jpeg': b'\xff\xd8\xff',
    '.webp': b'RIFF',
}

errors = []
checked = 0
for path in sorted(RES.rglob('*')):
    if not path.is_file() or path.suffix.lower() not in EXPECTED:
        continue
    checked += 1
    raw = path.read_bytes()[:16]
    ext = path.suffix.lower()
    ok = raw.startswith(EXPECTED[ext])
    if ext == '.webp':
        ok = raw.startswith(b'RIFF') and len(raw) >= 12 and raw[8:12] == b'WEBP'
    if not ok:
        errors.append(f'{path.relative_to(ROOT)}: extension {ext} does not match file signature')

if errors:
    print('STOP: invalid/mislabeled Android image resources detected:', file=sys.stderr)
    for e in errors:
        print(f'  - {e}', file=sys.stderr)
    raise SystemExit(1)
print(f'Android resource image signatures verified: {checked} files.')
