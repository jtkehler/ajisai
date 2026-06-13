#!/usr/bin/env bash
set -euo pipefail

PACKAGE="com.jtkehler.ajisai"
TEST_PACKAGE="com.jtkehler.ajisai.test"
RUNNER="androidx.test.runner.AndroidJUnitRunner"

URL="https://github.com/yomidevs/jmdict-yomitan/releases/latest/download/JMdict_english_with_examples.zip"
ZIP_NAME="JMdict_english_with_examples.zip"
LOCAL_ZIP="build/e2e/assets/${ZIP_NAME}"
DEVICE_TMP="/data/local/tmp/${ZIP_NAME}"
APP_ZIP_REL="files/e2e/${ZIP_NAME}"
LOG_FILE="build/e2e/logcat-real-jmdict.txt"

capture_logcat() {
  mkdir -p build/e2e
  adb logcat -d |
    grep -iE "FATAL EXCEPTION|AndroidRuntime|${PACKAGE}|Ajisai|hoshi|dictionary|WorkManager" \
      >"$LOG_FILE" || true
}

run_instrumentation_test() {
  local test_class="$1"
  local result_file="$2"

  adb shell am instrument -w -r \
    -e ajisaiRealJmdict true \
    -e class "$test_class" \
    "$TEST_PACKAGE/$RUNNER" | tee "$result_file"

  if grep -qE "INSTRUMENTATION_STATUS_CODE: -2|shortMsg=Process crashed|FAILURES!!!" "$result_file" || \
    ! grep -q "INSTRUMENTATION_CODE: -1" "$result_file"; then
    echo "Instrumentation test failed: ${test_class}"
    return 1
  fi
}

trap capture_logcat EXIT

mkdir -p build/e2e/assets

echo "== Connected devices =="
adb devices -l
DEVICE_COUNT="$(adb devices | awk 'NR > 1 && $2 == "device" { count++ } END { print count + 0 }')"
if [[ "$DEVICE_COUNT" -ne 1 ]]; then
  echo "Expected exactly one authorized adb device; found ${DEVICE_COUNT}."
  exit 1
fi

echo "== Downloading/caching JMdict fixture =="
if [[ ! -f "$LOCAL_ZIP" || "${E2E_REFRESH_JMDICT:-0}" == "1" ]]; then
  PARTIAL_ZIP="${LOCAL_ZIP}.partial"
  curl -L --fail --retry 3 -o "$PARTIAL_ZIP" "$URL"
  mv "$PARTIAL_ZIP" "$LOCAL_ZIP"
fi

echo "== Building and installing debug app/test APK =="
./gradlew installDebug installDebugAndroidTest

echo "== Clearing app data before import =="
adb shell pm clear "$PACKAGE" || true

echo "== Copying JMdict zip into app-private files =="
adb push "$LOCAL_ZIP" "$DEVICE_TMP"
adb shell chmod 644 "$DEVICE_TMP"
adb shell run-as "$PACKAGE" mkdir -p files/e2e
adb shell run-as "$PACKAGE" cp "$DEVICE_TMP" "$APP_ZIP_REL"

echo "== Clearing logcat =="
adb logcat -c || true

echo "== Phase 1: real JMdict import =="
run_instrumentation_test \
  com.jtkehler.ajisai.RealJmdictImportE2ETest \
  build/e2e/instrumentation-real-jmdict-import.txt

echo "== Clear cache/code_cache only, preserve files/shared_prefs =="
adb shell run-as "$PACKAGE" rm -rf cache code_cache
adb shell run-as "$PACKAGE" mkdir -p cache code_cache

echo "== Force-stop app to simulate cold restart =="
adb shell am force-stop "$PACKAGE"

echo "== Phase 2: post-restart Dictionaries render =="
run_instrumentation_test \
  com.jtkehler.ajisai.PostRestartDictionariesRenderE2ETest \
  build/e2e/instrumentation-real-jmdict-restart.txt

echo "== Capturing filtered logcat =="
capture_logcat
trap - EXIT

echo "== Real JMdict E2E passed =="
echo "Filtered logcat: ${LOG_FILE}"
