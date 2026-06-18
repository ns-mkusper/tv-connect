# TCL TV Screenshot

Kotlin Android + Jetpack Compose app for standalone screenshot capture and remote-control commands for compatible TCL/TLC TVs.

The runtime app does **not** require the companion app, TV ADB, phone ADB, or a hardcoded TV IP address. It discovers TVs on the local network and captures directly over the decoded TCL TV protocols.

## App behavior

1. Tap **Discover TV**.
2. The app sends TCL UDP discovery packets on port `6537` (`0x1989`).
3. It verifies candidates with the TV TCP control port `6553` using a plain `159>>{phoneName}>>1>>{uuid}>>1` handshake.
4. If UDP discovery is missed or blocked, it falls back to a local `/24` TCP `6553` scan.
5. Tap **Capture screenshot**.
6. The app opens a persistent TCP `6553` connection, sends the prompt/heartbeat, sends two screenshot requests, and downloads the second returned HTTP URL.
7. Scroll to **Remote control** to send D-pad, navigation, volume, channel, power, and number-pad commands to the selected TV.

Screenshots are saved in the app-private directory under `files/TCast/Images` and shown in the UI.

## Protocol details used by the standalone app

### Discovery

- UDP discovery port: `6537` (`0x1989`).
- Phone announcement: `1:{millis}:{phoneName}:PHONE:1:{phoneName}:{phoneId}:0:0\0`.
- TV announcements use sender type `TV`; the validated TV sends version `14` (`0xe`) with additional data `{deviceName}:{functionCode}:0:{mac}:{p2pMac}:{activeMac}\0`.
- If the phone receives TV command `1`, it answers command `3` back to the source IP/UDP port.
- Every UDP candidate is verified with TCP `6553`; unverified candidates are still shown, but verified ones include the `159>>` handshake details.

### Screenshot transport

- TCP control port: `6553`.
- Packet framing: 4-byte big-endian length followed by payload.
- Initial inquiry is plain UTF-8: `159>>{phoneName}>>1>>{uuid}>>1`.
- If the TV reports algorithm type `1`, follow-up commands and responses are AES/CBC/PKCS5-padded.
- AES key: `tnscreentnscreen`.
- AES IV hex: `1234567890abcdef1234567890abcdef`.
- Command order:
  1. Plain `159>>{phoneName}>>1>>{uuid}>>1`
  2. AES/plain depending on algorithm: `160>>{phoneId}>>{phoneName}`
  3. `150>>`
  4. `225>>`
  5. Wait about 500 ms
  6. `225>>`
  7. Download the second `225>>0>>http://...` URL

The two `225>>` requests are intentional. On the validated TV, the first URL can point at a stale or not-yet-listening HTTP port. The second URL on the same socket is reliable.


### Remote-control transport

Remote buttons reuse the same TCP `6553` connection setup as screenshot capture:

1. Plain `159>>{phoneName}>>1>>{uuid}>>1` inquiry.
2. AES/plain depending on algorithm: `160>>{phoneId}>>{phoneName}`.
3. `150>>` heartbeat.
4. Remote key command: `149>>{keyCode}`.

Implemented key codes:

- D-pad: up `11`, down `12`, left `13`, right `14`, OK `15`.
- Navigation: back `16`, menu `18`, home `19`, power `20`.
- Audio/channel: volume up `21`, volume down `22`, mute `23`, channel up `27`, channel down `28`.
- Number pad: `0` through `9`.

## Build

```bash
./gradlew :app:assembleDebug
```

The debug APK will be written to:

```bash
app/build/outputs/apk/debug/app-debug.apk
```

## Install on a connected Android device

```bash
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

ADB is only used here as a developer install method. The installed app itself does not use ADB.

## Developer/research scripts

The scripts in `scripts/` and APK captures in `captures/analysis/` were used to reverse-engineer and validate the protocol. They are not required by the runtime app.

Useful research scripts include:

- `scripts/probe_tcl_6553_handshake.py`: command-line probe for the TCL `6553` screenshot transport.
- `scripts/probe_raw_tcl_screenshot.py`: command-line probe for the older raw `6554` fallback socket.
- `scripts/companion_app_probe.sh`: one-time package/APK inspection helper.
- `scripts/take_phone_screenshot.sh` and `scripts/take_tv_screenshot.sh`: ADB screenshot helpers used only during reverse engineering.

## Notes

The app can only capture TVs that expose the compatible TCL screenshot services on the local network. It cannot silently screenshot arbitrary apps or unrelated TVs without a compatible TV-side screenshot service.
