#!/usr/bin/env bash
set -Eeuo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
ROOT_DIR="$(cd "$SCRIPT_DIR/.." && pwd)"
TEMPLATE="$ROOT_DIR/docs/github-actions/iumrah-emulator-live.yml"
DEST="$ROOT_DIR/.github/workflows/iumrah-emulator-live.yml"
MODE="${1:-install}"

fail() { printf 'ERROR: %s\n' "$*" >&2; exit 1; }

[ -f "$TEMPLATE" ] || fail "Workflow template not found: $TEMPLATE"
mkdir -p "$(dirname "$DEST")"
cp "$TEMPLATE" "$DEST"
printf 'Installed workflow: %s\n' "$DEST"

git -C "$ROOT_DIR" diff --check

if [ "$MODE" = "--push" ]; then
  [ -d "$ROOT_DIR/.git" ] || fail 'Not a Git checkout'
  branch="$(git -C "$ROOT_DIR" branch --show-current)"
  [ "$branch" = "main" ] || fail "Run this from branch main (current: $branch)"

  if [ -n "$(git -C "$ROOT_DIR" status --porcelain --untracked-files=no)" ]; then
    # Permit the workflow file itself; refuse unrelated tracked edits so this
    # helper never commits someone's in-progress Android Studio work.
    dirty="$(git -C "$ROOT_DIR" status --porcelain --untracked-files=no | grep -vE '^[ MARCUD?!]{1,2} \.github/workflows/iumrah-emulator-live\.yml$' || true)"
    [ -z "$dirty" ] || fail 'Other tracked files are modified. Commit/stash them before using --push.'
  fi

  git -C "$ROOT_DIR" add .github/workflows/iumrah-emulator-live.yml
  if git -C "$ROOT_DIR" diff --cached --quiet; then
    printf 'Workflow already matches repository version. Nothing to push.\n'
    exit 0
  fi
  git -C "$ROOT_DIR" commit -m 'Add iumrah local emulator workflow'
  git -C "$ROOT_DIR" push origin main
  printf 'Pushed workflow to origin/main.\n'
else
  cat <<'TXT'
Next:
  git add .github/workflows/iumrah-emulator-live.yml
  git commit -m "Add iumrah local emulator workflow"
  git push origin main

Or let this helper do those three commands after you make sure your tracked worktree is clean:
  bash scripts/install-iumrah-emulator-workflow.sh --push
TXT
fi
