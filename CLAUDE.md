# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

IntruderAlerts is a client-side Fabric Minecraft mod (Java 21) that alerts players when untrusted players enter their vicinity. It supports Minecraft 1.21.4 and 1.21.11.

## Build Commands

```bash
./gradlew build          # Build the mod JAR (output: build/libs/)
./gradlew runClient      # Launch Minecraft client with the mod loaded
./gradlew runServer      # Launch Minecraft server with the mod loaded
```

No tests or linting are configured.

## Switching Minecraft Versions

Change `minecraft_version` in `gradle.properties` to `1.21.4` or `1.21.11`. The build automatically selects the matching Yarn mappings, Fabric API version, and version-specific source directory.

## Architecture

**Source set layout (Fabric Loom `splitEnvironmentSourceSets`):**
- `src/main/` — Server/common side. `IntruderAlerts.java` is a stub `ModInitializer` (empty `onInitialize`).
- `src/client/` — All real logic lives here. `IntruderAlertsClient` is the `ClientModInitializer` entry point.
- `src/version/{1.21.4,1.21.11}/` — Per-version compatibility shims (`ProfileUtil`, `SoundUtil`) added to the client source set at build time. These exist because Mojang changed `GameProfile` (getters → record accessors) and `PositionedSoundInstance` (`.master()` → `.ui()`) between versions.

**Component wiring:** `IntruderAlertsClient.onInitializeClient()` creates all managers and passes them via constructor injection — no DI framework.

**Core data flow:**
1. `PlayerTracker` hooks `ClientTickEvents.END_CLIENT_TICK`, scanning every 20 ticks (1 second).
2. It checks if the player is inside a `ZoneManager` safe zone (suppresses alerts).
3. New untrusted players trigger `AlertManager.alert()` — chat message + `SystemToast` + Ender Dragon growl sound.

**Persistence:** `TrustManager` and `ZoneManager` read/write JSON config files via Gson to the Fabric config directory (`intruderalerts.json` and `intruderalerts_zones.json`).

**In-game commands** (`CommandRegistrar`): `/intruder trust|untrust|list|toggle|zone add|zone remove|zone list` — registered via Fabric Client Commands v2.
