#!/usr/bin/env bash
set -euo pipefail
ROOT="$(cd "$(dirname "$0")/.." && pwd)"
test -f "$ROOT/docs/github-actions/iumrah-emulator-live.yml"
test -f "$ROOT/scripts/iumrah-emulator-deploy.sh"
test -f "$ROOT/scripts/install-iumrah-emulator-workflow.sh"
grep -Fq 'runs-on: [self-hosted, macOS, iumrah-emulator]' "$ROOT/docs/github-actions/iumrah-emulator-live.yml"
grep -Fq "workflows: ['iumrah Android CI']" "$ROOT/docs/github-actions/iumrah-emulator-live.yml"
grep -Fq 'persist-credentials: false' "$ROOT/docs/github-actions/iumrah-emulator-live.yml"
grep -Fq 'iumrah-live-dev.sh" once' "$ROOT/scripts/iumrah-emulator-deploy.sh"
echo 'Emulator Actions workflow files: OK'
