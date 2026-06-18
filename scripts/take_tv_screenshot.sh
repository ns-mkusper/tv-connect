#!/usr/bin/env bash
set -euo pipefail

ADB=${ADB:-adb}
TV_IP=${1:-}
NAME=${2:-tlc-tv-$(date +%Y%m%d-%H%M%S).png}
mkdir -p captures

if [[ -z "$TV_IP" ]]; then
  echo "Usage: $0 <tv-ip-address> [filename.png]" >&2
  exit 2
fi

serial="$TV_IP:5555"
$ADB connect "$serial" >/dev/null
$ADB -s "$serial" exec-out screencap -p > "captures/$NAME"
echo "captures/$NAME"
