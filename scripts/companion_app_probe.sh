#!/usr/bin/env bash
set -euo pipefail

ADB=${ADB:-adb}
SERIAL_ARG=()
if [[ "${1:-}" != "" ]]; then
  SERIAL_ARG=(-s "$1")
fi

mkdir -p captures/apks captures/probe

echo "== devices =="
$ADB devices -l

echo "== the companion app-like packages =="
packages=$($ADB "${SERIAL_ARG[@]}" shell pm list packages | tr -d '\r' | grep -Ei 'magi|connect|tcl|roku|cast|tv' || true)
printf '%s\n' "$packages" | tee captures/probe/packages.txt

while read -r package_line; do
  [[ -z "$package_line" ]] && continue
  pkg=${package_line#package:}
  echo "== $pkg =="
  $ADB "${SERIAL_ARG[@]}" shell dumpsys package "$pkg" > "captures/probe/${pkg}.dumpsys.txt" || true
  $ADB "${SERIAL_ARG[@]}" shell cmd package resolve-activity --brief "$pkg" > "captures/probe/${pkg}.launcher.txt" || true
  apk_path=$($ADB "${SERIAL_ARG[@]}" shell pm path "$pkg" | tr -d '\r' | head -n1 | sed 's/^package://')
  if [[ -n "$apk_path" ]]; then
    $ADB "${SERIAL_ARG[@]}" pull "$apk_path" "captures/apks/${pkg}.apk" || true
  fi
done <<< "$packages"

echo "Wrote probe output to captures/probe and APK pulls to captures/apks."
