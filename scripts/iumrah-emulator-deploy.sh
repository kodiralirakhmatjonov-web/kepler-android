#!/usr/bin/env bash
set -Eeuo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
ROOT_DIR="$(cd "$SCRIPT_DIR/.." && pwd)"
OUT_DIR="$ROOT_DIR/artifacts/emulator-live"
LOG_FILE="$OUT_DIR/build-install.log"
SCREENSHOT_FILE="$OUT_DIR/iumrah-emulator.png"
APP_ID="${IUMRAH_APP_ID:-com.iumrah.beta}"
ACTIVITY="${IUMRAH_ACTIVITY:-.MainActivity}"
ADB_BIN=""
DEVICE_SERIAL=""

mkdir -p "$OUT_DIR"
: > "$LOG_FILE"

log() { printf '[iumrah-emulator] %s\n' "$*"; }
fail() { printf '[iumrah-emulator] ERROR: %s\n' "$*" >&2; exit 1; }

find_adb() {
  local candidate
  for candidate in \
    "${IUMRAH_ADB_PATH:-}" \
    "${ANDROID_HOME:-}/platform-tools/adb" \
    "${ANDROID_SDK_ROOT:-}/platform-tools/adb" \
    "$HOME/Library/Android/sdk/platform-tools/adb"; do
    [ -n "$candidate" ] || continue
    [ "$candidate" != "/platform-tools/adb" ] || continue
    if [ -x "$candidate" ]; then
      ADB_BIN="$candidate"
      return 0
    fi
  done
  if command -v adb >/dev/null 2>&1; then
    ADB_BIN="$(command -v adb)"
    return 0
  fi
  return 1
}

select_emulator() {
  find_adb || return 1
  "$ADB_BIN" start-server >/dev/null 2>&1 || true

  if [ -n "${IUMRAH_ADB_SERIAL:-}" ]; then
    if "$ADB_BIN" devices | awk 'NR>1 && $2=="device" {print $1}' | grep -Fxq "$IUMRAH_ADB_SERIAL"; then
      DEVICE_SERIAL="$IUMRAH_ADB_SERIAL"
      return 0
    fi
    return 1
  fi

  DEVICE_SERIAL="$("$ADB_BIN" devices | awk 'NR>1 && $2=="device" && $1 ~ /^emulator-/ {print $1; exit}')"
  [ -n "$DEVICE_SERIAL" ]
}

write_summary_failure() {
  [ -n "${GITHUB_STEP_SUMMARY:-}" ] || return 0
  {
    echo '## ❌ iumrah emulator deploy failed'
    echo
    echo 'The self-hosted Mac runner was reached, but the Android build/install failed.'
    echo
    echo '```text'
    grep -E '(^e: |error:|Unresolved reference|FAILURE:|What went wrong:|Could not resolve|Compilation error|BUILD FAILED)' "$LOG_FILE" | tail -80 || tail -80 "$LOG_FILE"
    echo '```'
  } >> "$GITHUB_STEP_SUMMARY"
}

write_summary_success() {
  [ -n "${GITHUB_STEP_SUMMARY:-}" ] || return 0
  local sha version
  sha="$(git -C "$ROOT_DIR" rev-parse --short HEAD 2>/dev/null || printf unknown)"
  version="$("$ADB_BIN" -s "$DEVICE_SERIAL" shell dumpsys package "$APP_ID" 2>/dev/null | sed -n 's/^[[:space:]]*versionName=//p' | head -1 | tr -d '\r')"
  {
    echo '## ✅ iumrah installed on local emulator'
    echo
    echo "- Commit: \`$sha\`"
    echo "- Device: \`$DEVICE_SERIAL\`"
    echo "- Package: \`$APP_ID\`"
    [ -n "$version" ] && echo "- Version: \`$version\`"
    echo '- App was force-stopped and relaunched after installation.'
  } >> "$GITHUB_STEP_SUMMARY"
}

doctor() {
  [ -f "$ROOT_DIR/scripts/iumrah-live-dev.sh" ] || fail 'scripts/iumrah-live-dev.sh is missing'
  log "Runner user: $(id -un)"
  log "Home: $HOME"
  log "Project: $ROOT_DIR"
  bash "$ROOT_DIR/scripts/iumrah-live-dev.sh" doctor
  if select_emulator; then
    log "Emulator online: $DEVICE_SERIAL"
  else
    fail 'No running Android emulator is visible to ADB. Start Pixel 4a before the workflow runs.'
  fi
}

deploy() {
  [ -f "$ROOT_DIR/scripts/iumrah-live-dev.sh" ] || fail 'scripts/iumrah-live-dev.sh is missing'
  select_emulator || fail 'No running Android emulator. Keep Pixel 4a open on the Mac.'
  log "Deploy target: $DEVICE_SERIAL"

  set +e
  bash "$ROOT_DIR/scripts/iumrah-live-dev.sh" once 2>&1 | tee "$LOG_FILE"
  build_status=${PIPESTATUS[0]}
  set -e

  if [ "$build_status" -ne 0 ]; then
    write_summary_failure
    exit "$build_status"
  fi

  # A deterministic relaunch makes visual review obvious even when Android keeps
  # the previous task in memory after package replacement.
  "$ADB_BIN" -s "$DEVICE_SERIAL" shell am force-stop "$APP_ID" >/dev/null 2>&1 || true
  "$ADB_BIN" -s "$DEVICE_SERIAL" shell am start -n "$APP_ID/$ACTIVITY" >/dev/null 2>&1 || \
    "$ADB_BIN" -s "$DEVICE_SERIAL" shell monkey -p "$APP_ID" -c android.intent.category.LAUNCHER 1 >/dev/null 2>&1

  sleep 2
  if "$ADB_BIN" -s "$DEVICE_SERIAL" exec-out screencap -p > "$SCREENSHOT_FILE" 2>/dev/null; then
    log "Screenshot saved: $SCREENSHOT_FILE"
  else
    rm -f "$SCREENSHOT_FILE"
    log 'Screenshot capture failed; app installation still succeeded.'
  fi

  "$ADB_BIN" -s "$DEVICE_SERIAL" shell pm path "$APP_ID" >/dev/null 2>&1 || fail "Package $APP_ID is not installed after build"
  write_summary_success
  log 'Done. iumrah is open in the emulator.'
}

case "${1:-deploy}" in
  deploy) deploy ;;
  doctor) doctor ;;
  *) echo "Usage: bash scripts/iumrah-emulator-deploy.sh [deploy|doctor]"; exit 2 ;;
esac
