#!/usr/bin/env bash
set -Eeuo pipefail

# iumrah Android live development bridge for macOS.
# Works in two directions at once:
#   1) local file changes -> incremental Gradle install -> emulator relaunch
#   2) clean Git working tree -> fast-forward current/selected branch from origin -> same install
#
# Designed to be invoked with `bash scripts/iumrah-live-dev.sh ...`, so executable
# permissions are not required after the repository patch bot extracts the file.

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
ROOT_DIR="$(cd "$SCRIPT_DIR/.." && pwd)"

APP_ID="${IUMRAH_APP_ID:-com.iumrah.beta}"
ACTIVITY="${IUMRAH_ACTIVITY:-.MainActivity}"
GRADLE_VERSION="${IUMRAH_GRADLE_VERSION:-9.6.0}"
LOCAL_INTERVAL="${IUMRAH_LIVE_LOCAL_INTERVAL:-1}"
REMOTE_INTERVAL="${IUMRAH_LIVE_REMOTE_INTERVAL:-4}"
TOOLS_DIR="${IUMRAH_LIVE_TOOLS_DIR:-$HOME/.iumrah-live-dev}"
NOTIFY="${IUMRAH_LIVE_NOTIFY:-0}"

ADB_BIN=""
GRADLE_BIN=""
DEVICE_SERIAL=""
WATCH_BRANCH=""
LAST_REMOTE_POLL=0
DEPLOYED_FINGERPRINT=""
FAILED_FINGERPRINT=""
REMOTE_WARNING=""

bold='\033[1m'
green='\033[32m'
yellow='\033[33m'
red='\033[31m'
dim='\033[2m'
reset='\033[0m'

stamp() { date '+%H:%M:%S'; }
log() { printf "%b[%s] %s%b\n" "$dim" "$(stamp)" "$*" "$reset"; }
ok() { printf "%b[%s] ✓ %s%b\n" "$green" "$(stamp)" "$*" "$reset"; }
warn() { printf "%b[%s] ! %s%b\n" "$yellow" "$(stamp)" "$*" "$reset"; }
fail() { printf "%b[%s] ✕ %s%b\n" "$red" "$(stamp)" "$*" "$reset" >&2; exit 1; }

usage() {
  cat <<'TXT'
iumrah Android Live Dev

Usage:
  bash scripts/iumrah-live-dev.sh setup   # one-time Mac setup + initial install when emulator is running
  bash scripts/iumrah-live-dev.sh watch   # local + GitHub watcher; rebuild/install automatically
  bash scripts/iumrah-live-dev.sh once    # build/install/relaunch current source once
  bash scripts/iumrah-live-dev.sh doctor  # check Java / Android SDK / emulator / Gradle / Git
  bash scripts/iumrah-live-dev.sh stop    # stop Gradle daemon used by this project

Optional environment variables:
  IUMRAH_LIVE_BRANCH=main
  IUMRAH_ADB_SERIAL=emulator-5554
  IUMRAH_LIVE_LOCAL_INTERVAL=1
  IUMRAH_LIVE_REMOTE_INTERVAL=4
  IUMRAH_LIVE_NOTIFY=1

The watcher never hard-resets local work. Remote sync is skipped while the Git tree is dirty.
TXT
}

require_project() {
  [ -f "$ROOT_DIR/settings.gradle.kts" ] || fail "settings.gradle.kts not found at $ROOT_DIR"
  [ -f "$ROOT_DIR/app/build.gradle.kts" ] || fail "app/build.gradle.kts not found"
  [ -f "$ROOT_DIR/app/src/main/AndroidManifest.xml" ] || fail "AndroidManifest.xml not found"
}

