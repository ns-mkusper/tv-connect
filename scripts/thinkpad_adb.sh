#!/usr/bin/env bash
set -euo pipefail

# Runs adb on the remote ThinkPad that has the Ulefone plugged in.
# Set THINKPAD_HOST to the SSH alias or address from your home-lab repo/inventory.
THINKPAD_HOST=${THINKPAD_HOST:-poland-thinkpad}

if [[ $# -eq 0 ]]; then
  echo "Usage: THINKPAD_HOST=<host> $0 <adb args...>" >&2
  echo "Example: THINKPAD_HOST=10.0.0.42 $0 devices -l" >&2
  exit 2
fi

ssh "$THINKPAD_HOST" adb "$@"
