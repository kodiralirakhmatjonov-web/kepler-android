# iumrah — GitHub Actions → local Android Emulator

## Goal

After one-time setup, a source change on `main` is built on the MacBook by a repository-level GitHub Actions self-hosted runner. The job installs the debug APK on the already-running Android Emulator and relaunches `com.iumrah.beta/.MainActivity`.

Daily loop:

`source change → GitHub → self-hosted Mac runner → Gradle installDebug → ADB → Pixel 4a`

No watcher Terminal is required after the runner is installed as a macOS service.

## Why workflow_run exists

The existing `iumrah Android CI` update bot applies `iumrah-android-update-*.zip` and commits the extracted files with `[skip ci]`. The emulator workflow therefore also listens for successful completion of `iumrah Android CI`, then checks out the latest `main`. Direct source pushes to `main` trigger the emulator workflow normally.

## One-time repository setup

The existing ZIP patch bot intentionally refuses to extract files directly into `.github/workflows/`. This update does not bypass that protection. After this patch is applied, install the reviewed workflow explicitly from the Mac:

```bash
bash scripts/install-iumrah-emulator-workflow.sh --push
```

The source template is `docs/github-actions/iumrah-emulator-live.yml`.

## One-time GitHub runner setup

1. Repository → **Settings → Actions → Runners → New self-hosted runner**.
2. Choose **macOS** and **x64** for the Intel MacBook.
3. On the Mac, run GitHub's displayed download/extract commands.
4. On the displayed `./config.sh ...` command, append:

   ```text
   --name iumrah-macbook --labels iumrah-emulator
   ```

5. After configuration succeeds, install and start the macOS runner service from the runner directory:

   ```bash
   ./svc.sh install
   ./svc.sh start
   ./svc.sh status
   ```

6. GitHub should show the runner **Online / Idle** with labels including `self-hosted`, `macOS`, and `iumrah-emulator`.

The runner registration token displayed by GitHub is short-lived. Do not store it in the repository.

## Emulator requirement

Keep the Pixel 4a emulator running while reviewing UI. The job intentionally fails with a clear error when no emulator is visible to ADB; it does not silently install to an unrelated physical Android device.

The deploy script selects the first online `emulator-*` serial. To pin a serial later, define `IUMRAH_ADB_SERIAL` in the workflow or runner environment.

## What the job does

- checks out latest `main`;
- reuses the existing `scripts/iumrah-live-dev.sh once` path for Java/Gradle/SDK detection;
- runs incremental `:app:installDebug`;
- force-stops and relaunches Iumrah;
- captures an emulator screenshot;
- uploads the screenshot and full build log as a short-lived Actions artifact;
- writes a compact pass/fail summary in the Actions run.

## Manual deploy

Actions → **iumrah Emulator Live** → **Run workflow**.

This is useful when the emulator was off during the previous automatic run.

## Security boundary

Use the self-hosted Mac runner only with a private repository and tightly controlled write access. The workflow has no `pull_request` trigger and uses `contents: read`, but any code pushed to `main` executes on the self-hosted Mac during Gradle build. GitHub recommends self-hosted runners primarily for private repositories because untrusted workflow/code execution can persistently compromise a host.
