#!/usr/bin/env bash
set -euo pipefail

# Intended to run inside a CI job where an Android emulator is already booted.
# Example: reactivecircus/android-emulator-runner script step.

APK=${APK:-app/build/outputs/apk/debug/app-debug.apk}
OUT=${OUT:-captures/compose-workbench-emulator.png}
PACKAGE=${PACKAGE:-com.example.tlctvscreenshot}
ACTIVITY=${ACTIVITY:-.MainActivity}

mkdir -p "$(dirname "$OUT")"
adb devices -l
adb install -r "$APK"
adb shell am start -n "$PACKAGE/$ACTIVITY"
sleep 5
adb exec-out screencap -p > "$OUT"
echo "$OUT"
