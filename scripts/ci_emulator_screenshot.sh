#!/usr/bin/env bash
set -euo pipefail

# Run this after the Android emulator starts in CI.

APK=${APK:-app/build/outputs/apk/debug/app-debug.apk}
OUT=${OUT:-captures/compose-workbench-emulator.png}
PACKAGE=${PACKAGE:-com.example.tlctvscreenshot}
ACTIVITY=${ACTIVITY:-.MainActivity}
LABEL=${LABEL:-compose-workbench}
UI_TEST_MODE=${UI_TEST_MODE:-1}

mkdir -p "$(dirname "$OUT")"
test -s "$APK"
ls -lh "$APK"
adb wait-for-device
adb devices -l
adb shell getprop sys.boot_completed
timeout 180 adb install -r -g -t "$APK"
adb shell pm clear "$PACKAGE" >/dev/null 2>&1 || true
adb shell am force-stop "$PACKAGE" || true
if [[ "$UI_TEST_MODE" == "1" ]]; then
  adb shell am start \
    -n "$PACKAGE/$ACTIVITY" \
    --ez com.example.tlctvscreenshot.UI_TEST_MODE true \
    --es screenshot_label "$LABEL"
else
  adb shell am start -n "$PACKAGE/$ACTIVITY"
fi
sleep 5
adb exec-out screencap -p > "$OUT"
echo "$OUT"
