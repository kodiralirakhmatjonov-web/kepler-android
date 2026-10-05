# iumrah Android v36 — GitHub Actions build failure fix

Observed failure in `logs_101046265308.zip`:

- Job stopped in **Android SDK**, before Gradle and before Billing dependency resolution.
- `android-actions/setup-android@v3` invoked `sdkmanager tools`.
- Google no longer serves the legacy SDK package named `tools`.
- Runner returned `Warning: Failed to find package 'tools'` and exit code 1.

Fix:

- `android-actions/setup-android@v3` -> `android-actions/setup-android@v4`.
- `packages: ''` prevents unnecessary package installation in setup action.
- API install is explicit: `platform-tools`, `platforms;android-36`, `build-tools;36.0.0`.
- Signing secrets accept both the current `PLAY_*` names and the older `IUMRAH_UPLOAD_*` names.
- All v35 non-Billing dependency/AAB gates remain enabled.

No package/application/signing identity was changed.
