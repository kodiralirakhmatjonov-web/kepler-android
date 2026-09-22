#!/usr/bin/env bash
cd "$(dirname "$0")"
bash scripts/iumrah-emulator-deploy.sh doctor
printf '\nPress Enter to close...'
read -r _
