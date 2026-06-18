#!/usr/bin/env python3
"""Probe TCL TCP 6553 command transport.

Decoded from com.tcl.tcastsdk:
- Packet framing is Java DataOutputStream.writeInt(length) + UTF-8 bytes.
- Initial inquiry command is unencrypted: 159>><phoneName>>1>><uuid>>1
- Normal post-handshake prompt command is encrypted when algorithm=1: 160>><phoneImei>><phoneName>
- Heartbeat is encrypted when algorithm=1: 150>>
- Screenshot command is encrypted when algorithm=1: 225>>
"""

from __future__ import annotations

import argparse
import socket
import struct
import subprocess
import sys
import time
import urllib.parse
import urllib.request
import uuid
from dataclasses import dataclass
from pathlib import Path

try:
    from Crypto.Cipher import AES
except ImportError:  # pragma: no cover - command-line environment fallback
    AES = None

DEFAULT_PORT = 6553
DEFAULT_TIMEOUT_SECONDS = 10.0
MAX_PACKET_BYTES = 1024 * 1024
AES_KEY = b"tnscreentnscreen"
AES_IV = bytes.fromhex("1234567890abcdef1234567890abcdef")


@dataclass
class Packet:
    text: str
    raw: bytes


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


def pkcs7_pad(data: bytes) -> bytes:
    pad_len = 16 - (len(data) % 16)
    return data + bytes([pad_len]) * pad_len


def pkcs7_unpad(data: bytes) -> bytes:
    if not data:
        raise ValueError("empty AES plaintext")
    pad_len = data[-1]
    if pad_len < 1 or pad_len > 16 or data[-pad_len:] != bytes([pad_len]) * pad_len:
        raise ValueError(f"invalid PKCS padding length {pad_len}")
    return data[:-pad_len]


def aes_encrypt(data: bytes) -> bytes:
    if AES is None:
        raise RuntimeError("pycryptodome is required for AES payloads")
    return AES.new(AES_KEY, AES.MODE_CBC, AES_IV).encrypt(pkcs7_pad(data))


def aes_decrypt(data: bytes) -> bytes:
    if AES is None:
        raise RuntimeError("pycryptodome is required for AES payloads")
    return pkcs7_unpad(AES.new(AES_KEY, AES.MODE_CBC, AES_IV).decrypt(data))


def send_text(sock: socket.socket, text: str, *, encrypted: bool = False) -> None:
    payload = text.encode("utf-8")
    if encrypted:
        payload = aes_encrypt(payload)
    sock.sendall(struct.pack(">I", len(payload)) + payload)


def read_packet(sock: socket.socket) -> Packet:
    raw_length = read_exact(sock, 4)
    (length,) = struct.unpack(">I", raw_length)
    if length < 0 or length >= MAX_PACKET_BYTES:
        raise ValueError(f"unexpected packet length {length}")
    raw = read_exact(sock, length)
    return Packet(text=raw.decode("utf-8", errors="replace"), raw=raw)


def decode_packet(packet: Packet, *, encrypted: bool) -> str:
    if encrypted:
        decrypted = aes_decrypt(packet.raw)
        text = decrypted.decode("utf-8", errors="replace")
        print(f"recv-encrypted[{len(packet.raw)}] decrypted[{len(decrypted)}]: {text}")
        return text
    print(f"recv[{len(packet.raw)}]: {packet.text}")
    return packet.text


def read_optional_packet(sock: socket.socket, *, encrypted: bool, label: str, timeout: float) -> str | None:
    previous_timeout = sock.gettimeout()
    sock.settimeout(timeout)
    try:
        packet = read_packet(sock)
    except socket.timeout:
        print(f"{label}: no response within {timeout}s")
        return None
    finally:
        sock.settimeout(previous_timeout)
    print(f"{label}:")
    return decode_packet(packet, encrypted=encrypted)


def probe_tcp_port(host: str, port: int, timeout: float) -> str:
    try:
        with socket.create_connection((host, port), timeout=timeout):
            return "open"
    except Exception as exc:  # noqa: BLE001 - diagnostic helper
        return f"closed/error: {exc}"


def manual_http_get(url: str, timeout: float) -> bytes:
    parsed = urllib.parse.urlparse(url)
    if parsed.scheme != "http" or not parsed.hostname:
        raise ValueError(f"manual HTTP probe only supports http URLs: {url}")
    port = parsed.port or 80
    path = urllib.parse.urlunparse(("", "", parsed.path or "/", parsed.params, parsed.query, ""))
    request = (
        f"GET {path} HTTP/1.1\r\n"
        f"Host: {parsed.hostname}:{port}\r\n"
        "User-Agent: TCLTvScreenshotProbe\r\n"
        "Connection: close\r\n"
        "\r\n"
    ).encode("ascii")
    with socket.create_connection((parsed.hostname, port), timeout=timeout) as http_sock:
        http_sock.settimeout(timeout)
        http_sock.sendall(request)
        chunks: list[bytes] = []
        while True:
            try:
                chunk = http_sock.recv(65536)
            except socket.timeout:
                break
            if not chunk:
                break
            chunks.append(chunk)
    return b"".join(chunks)


