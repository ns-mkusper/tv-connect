#!/usr/bin/env bash
set -euo pipefail

THINKPAD_HOST=${THINKPAD_HOST:-poland-thinkpad}
SERIAL=${1:-}
NAME=${2:-phone-capture-$(date +%Y%m%d-%H%M%S).png}
mkdir -p captures

if [[ -z "$SERIAL" ]]; then
  echo "Usage: THINKPAD_HOST=<host> $0 <adb-serial> [filename.png]" >&2
  ssh "$THINKPAD_HOST" adb devices -l >&2
  exit 2
fi

ssh "$THINKPAD_HOST" adb -s "$SERIAL" exec-out screencap -p > "captures/$NAME"
echo "captures/$NAME"