find_java() {
  if [ -n "${JAVA_HOME:-}" ] && [ -x "$JAVA_HOME/bin/java" ]; then
    return 0
  fi

  if [ -x /usr/libexec/java_home ]; then
    local j17
    j17="$(/usr/libexec/java_home -v 17 2>/dev/null || true)"
    if [ -n "$j17" ] && [ -x "$j17/bin/java" ]; then
      export JAVA_HOME="$j17"
      return 0
    fi
  fi

  local candidate
  for candidate in \
    "/Applications/Android Studio.app/Contents/jbr/Contents/Home" \
    "$HOME/Applications/Android Studio.app/Contents/jbr/Contents/Home"; do
    if [ -x "$candidate/bin/java" ]; then
      export JAVA_HOME="$candidate"
      return 0
    fi
  done

  if command -v java >/dev/null 2>&1; then
    return 0
  fi

  return 1
}

find_adb() {
  if [ -n "${IUMRAH_ADB_PATH:-}" ] && [ -x "$IUMRAH_ADB_PATH" ]; then
    ADB_BIN="$IUMRAH_ADB_PATH"
    return 0
  fi

  local candidate
  for candidate in \
    "${ANDROID_HOME:-}/platform-tools/adb" \
    "${ANDROID_SDK_ROOT:-}/platform-tools/adb" \
    "$HOME/Library/Android/sdk/platform-tools/adb"; do
    if [ "$candidate" != "/platform-tools/adb" ] && [ -x "$candidate" ]; then
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

select_device() {
  [ -n "$ADB_BIN" ] || find_adb || return 1

  if [ -n "${IUMRAH_ADB_SERIAL:-}" ]; then
    if "$ADB_BIN" devices | awk 'NR > 1 && $2 == "device" {print $1}' | grep -Fxq "$IUMRAH_ADB_SERIAL"; then
      DEVICE_SERIAL="$IUMRAH_ADB_SERIAL"
      return 0
    fi
    warn "Requested ADB device $IUMRAH_ADB_SERIAL is not online."
    return 1
  fi

  DEVICE_SERIAL="$("$ADB_BIN" devices | awk 'NR > 1 && $2 == "device" && $1 ~ /^emulator-/ {print $1; exit}')"
  if [ -z "$DEVICE_SERIAL" ]; then
    DEVICE_SERIAL="$("$ADB_BIN" devices | awk 'NR > 1 && $2 == "device" {print $1; exit}')"
  fi

  [ -n "$DEVICE_SERIAL" ]
}

ensure_gradle() {
  if [ -n "${IUMRAH_GRADLE_PATH:-}" ] && [ -x "$IUMRAH_GRADLE_PATH" ]; then
    GRADLE_BIN="$IUMRAH_GRADLE_PATH"
    return 0
  fi

  if [ -f "$ROOT_DIR/gradlew" ]; then
    chmod +x "$ROOT_DIR/gradlew" 2>/dev/null || true
    if [ -x "$ROOT_DIR/gradlew" ]; then
      GRADLE_BIN="$ROOT_DIR/gradlew"
      return 0
    fi
  fi

  local local_gradle="$TOOLS_DIR/gradle-$GRADLE_VERSION/bin/gradle"
  if [ -x "$local_gradle" ]; then
    GRADLE_BIN="$local_gradle"
    return 0
  fi

  command -v curl >/dev/null 2>&1 || fail "curl is required to bootstrap Gradle $GRADLE_VERSION"
  command -v unzip >/dev/null 2>&1 || fail "unzip is required to bootstrap Gradle $GRADLE_VERSION"
  command -v shasum >/dev/null 2>&1 || fail "shasum is required to verify the Gradle download"

  mkdir -p "$TOOLS_DIR"
  local zip="$TOOLS_DIR/gradle-$GRADLE_VERSION-bin.zip"
  local sha_file="$zip.sha256"
  local base="https://services.gradle.org/distributions/gradle-$GRADLE_VERSION-bin.zip"

  log "Gradle wrapper is absent; bootstrapping Gradle $GRADLE_VERSION into $TOOLS_DIR"
  curl -fL --retry 3 --connect-timeout 15 "$base" -o "$zip"
  curl -fL --retry 3 --connect-timeout 15 "$base.sha256" -o "$sha_file"

  local expected actual
  expected="$(tr -d '[:space:]' < "$sha_file")"
  actual="$(shasum -a 256 "$zip" | awk '{print $1}')"
  [ "$expected" = "$actual" ] || fail "Gradle archive checksum mismatch"

  rm -rf "$TOOLS_DIR/gradle-$GRADLE_VERSION"
  unzip -q "$zip" -d "$TOOLS_DIR"
  rm -f "$zip" "$sha_file"
  [ -x "$local_gradle" ] || fail "Gradle bootstrap completed but binary is missing"
  GRADLE_BIN="$local_gradle"
  ok "Gradle $GRADLE_VERSION ready"
}

resolve_branch() {
  if [ -n "${IUMRAH_LIVE_BRANCH:-}" ]; then
    WATCH_BRANCH="$IUMRAH_LIVE_BRANCH"
    return
  fi

  if [ -d "$ROOT_DIR/.git" ]; then
    WATCH_BRANCH="$(git -C "$ROOT_DIR" branch --show-current 2>/dev/null || true)"
  fi
  [ -n "$WATCH_BRANCH" ] || WATCH_BRANCH="main"
}

source_files() {
  if [ -d "$ROOT_DIR/app/src" ]; then
    find "$ROOT_DIR/app/src" -type f -print
  fi

  local f
  for f in \
    "$ROOT_DIR/build.gradle.kts" \
    "$ROOT_DIR/settings.gradle.kts" \
    "$ROOT_DIR/gradle.properties" \
    "$ROOT_DIR/app/build.gradle.kts" \
    "$ROOT_DIR/app/proguard-rules.pro"; do
    [ -f "$f" ] && printf '%s\n' "$f"
  done
}

file_metadata() {
  local file="$1"
  if stat -f '%m:%z:%N' "$file" >/dev/null 2>&1; then
    stat -f '%m:%z:%N' "$file"
  else
    stat -c '%Y:%s:%n' "$file"
  fi
}

fingerprint() {
  # Hash text/code exactly; use cheap size+mtime metadata for large binary media.
  # This keeps a one-second polling loop light even with tens of MB of videos/images.
  source_files | LC_ALL=C sort -u | while IFS= read -r file; do
    [ -f "$file" ] || continue
    case "$file" in
      *.kt|*.kts|*.xml|*.json|*.properties|*.pro|*.txt|*.md)
        shasum -a 256 "$file"
        ;;
      *)
        file_metadata "$file"
        ;;
    esac
  done | shasum -a 256 | awk '{print $1}'
}