def adb_port_state(adb_serial: str, port: int) -> str:
    port_hex = f"{port:04X}"
    try:
        completed = subprocess.run(
            [
                "adb",
                "-s",
                adb_serial,
                "shell",
                f"cat /proc/net/tcp /proc/net/tcp6 2>/dev/null | grep -i ':{port_hex} ' || true",
            ],
            check=False,
            text=True,
            stdout=subprocess.PIPE,
            stderr=subprocess.STDOUT,
            timeout=2.0,
        )
    except Exception as exc:  # noqa: BLE001 - diagnostic helper
        return f"adb-error: {exc}"
    output = completed.stdout.strip()
    return output or "not-listed"


def download_with_retries(
    url: str,
    output: Path,
    *,
    timeout: float,
    retries: int,
    delay: float,
    manual: bool,
    adb_serial: str | None = None,
    before_attempt=None,
) -> bytes:
    parsed = urllib.parse.urlparse(url)
    host = parsed.hostname
    port = parsed.port or (80 if parsed.scheme == "http" else 443)
    last_exc: Exception | None = None
    for attempt in range(1, retries + 1):
        if before_attempt is not None:
            before_attempt(attempt)
        if adb_serial:
            print(f"download attempt {attempt}/{retries}: adb /proc port {port}: {adb_port_state(adb_serial, port)}")
        if host:
            port_state = probe_tcp_port(host, port, min(timeout, 1.0))
            print(f"download attempt {attempt}/{retries}: tcp {host}:{port} {port_state}")
        else:
            print(f"download attempt {attempt}/{retries}")
        try:
            if manual:
                response_bytes = manual_http_get(url, timeout)
                header, sep, body = response_bytes.partition(b"\r\n\r\n")
                first_line = header.splitlines()[0].decode("iso-8859-1", errors="replace") if header else "<no response>"
                print(f"manual-http status: {first_line}; total={len(response_bytes)} body={len(body) if sep else 0}")
                if not first_line.startswith("HTTP/1.") or " 200 " not in first_line:
                    raise RuntimeError(f"unexpected manual HTTP status {first_line!r}")
                data = body
            else:
                request = urllib.request.Request(url, headers={"User-Agent": "TCLTvScreenshotProbe"})
                with urllib.request.urlopen(request, timeout=timeout) as response:
                    data = response.read()
            output.parent.mkdir(parents=True, exist_ok=True)
            output.write_bytes(data)
            print(f"downloaded {len(data)} bytes to {output}; magic={data[:16].hex(' ')}")
            return data
        except Exception as exc:  # noqa: BLE001 - print every attempt for timing diagnostics
            last_exc = exc
            print(f"download attempt {attempt}/{retries} failed: {exc}")
            if attempt < retries:
                time.sleep(delay)
    raise RuntimeError(f"download failed after {retries} attempts: {last_exc}")


