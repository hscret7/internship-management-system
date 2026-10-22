#!/usr/bin/env bash
# Runs only disposable MySQL fixtures; never reads the live application's db.properties.
set -euo pipefail
cd "$(dirname "$0")/.."
bash scripts/test-be1-registration.sh
bash scripts/test-be1-inapp-notifications.sh
bash scripts/test-be1-meeting-notifications.sh
bash scripts/test-be1-audit.sh
mvn clean package
printf 'BUSINESS AUDIT CHECKS PASS\nBUILD DIR: %s\n' "$PWD"
