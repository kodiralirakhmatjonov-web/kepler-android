# iumrah Android — Live Development on Mac

This repository includes a macOS watcher for fast UI iteration against an already-running Android emulator.

## What it does

- watches Android source/resources locally every second;
- optionally polls the current Git branch from `origin` every 4 seconds;
- never uses `git reset --hard` and never overwrites a dirty local working tree;
- uses the Gradle daemon and the project's existing build cache for incremental builds;
- runs `:app:installDebug`, preserving app data where Android allows it;
- relaunches `com.iumrah.beta/.MainActivity` automatically after a successful install;
- keeps watching after a compile error and retries after the next source change.

This is the reliable path for changes arriving from GitHub. Android Studio Compose Live Edit can still be enabled as an additional acceleration for edits performed directly inside the IDE.

## One-time setup

Open the repository clone in Android Studio, start the desired Android emulator, then run from Terminal at the repository root:

```bash
bash scripts/iumrah-live-dev.sh setup
```

The project currently has no Gradle wrapper. The setup script therefore downloads Gradle 9.6.0 from `services.gradle.org`, downloads the official `.sha256` file, verifies the archive, and stores it under `~/.iumrah-live-dev/`. It does not modify the repository with downloaded Gradle binaries.

Then start live mode:

```bash
bash scripts/iumrah-live-dev.sh watch
```

After `setup`, `IUMRAH_LIVE.command` is also marked executable and can be double-clicked in Finder to start the watcher.

## GitHub sync

By default the watcher follows the Git branch currently checked out when it starts. To pin a branch:

```bash
IUMRAH_LIVE_BRANCH=main bash scripts/iumrah-live-dev.sh watch
```

When a new remote commit appears and the local tracked working tree is clean, the watcher uses only a fast-forward merge. If local tracked files are modified, remote pulling pauses but local build/install continues.

## Local direct-edit mode

No GitHub commit is needed for local changes. Any change under `app/src` or the main Gradle configuration files changes the source fingerprint and triggers an incremental install automatically. This is useful when Android Studio, a local coding agent, or ChatGPT desktop/Work edits the clone directly.

## Compose Live Edit

In Android Studio, enable Live Edit / automatic edit pushing if available in your IDE version. It can make supported Compose-only edits appear faster while the app is running. Do not rely on it for externally pulled Git changes, resource additions, manifest changes, dependencies, or broad refactors; the repository watcher handles those through Gradle + ADB.

## Commands

```bash
bash scripts/iumrah-live-dev.sh doctor
bash scripts/iumrah-live-dev.sh once
bash scripts/iumrah-live-dev.sh watch
bash scripts/iumrah-live-dev.sh stop
```

Useful overrides:

```bash
IUMRAH_ADB_SERIAL=emulator-5554 bash scripts/iumrah-live-dev.sh watch
IUMRAH_LIVE_REMOTE_INTERVAL=2 bash scripts/iumrah-live-dev.sh watch
IUMRAH_LIVE_NOTIFY=1 bash scripts/iumrah-live-dev.sh watch
```

`IUMRAH_LIVE_NOTIFY=1` enables a macOS notification after each successful emulator update.

## Expected development loop

1. Keep Android Emulator running.
2. Keep `iumrah-live-dev.sh watch` running in one Terminal window.
3. Change source locally or push a commit to the watched GitHub branch.
4. The watcher detects it, performs an incremental debug install, and reopens Iumrah automatically.
5. If a build fails, the bad source stays visible in Terminal; the next source change triggers a new attempt.

This removes the repeated manual cycle of downloading a ZIP, rebuilding by hand, installing the APK, and reopening the app.
