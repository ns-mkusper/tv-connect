#!/usr/bin/env python3
"""Probe the raw TCL screenshot socket.

Protocol notes:
- Connect to TV TCP port 6554.
- Send Java DataOutputStream-style big-endian length + UTF-8 "JSReq".
- Read big-endian length + image bytes.
"""

from __future__ import annotations

import argparse
import os
import socket
import struct
import sys
import time
from pathlib import Path

COMMAND = b"JSReq"
DEFAULT_PORT = 6554
DEFAULT_TIMEOUT_SECONDS = 10.0
MAX_IMAGE_BYTES = 25 * 1024 * 1024


def read_exact(sock: socket.socket, size: int) -> bytes:
    chunks: list[bytes] = []
    remaining = size
    while remaining:
        chunk = sock.recv(remaining)
        if not chunk:
            raise EOFError(f"socket closed with {remaining} bytes left to read")
        chunks.append(chunk)
        remaining -= len(chunk)
    return b"".join(chunks)


def detect_extension(data: bytes) -> str:
    if data.startswith(b"\x89PNG\r\n\x1a\n"):
        return ".png"
    if data.startswith(b"\xff\xd8\xff"):
        return ".jpg"
    if data.startswith(b"RIFF") and data[8:12] == b"WEBP":
        return ".webp"
    return ".bin"


def probe(ip: str, port: int, timeout: float) -> bytes:
    with socket.create_connection((ip, port), timeout=timeout) as sock:
        sock.settimeout(timeout)
        sock.sendall(struct.pack(">I", len(COMMAND)) + COMMAND)
        raw_length = read_exact(sock, 4)
        (length,) = struct.unpack(">I", raw_length)
        if length < 1 or length > MAX_IMAGE_BYTES:
            raise ValueError(f"unexpected response length {length}")
        return read_exact(sock, length)


def main() -> int:
    parser = argparse.ArgumentParser(description="Check the raw TV screenshot socket")
    parser.add_argument("tv_ip", help="TV IP address")
    parser.add_argument("output", nargs="?", help="output image path; extension auto-added if omitted")
    parser.add_argument("--port", type=int, default=DEFAULT_PORT, help=f"TCP port (default: {DEFAULT_PORT})")
    parser.add_argument("--timeout", type=float, default=DEFAULT_TIMEOUT_SECONDS, help="socket timeout in seconds")
    args = parser.parse_args()

    started = time.time()
    data = probe(args.tv_ip, args.port, args.timeout)
    ext = detect_extension(data)

    if args.output:
        output = Path(args.output)
        if output.suffix == "":
            output = output.with_suffix(ext)
    else:
        output = Path("captures") / f"raw-tcl-{args.tv_ip.replace('.', '-')}-{int(time.time())}{ext}"

    output.parent.mkdir(parents=True, exist_ok=True)
    output.write_bytes(data)
    elapsed = time.time() - started

    print(f"saved {len(data)} bytes to {output} in {elapsed:.2f}s")
    print(f"magic: {data[:16].hex(' ')}")
    return 0


if __name__ == "__main__":
    try:
        raise SystemExit(main())
    except Exception as exc:  # noqa: BLE001 - command-line probe should print concise errors
        print(f"probe failed: {exc}", file=sys.stderr)
        raise SystemExit(1)
