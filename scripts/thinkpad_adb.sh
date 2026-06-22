#!/usr/bin/env bash
set -euo pipefail

# Run adb on the remote ThinkPad.
# Set THINKPAD_HOST to the SSH name for that machine.
THINKPAD_HOST=${THINKPAD_HOST:-poland-thinkpad}

if [[ $# -eq 0 ]]; then
  echo "Usage: THINKPAD_HOST=<host> $0 <adb args...>" >&2
  echo "Example: THINKPAD_HOST=10.0.0.42 $0 devices -l" >&2
  exit 2
fi

ssh "$THINKPAD_HOST" adb "$@"
