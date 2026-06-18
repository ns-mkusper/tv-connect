#!/usr/bin/env bash
set -euo pipefail

THINKPAD_HOST=${THINKPAD_HOST:-poland-thinkpad}
SERIAL=${1:-}
SERIAL_ARGS=()
if [[ -n "$SERIAL" ]]; then
  SERIAL_ARGS=(-s "$SERIAL")
fi

mkdir -p captures/apks captures/probe

echo "== remote devices on $THINKPAD_HOST =="
ssh "$THINKPAD_HOST" adb devices -l

echo "== the companion app-like packages =="
packages=$(ssh "$THINKPAD_HOST" adb "${SERIAL_ARGS[@]}" shell pm list packages | tr -d '\r' | grep -Ei 'magi|connect|tcl|roku|cast|tv' || true)
printf '%s\n' "$packages" | tee captures/probe/packages.txt

while read -r package_line; do
  [[ -z "$package_line" ]] && continue
  pkg=${package_line#package:}
  echo "== $pkg =="
  ssh "$THINKPAD_HOST" adb "${SERIAL_ARGS[@]}" shell dumpsys package "$pkg" > "captures/probe/${pkg}.dumpsys.txt" || true
  ssh "$THINKPAD_HOST" adb "${SERIAL_ARGS[@]}" shell cmd package resolve-activity --brief "$pkg" > "captures/probe/${pkg}.launcher.txt" || true
  apk_path=$(ssh "$THINKPAD_HOST" adb "${SERIAL_ARGS[@]}" shell pm path "$pkg" | tr -d '\r' | head -n1 | sed 's/^package://')
  if [[ -n "$apk_path" ]]; then
    ssh "$THINKPAD_HOST" adb "${SERIAL_ARGS[@]}" exec-out cat "$apk_path" > "captures/apks/${pkg}.apk" || true
  fi
done <<< "$packages"

echo "Wrote local probe output to captures/probe and APK pulls to captures/apks."
