#!/usr/bin/env bash
set -euo pipefail
ROOT="$(cd "$(dirname "$0")/.." && pwd)"
cd "$ROOT"
SRC=docs/release/iumrah-play-release.yml
DST=.github/workflows/iumrah-play-release.yml
test -s "$SRC" || { echo "STOP: $SRC missing"; exit 1; }
mkdir -p .github/workflows
cp "$SRC" "$DST"
echo "Installed $DST"
echo "Commit and push it once. The automatic ZIP extractor intentionally protects .github/workflows/."