notify_success() {
  [ "$NOTIFY" = "1" ] || return 0
  command -v osascript >/dev/null 2>&1 || return 0
  osascript -e 'display notification "Emulator updated" with title "iumrah Android Live"' >/dev/null 2>&1 || true
}

launch_app() {
  [ -n "$DEVICE_SERIAL" ] || select_device || return 1
  "$ADB_BIN" -s "$DEVICE_SERIAL" shell am start -n "$APP_ID/$ACTIVITY" >/dev/null 2>&1 || \
    "$ADB_BIN" -s "$DEVICE_SERIAL" shell monkey -p "$APP_ID" -c android.intent.category.LAUNCHER 1 >/dev/null 2>&1
}

build_install() {
  select_device || {
    warn "No online Android device/emulator. Start the emulator; watcher will keep running."
    return 2
  }
  ensure_gradle
  find_java || fail "Java not found. Install/open Android Studio or provide JAVA_HOME."

  printf "\n%b[%s] Building + installing → %s%b\n" "$bold" "$(stamp)" "$DEVICE_SERIAL" "$reset"
  if (cd "$ROOT_DIR" && "$GRADLE_BIN" --daemon --console=plain --stacktrace :app:installDebug); then
    launch_app || warn "APK installed, but automatic app launch failed."
    ok "Emulator updated from $(git -C "$ROOT_DIR" rev-parse --short HEAD 2>/dev/null || printf 'local source')"
    notify_success
    return 0
  fi

  warn "Build/install failed. Fix the source or dependency error; next source change will trigger another attempt."
  return 1
}

