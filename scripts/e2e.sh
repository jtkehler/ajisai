#!/usr/bin/env bash
set -euo pipefail

PACKAGE="com.jtkehler.ajisai"
LOG_FILE="build/e2e/logcat-filtered.txt"

capture_logcat() {
  mkdir -p build/e2e
  adb logcat -d |
    grep -iE "FATAL EXCEPTION|AndroidRuntime|${PACKAGE}|Ajisai|hoshi|dictionary|WorkManager" \
      >"$LOG_FILE" || true
}

trap capture_logcat EXIT

echo "== Connected devices =="
adb devices -l

DEVICE_COUNT="$(adb devices | awk 'NR > 1 && $2 == "device" { count++ } END { print count + 0 }')"
if [[ "$DEVICE_COUNT" -ne 1 ]]; then
  echo "Expected exactly one authorized adb device; found ${DEVICE_COUNT}."
  exit 1
fi

echo "== Clearing logcat =="
adb logcat -c || true

echo "== Running unit tests =="
./gradlew test

echo "== Running connected Android tests =="
./gradlew connectedDebugAndroidTest

echo "== Capturing filtered logcat =="
capture_logcat
trap - EXIT

echo "== Done =="
echo "Filtered logcat: ${LOG_FILE}"
echo "Android test report: app/build/reports/androidTests/connected/index.html"
echo "Android test results: app/build/outputs/androidTest-results/connected/"
