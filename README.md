# IntruderAlerts

A client-side Fabric mod that notifies you whenever an untrusted player enters your render distance. Stay aware of who's approaching you on any multiplayer server — no server-side installation required.

## Features

- **Instant alerts** — Get notified through three channels at once: a bold chat message, a toast popup, and an Ender Dragon growl sound
- **Trust list** — Mark players as trusted so they never trigger alerts. Saved by UUID, so name changes won't break it
- **Safe zones** — Define locations where alerts are suppressed (e.g. spawn areas, trading hubs). Zones are dimension-aware and persist across sessions
- **Toggle on/off** — Quickly enable or disable the mod without restarting
- **Fully client-side** — Works on any server. No server-side mod or permissions needed
- **Persistent config** — Your trust list and zones are saved to JSON files and survive restarts

## Supported Versions

- Minecraft 1.21.4
- Minecraft 1.21.11

## Dependencies

- [Fabric Loader](https://fabricmc.net/) >= 0.18.4
- [Fabric API](https://modrinth.com/mod/fabric-api)

## Commands

All commands use the `/intruder` prefix:

| Command | Description |
|---|---|
| `/intruder trust <player>` | Add a player to your trust list (with tab-completion) |
| `/intruder untrust <player>` | Remove a player from your trust list |
| `/intruder list` | Show all trusted players |
| `/intruder toggle` | Enable or disable alerts |
| `/intruder settings sounds toggle` | Enable or disable alert sounds |
| `/intruder zone add <name>` | Create a safe zone at your current location |
| `/intruder zone remove <name>` | Delete a safe zone |
| `/intruder zone list` | Show all safe zones |

## How It Works

The mod scans nearby players once per second. When an untrusted player first appears within your render distance, you'll receive:

1. A **chat message**: `[IntruderAlerts] PlayerName entered your render distance!`
2. A **toast notification** in the top-right corner
3. An **Ender Dragon growl** sound effect

You'll only be alerted once per player — they won't trigger repeated alerts until they leave and re-enter your range. If you're inside a safe zone, all alerts are paused.

## Building

```bash
./gradlew build
```

The output JAR will be in `build/libs/`. To switch Minecraft versions, change `minecraft_version` in `gradle.properties` to `1.21.4` or `1.21.11`.

## License

MIT