is_git_clean() {
  [ -d "$ROOT_DIR/.git" ] || return 1
  [ -z "$(git -C "$ROOT_DIR" status --porcelain --untracked-files=no 2>/dev/null)" ]
}

sync_remote_once() {
  [ -d "$ROOT_DIR/.git" ] || return 0
  git -C "$ROOT_DIR" remote get-url origin >/dev/null 2>&1 || return 0

  resolve_branch
  local current_branch
  current_branch="$(git -C "$ROOT_DIR" branch --show-current 2>/dev/null || true)"
  if [ "$current_branch" != "$WATCH_BRANCH" ]; then
    if [ "$REMOTE_WARNING" != "branch" ]; then
      warn "Remote sync paused: current branch '$current_branch' differs from watched '$WATCH_BRANCH'."
      REMOTE_WARNING="branch"
    fi
    return 0
  fi

  if ! is_git_clean; then
    if [ "$REMOTE_WARNING" != "dirty" ]; then
      warn "Remote sync paused while local tracked files are modified; local live rebuild remains active."
      REMOTE_WARNING="dirty"
    fi
    return 0
  fi

  if ! git -C "$ROOT_DIR" fetch origin "$WATCH_BRANCH" --quiet; then
    if [ "$REMOTE_WARNING" != "network" ]; then
      warn "Git fetch failed; local live rebuild remains active."
      REMOTE_WARNING="network"
    fi
    return 0
  fi

  REMOTE_WARNING=""
  local local_sha remote_sha
  local_sha="$(git -C "$ROOT_DIR" rev-parse HEAD)"
  remote_sha="$(git -C "$ROOT_DIR" rev-parse "origin/$WATCH_BRANCH" 2>/dev/null || true)"
  [ -n "$remote_sha" ] || return 0
  [ "$local_sha" != "$remote_sha" ] || return 0

  if git -C "$ROOT_DIR" merge-base --is-ancestor "$local_sha" "$remote_sha"; then
    log "New GitHub commit detected on $WATCH_BRANCH; applying fast-forward."
    if git -C "$ROOT_DIR" merge --ff-only "origin/$WATCH_BRANCH" >/dev/null; then
      ok "Synced $(git -C "$ROOT_DIR" rev-parse --short HEAD)"
    else
      warn "Fast-forward failed; no local files were reset."
    fi
  elif git -C "$ROOT_DIR" merge-base --is-ancestor "$remote_sha" "$local_sha"; then
    # Local commits are ahead; nothing to pull. Local file watcher handles deployment.
    return 0
  else
    warn "Local and origin/$WATCH_BRANCH diverged. Resolve Git manually; live local rebuild remains active."
  fi
}

doctor() {
  require_project
  printf "%biumrah Android Live Dev doctor%b\n" "$bold" "$reset"

  if find_java; then
    local java_bin
    if [ -n "${JAVA_HOME:-}" ] && [ -x "$JAVA_HOME/bin/java" ]; then
      java_bin="$JAVA_HOME/bin/java"
    else
      java_bin="$(command -v java)"
    fi
    ok "Java: $("$java_bin" -version 2>&1 | head -1)"
  else
    warn "Java not found"
  fi

  if find_adb; then
    ok "ADB: $ADB_BIN"
    if select_device; then
      ok "Device: $DEVICE_SERIAL"
    else
      warn "No online device/emulator"
    fi
  else
    warn "ADB not found (expected under ~/Library/Android/sdk/platform-tools/adb)"
  fi

  if [ -x "$ROOT_DIR/gradlew" ]; then
    ok "Gradle: project wrapper"
  elif [ -x "$TOOLS_DIR/gradle-$GRADLE_VERSION/bin/gradle" ]; then
    ok "Gradle: cached $GRADLE_VERSION"
  else
    warn "Gradle wrapper absent; setup/once/watch will securely download Gradle $GRADLE_VERSION"
  fi

  if [ -d "$ROOT_DIR/.git" ]; then
    resolve_branch
    ok "Git repository: branch $WATCH_BRANCH"
    if git -C "$ROOT_DIR" remote get-url origin >/dev/null 2>&1; then
      ok "GitHub/origin remote configured"
    else
      warn "No origin remote; local live rebuild will still work"
    fi
  else
    warn "No .git directory; local live rebuild works, GitHub auto-sync does not"
  fi
}

