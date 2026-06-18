#!/usr/bin/env bash
set -euo pipefail

ADB=${ADB:-adb}
SERIAL=${1:-}
NAME=${2:-phone-capture-$(date +%Y%m%d-%H%M%S).png}
mkdir -p captures

if [[ -z "$SERIAL" ]]; then
  echo "Usage: $0 <adb-serial> [filename.png]" >&2
  $ADB devices -l >&2
  exit 2
fi

$ADB -s "$SERIAL" exec-out screencap -p > "captures/$NAME"
echo "captures/$NAME"
