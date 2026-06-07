# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

IntruderAlerts is a client-side Fabric Minecraft mod (Java 25) that alerts players when untrusted players enter their vicinity. It targets Minecraft 26.1.2.

## Build Commands

```bash
./gradlew build          # Build the mod JAR (output: build/libs/intruderalerts-<version>+mc26.1.2.jar)
./gradlew clean build    # Clean rebuild from scratch
./gradlew runClient      # Launch Minecraft client with the mod loaded
./gradlew runServer      # Launch Minecraft server with the mod loaded
```

Gradle must run on JDK 25. If your default `java` is older, set `JAVA_HOME` to a JDK 25 install before invoking the wrapper, or add `org.gradle.java.home=<path>` to `~/.gradle/gradle.properties`.

No tests or linting are configured.

## Toolchain Requirements

- **Java 25** (Minecraft 26.1+ requirement).
- **Gradle 9.4.1** (configured via wrapper).
- **Fabric Loom 1.16-SNAPSHOT** (the unobfuscated-Minecraft plugin variant — note the `net.fabricmc.fabric-loom` plugin id).

Minecraft 26.1 was the first unobfuscated release: there is no `mappings` dependency, and the Fabric API uses Mojang official class names (`Minecraft`, `Level`, `Component`, `ChatFormatting`, `PlayerInfo`, `Identifier`, …) rather than Yarn names.

Local-machine setup, IDE tasks, and personal deploy steps live in `CLAUDE.local.md` (gitignored) when present.

## Architecture

**Source set layout (Fabric Loom `splitEnvironmentSourceSets`):**

- `src/main/` — Server/common side. `IntruderAlerts.java` is a stub `ModInitializer` (empty `onInitialize`).
- `src/client/` — All real logic lives here. `IntruderAlertsClient` is the `ClientModInitializer` entry point.

**Component wiring:** `IntruderAlertsClient.onInitializeClient()` creates all managers and passes them via constructor injection — no DI framework.

**Core data flow:**

1. `PlayerTracker` hooks `ClientTickEvents.END_CLIENT_TICK`, scanning every 20 ticks (1 second).
2. It checks if the player is inside a `ZoneManager` safe zone (suppresses alerts).
3. New untrusted players trigger `AlertManager.alert()` — chat message + `SystemToast` + Ender Dragon growl sound.

**Persistence:** `TrustManager`, `ZoneManager`, `HistoryManager`, `IgnoreManager`, and `SettingsManager` each read/write a JSON config file via Gson to the Fabric config directory (`intruderalerts.json`, `intruderalerts_zones.json`, `intruderalerts_history.json`, `intruderalerts_ignore.json`, `intruderalerts_settings.json`).

**In-game commands** (`CommandRegistrar`): `/intruder trust|untrust|list|toggle|settings sounds toggle|history [name|clear]|ignore add|ignore remove|ignore list|zone add|zone remove|zone list` — registered via Fabric Client Commands v2.