setup() {
  require_project
  find_java || fail "Java not found. Install/open Android Studio and rerun setup."
  find_adb || fail "ADB not found. Install Android SDK Platform-Tools from Android Studio."
  ensure_gradle
  chmod +x "$ROOT_DIR/scripts/iumrah-live-dev.sh" 2>/dev/null || true
  [ -f "$ROOT_DIR/IUMRAH_LIVE.command" ] && chmod +x "$ROOT_DIR/IUMRAH_LIVE.command" 2>/dev/null || true

  ok "Mac live-development toolchain is ready"
  doctor

  if select_device; then
    log "Running first incremental install to verify the full chain."
    build_install || fail "Initial build/install failed; see Gradle error above."
  else
    warn "Setup finished without install because no emulator/device is online."
    printf "Start the emulator, then run:\n  %bbash scripts/iumrah-live-dev.sh watch%b\n" "$bold" "$reset"
  fi
}

once() {
  require_project
  find_java || fail "Java not found. Install/open Android Studio or set JAVA_HOME."
  find_adb || fail "ADB not found."
  ensure_gradle
  build_install
}

watch() {
  require_project
  find_java || fail "Java not found. Install/open Android Studio or set JAVA_HOME."
  find_adb || fail "ADB not found."
  ensure_gradle
  resolve_branch

  printf "%biumrah LIVE mode%b\n" "$bold" "$reset"
  printf "Project: %s\n" "$ROOT_DIR"
  printf "Git branch: %s  |  local scan: %ss  |  remote scan: %ss\n" "$WATCH_BRANCH" "$LOCAL_INTERVAL" "$REMOTE_INTERVAL"
  printf "Press Ctrl+C to stop. No hard reset is ever performed.\n\n"

  trap 'printf "\n"; log "Live watcher stopped."; exit 0' INT TERM

  sync_remote_once || true
  local current
  current="$(fingerprint)"
  if build_install; then
    DEPLOYED_FINGERPRINT="$current"
    FAILED_FINGERPRINT=""
  else
    FAILED_FINGERPRINT="$current"
  fi
  LAST_REMOTE_POLL=$SECONDS

  while true; do
    if [ $((SECONDS - LAST_REMOTE_POLL)) -ge "$REMOTE_INTERVAL" ]; then
      sync_remote_once || true
      LAST_REMOTE_POLL=$SECONDS
    fi

    current="$(fingerprint)"
    if [ "$current" != "$DEPLOYED_FINGERPRINT" ] && [ "$current" != "$FAILED_FINGERPRINT" ]; then
      log "Source change detected."
      if build_install; then
        DEPLOYED_FINGERPRINT="$current"
        FAILED_FINGERPRINT=""
      else
        FAILED_FINGERPRINT="$current"
      fi
    fi

    sleep "$LOCAL_INTERVAL"
  done
}

stop_daemon() {
  ensure_gradle
  (cd "$ROOT_DIR" && "$GRADLE_BIN" --stop) || true
}

cmd="${1:-help}"
case "$cmd" in
  setup) setup ;;
  watch) watch ;;
  once) once ;;
  doctor) doctor ;;
  stop) stop_daemon ;;
  help|-h|--help) usage ;;
  *) usage; fail "Unknown command: $cmd" ;;
esac
