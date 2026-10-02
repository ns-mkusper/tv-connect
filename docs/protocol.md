# TV protocol notes

Transport details decoded from a compatible TV's companion-app protocol and implemented by the app.

## Discovery

- UDP discovery port: `6537` (`0x1989`).
- Phone announcement: `1:{millis}:{phoneName}:PHONE:1:{phoneName}:{phoneId}:0:0\0`.
- TV announcements use sender type `TV`; the validated TV sends version `14` (`0xe`) with additional data `{deviceName}:{functionCode}:0:{mac}:{p2pMac}:{activeMac}\0`.
- If the phone receives TV command `1`, it answers command `3` back to the source IP/UDP port.
- Every UDP candidate is verified over TCP `6553`; unverified candidates are still listed.
- If UDP discovery is blocked, the app falls back to a local `/24` TCP `6553` scan.

## Screenshot transport

- TCP control port: `6553`.
- Framing: 4-byte big-endian length followed by payload.
- Initial inquiry is plain UTF-8: `159>>{phoneName}>>1>>{uuid}>>1`.
- If the TV reports algorithm type `1`, follow-up commands and responses are AES/CBC/PKCS5-padded.
- AES key: `tnscreentnscreen`.
- AES IV hex: `1234567890abcdef1234567890abcdef`.
- Command order:
  1. Plain `159>>{phoneName}>>1>>{uuid}>>1`
  2. `160>>{phoneId}>>{phoneName}` (AES or plain, per algorithm)
  3. `150>>`
  4. `225>>`
  5. Wait about 500 ms
  6. `225>>`
  7. Download the second `225>>0>>http://...` URL

The two `225>>` requests are intentional. The first URL can point at a stale or not-yet-listening HTTP port; the second URL on the same socket is reliable.

## Remote-control transport

Same session setup as capture:

1. Plain `159>>{phoneName}>>1>>{uuid}>>1` inquiry.
2. `160>>{phoneId}>>{phoneName}` (AES or plain, per algorithm).
3. `150>>` heartbeat.
4. Key command: `149>>{keyCode}`.

Key codes:

- D-pad: up `11`, down `12`, left `13`, right `14`, OK `15`.
- Navigation: back `16`, menu `18`, home `19`, power `20`.
- Audio/channel: volume up `21`, volume down `22`, mute `23`, channel up `27`, channel down `28`.
- Number pad: `0` through `9`.

## Storage

Captures are saved in app-private storage, listed after restart, and shared through a content URI rather than a file path. Exports land in `Pictures/TV Connect`.

## Limits

The app can only capture TVs that expose a compatible screenshot service on the local network.