def main() -> int:
    parser = argparse.ArgumentParser(description="Check the TV screenshot command socket")
    parser.add_argument("tv_ip", help="TV IP address")
    parser.add_argument("--port", type=int, default=DEFAULT_PORT)
    parser.add_argument("--timeout", type=float, default=DEFAULT_TIMEOUT_SECONDS)
    parser.add_argument("--phone-name", default="CoderProbe", help="handshake phone name; real app uses Build.MODEL")
    parser.add_argument("--uuid", default=str(uuid.uuid4()), help="handshake UUID; real app uses Settings.Secure android_id")
    parser.add_argument("--phone-imei", help="phone id for 160 prompt; defaults to --uuid")
    parser.add_argument("--send-prompt", action="store_true", help="send post-handshake 160>><phoneImei>><phoneName> after the initial handshake")
    parser.add_argument("--read-after-prompt", action="store_true", help="try to read one optional response after 160 prompt")
    parser.add_argument("--send-heartbeat", action="store_true", help="send encrypted heartbeat 150>> after handshake/prompt")
    parser.add_argument("--read-after-heartbeat", action="store_true", help="try to read one optional response after 150 heartbeat")
    parser.add_argument("--optional-read-timeout", type=float, default=1.0, help="timeout for optional prompt/heartbeat response reads")
    parser.add_argument("--send-shot", action="store_true", help="send 225>> after handshake")
    parser.add_argument("--shot-count", type=int, default=1, help="number of sequential 225>> screenshot commands to send on the same 6553 socket")
    parser.add_argument("--shot-gap", type=float, default=0.25, help="seconds between sequential 225>> screenshot commands")
    parser.add_argument("--force-plain-shot", action="store_true", help="do not AES-encrypt post-handshake commands even if TV negotiates algorithm 1")
    parser.add_argument("--output", help="download screenshot URL to this path when response status is 0")
    parser.add_argument("--download-retries", type=int, default=10, help="URL download attempts before failing")
    parser.add_argument("--download-delay", type=float, default=0.25, help="seconds between URL download attempts")
    parser.add_argument("--manual-http", action="store_true", help="download URL with a minimal raw HTTP GET for diagnostics")
    parser.add_argument("--post-url-delay", type=float, default=0.0, help="seconds to keep 6553 socket open after receiving URL before HTTP")
    parser.add_argument("--heartbeat-during-download", action="store_true", help="send 150>> on the 6553 socket before each HTTP attempt")
    parser.add_argument("--adb-serial", help="ADB serial for TV; dumps /proc/net/tcp* entry for the returned HTTP port")
    args = parser.parse_args()

    inquiry = f"159>>{args.phone_name}>>1>>{args.uuid}>>1"
    print(f"connect {args.tv_ip}:{args.port}")
    with socket.create_connection((args.tv_ip, args.port), timeout=args.timeout) as sock:
        sock.settimeout(args.timeout)
        print(f"send: {inquiry}")
        send_text(sock, inquiry)
        first = read_packet(sock)
        print(f"recv[{len(first.raw)}]: {first.text}")
        fields = first.text.split(">>")
        algorithm_type = fields[6] if len(fields) > 6 else ""
        if len(fields) > 6:
            print(f"algorithm_type_field[6]: {algorithm_type!r}")
        if len(fields) > 10:
            print(f"function_code_field[10]: {fields[10]!r}")
        encrypted = algorithm_type == "1" and not args.force_plain_shot
        if args.send_prompt:
            phone_imei = args.phone_imei or args.uuid
            prompt = f"160>>{phone_imei}>>{args.phone_name}"
            print(f"send: {prompt} encrypted={encrypted}")
            send_text(sock, prompt, encrypted=encrypted)
            if args.read_after_prompt:
                read_optional_packet(
                    sock,
                    encrypted=encrypted,
                    label="prompt response",
                    timeout=max(0.0, args.optional_read_timeout),
                )
        if args.send_heartbeat:
            heartbeat = "150>>"
            print(f"send: {heartbeat} encrypted={encrypted}")
            send_text(sock, heartbeat, encrypted=encrypted)
            if args.read_after_heartbeat:
                read_optional_packet(
                    sock,
                    encrypted=encrypted,
                    label="heartbeat response",
                    timeout=max(0.0, args.optional_read_timeout),
                )
        if args.send_shot:
            last_url: str | None = None
            for shot_index in range(1, max(1, args.shot_count) + 1):
                if shot_index > 1 and args.shot_gap > 0:
                    print(f"shot gap {args.shot_gap}s")
                    time.sleep(args.shot_gap)
                shot = "225>>"
                print(f"send shot {shot_index}/{max(1, args.shot_count)}: {shot} encrypted={encrypted}")
                send_text(sock, shot, encrypted=encrypted)
                deadline = time.monotonic() + args.timeout
                shot_text: str | None = None
                while time.monotonic() < deadline:
                    shot_reply = read_packet(sock)
                    decoded_text = decode_packet(shot_reply, encrypted=encrypted)
                    if decoded_text.startswith("225>>"):
                        shot_text = decoded_text
                        break
                    print(f"ignore non-shot response while waiting for 225: {decoded_text}")
                if shot_text is None:
                    raise TimeoutError("timed out waiting for 225 screenshot response")
                shot_fields = shot_text.split(">>")
                if len(shot_fields) >= 3 and shot_fields[0] == "225" and shot_fields[1] == "0":
                    last_url = shot_fields[2]
                    print(f"screenshot_url[{shot_index}]: {last_url}")
                else:
                    raise RuntimeError(f"unexpected screenshot response: {shot_text}")
            if last_url is not None:
                url = last_url
                if args.post_url_delay > 0:
                    print(f"post-url delay {args.post_url_delay}s with 6553 socket still open")
                    time.sleep(args.post_url_delay)
                if args.output:
                    output = Path(args.output)

                    def before_attempt(attempt: int) -> None:
                        if args.heartbeat_during_download:
                            print(f"download keepalive before attempt {attempt}: send 150>> encrypted={encrypted}")
                            send_text(sock, "150>>", encrypted=encrypted)

                    download_with_retries(
                        url,
                        output,
                        timeout=args.timeout,
                        retries=max(1, args.download_retries),
                        delay=max(0.0, args.download_delay),
                        manual=args.manual_http,
                        adb_serial=args.adb_serial,
                        before_attempt=before_attempt,
                    )
        time.sleep(0.2)
    return 0


if __name__ == "__main__":
    try:
        raise SystemExit(main())
    except Exception as exc:  # noqa: BLE001
        print(f"probe failed: {exc}", file=sys.stderr)
        raise SystemExit(1)
