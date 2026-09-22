#!/usr/bin/env bash
set -e
ROOT="$(cd "$(dirname "$0")" && pwd)"
cd "$ROOT"
exec bash scripts/iumrah-live-dev.sh watch
