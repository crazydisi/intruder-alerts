package com.intruderalerts.client;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.PlayerListEntry;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static net.fabricmc.fabric.api.client.command.v2.ClientCommandManager.argument;
import static net.fabricmc.fabric.api.client.command.v2.ClientCommandManager.literal;

public class CommandRegistrar {

    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    public static void register(TrustManager trustManager, PlayerTracker playerTracker, ZoneManager zoneManager, AlertManager alertManager, SettingsManager settingsManager, HistoryManager historyManager) {
        ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) -> {
            dispatcher.register(literal("intruder")
                    .then(literal("demo")
                            .then(argument("name", StringArgumentType.word())
                                    .executes(ctx -> {
                                        alertManager.alert(StringArgumentType.getString(ctx, "name"));
                                        return 1;
                                    })
                            )
                    )
                    .then(literal("trust")
                            .then(argument("name", StringArgumentType.word())
                                    .suggests(suggestOnlinePlayers())
                                    .executes(ctx -> {
                                        String name = StringArgumentType.getString(ctx, "name");
                                        return executeTrust(ctx.getSource(), trustManager, name);
                                    })
                            )
                    )
                    .then(literal("untrust")
                            .then(argument("name", StringArgumentType.word())
                                    .suggests(suggestTrustedPlayers(trustManager))
                                    .executes(ctx -> {
                                        String name = StringArgumentType.getString(ctx, "name");
                                        return executeUntrust(ctx.getSource(), trustManager, name);
                                    })
                            )
                    )
                    .then(literal("list")
                            .executes(ctx -> executeList(ctx.getSource(), trustManager))
                    )
                    .then(literal("toggle")
                            .executes(ctx -> executeToggle(ctx.getSource(), playerTracker))
                    )
                    .then(literal("settings")
                            .then(literal("sounds")
                                    .then(literal("toggle")
                                            .executes(ctx -> executeSettingsSoundsToggle(ctx.getSource(), settingsManager))
                                    )
                            )
                    )
                    .then(literal("history")
                            .executes(ctx -> executeHistoryList(ctx.getSource(), historyManager, null))
                            .then(literal("clear")
                                    .executes(ctx -> executeHistoryClear(ctx.getSource(), historyManager))
                            )
                            .then(argument("name", StringArgumentType.word())
                                    .suggests(suggestHistoryNames(historyManager))
                                    .executes(ctx -> executeHistoryList(ctx.getSource(), historyManager, StringArgumentType.getString(ctx, "name")))
                            )
                    )
                    .then(literal("zone")
                            .then(literal("add")
                                    .then(argument("name", StringArgumentType.word())
                                            .executes(ctx -> {
                                                String name = StringArgumentType.getString(ctx, "name");
                                                return executeZoneAdd(ctx.getSource(), zoneManager, name);
                                            })
                                    )
                            )
                            .then(literal("remove")
                                    .then(argument("name", StringArgumentType.word())
                                            .suggests(suggestZoneNames(zoneManager))
                                            .executes(ctx -> {
                                                String name = StringArgumentType.getString(ctx, "name");
                                                return executeZoneRemove(ctx.getSource(), zoneManager, name);
                                            })
                                    )
                            )
                            .then(literal("list")
                                    .executes(ctx -> executeZoneList(ctx.getSource(), zoneManager))
                            )
                    )
            );
        });
    }

    private static int executeTrust(FabricClientCommandSource source, TrustManager trustManager, String name) {
        MinecraftClient client = source.getClient();
        if (client.getNetworkHandler() == null) {
            source.sendError(Text.translatable("intruderalerts.error.not_connected"));
            return 0;
        }

        PlayerListEntry target = null;
        for (PlayerListEntry entry : client.getNetworkHandler().getPlayerList()) {
            if (ProfileUtil.getName(entry.getProfile()).equalsIgnoreCase(name)) {
                target = entry;
                break;
            }
        }

        if (target == null) {
            source.sendError(Text.translatable("intruderalerts.error.player_not_in_tab", name));
            return 0;
        }

        UUID uuid = ProfileUtil.getId(target.getProfile());
        String actualName = ProfileUtil.getName(target.getProfile());

        if (trustManager.trust(uuid, actualName)) {
            source.sendFeedback(Text.empty()
                    .append(Text.translatable("intruderalerts.prefix").formatted(Formatting.GREEN))
                    .append(Text.translatable("intruderalerts.command.trust.success",
                            Text.literal(actualName).formatted(Formatting.YELLOW)).formatted(Formatting.WHITE))
                    .append(Text.literal(" (" + uuid + ")").formatted(Formatting.GRAY)));
        } else {
            source.sendFeedback(Text.empty()
                    .append(Text.translatable("intruderalerts.prefix").formatted(Formatting.YELLOW))
                    .append(Text.translatable("intruderalerts.command.trust.already",
                            Text.literal(actualName).formatted(Formatting.YELLOW)).formatted(Formatting.WHITE)));
        }

        return 1;
    }

    private static int executeUntrust(FabricClientCommandSource source, TrustManager trustManager, String name) {
        if (trustManager.untrust(name)) {
            source.sendFeedback(Text.empty()
                    .append(Text.translatable("intruderalerts.prefix").formatted(Formatting.GREEN))
                    .append(Text.translatable("intruderalerts.command.untrust.success",
                            Text.literal(name).formatted(Formatting.YELLOW)).formatted(Formatting.WHITE)));
        } else {
            source.sendError(Text.translatable("intruderalerts.error.player_not_trusted", name));
        }

        return 1;
    }

    private static int executeList(FabricClientCommandSource source, TrustManager trustManager) {
        var entries = trustManager.getTrustedEntries();
        if (entries.isEmpty()) {
            source.sendFeedback(Text.empty()
                    .append(Text.translatable("intruderalerts.prefix").formatted(Formatting.GREEN))
                    .append(Text.translatable("intruderalerts.command.list.empty").formatted(Formatting.WHITE)));
            return 1;
        }

        source.sendFeedback(Text.empty()
                .append(Text.translatable("intruderalerts.prefix").formatted(Formatting.GREEN))
                .append(Text.translatable("intruderalerts.command.list.header").formatted(Formatting.WHITE)));

        for (Map.Entry<UUID, String> entry : entries) {
            source.sendFeedback(Text.empty()
                    .append(Text.literal("  - ").formatted(Formatting.GRAY))
                    .append(Text.literal(entry.getValue()).formatted(Formatting.YELLOW))
                    .append(Text.literal(" (" + entry.getKey() + ")").formatted(Formatting.GRAY)));
        }

        return 1;
    }

    private static int executeToggle(FabricClientCommandSource source, PlayerTracker playerTracker) {
        boolean enabled = IntruderAlertsClient.toggleEnabled();

        if (!enabled) {
            playerTracker.clearTracking();
        }

        source.sendFeedback(Text.empty()
                .append(Text.translatable("intruderalerts.prefix").formatted(Formatting.GREEN))
                .append(enabled
                        ? Text.translatable("intruderalerts.command.toggle.enabled").formatted(Formatting.GREEN)
                        : Text.translatable("intruderalerts.command.toggle.disabled").formatted(Formatting.RED)));

        return 1;
    }

    private static SuggestionProvider<FabricClientCommandSource> suggestOnlinePlayers() {
        return (ctx, builder) -> {
            MinecraftClient client = ctx.getSource().getClient();
            if (client.getNetworkHandler() != null) {
                for (PlayerListEntry entry : client.getNetworkHandler().getPlayerList()) {
                    String name = ProfileUtil.getName(entry.getProfile());
                    if (name.toLowerCase().startsWith(builder.getRemainingLowerCase())) {
                        builder.suggest(name);
                    }
                }
            }
            return builder.buildFuture();
        };
    }

    private static SuggestionProvider<FabricClientCommandSource> suggestTrustedPlayers(TrustManager trustManager) {
        return (ctx, builder) -> {
            for (String name : trustManager.getTrustedNames()) {
                if (name.toLowerCase().startsWith(builder.getRemainingLowerCase())) {
                    builder.suggest(name);
                }
            }
            return builder.buildFuture();
        };
    }

    private static SuggestionProvider<FabricClientCommandSource> suggestZoneNames(ZoneManager zoneManager) {
        return (ctx, builder) -> {
            for (String name : zoneManager.getZoneNames()) {
                if (name.toLowerCase().startsWith(builder.getRemainingLowerCase())) {
                    builder.suggest(name);
                }
            }
            return builder.buildFuture();
        };
    }

    private static int executeSettingsSoundsToggle(FabricClientCommandSource source, SettingsManager settingsManager) {
        boolean enabled = settingsManager.toggleSound();

        source.sendFeedback(Text.empty()
                .append(Text.translatable("intruderalerts.prefix").formatted(Formatting.GREEN))
                .append(enabled
                        ? Text.translatable("intruderalerts.command.settings.sounds.enabled").formatted(Formatting.GREEN)
                        : Text.translatable("intruderalerts.command.settings.sounds.disabled").formatted(Formatting.RED)));

        return 1;
    }

    private static int executeZoneAdd(FabricClientCommandSource source, ZoneManager zoneManager, String name) {
        MinecraftClient client = source.getClient();
        if (client.player == null) {
            source.sendError(Text.translatable("intruderalerts.error.not_in_world"));
            return 0;
        }

        double x = client.player.getX();
        double y = client.player.getY();
        double z = client.player.getZ();
        String dimension = client.world.getRegistryKey().getValue().toString();

        if (zoneManager.addZone(name, x, y, z, dimension)) {
            source.sendFeedback(Text.empty()
                    .append(Text.translatable("intruderalerts.prefix").formatted(Formatting.GREEN))
                    .append(Text.translatable("intruderalerts.command.zone.added",
                            Text.literal(name).formatted(Formatting.YELLOW)).formatted(Formatting.WHITE))
                    .append(Text.literal(String.format(" at %.0f, %.0f, %.0f in %s", x, y, z, dimension)).formatted(Formatting.GRAY)));
        } else {
            source.sendFeedback(Text.empty()
                    .append(Text.translatable("intruderalerts.prefix").formatted(Formatting.YELLOW))
                    .append(Text.translatable("intruderalerts.command.zone.exists",
                            Text.literal(name).formatted(Formatting.YELLOW)).formatted(Formatting.WHITE)));
        }

        return 1;
    }

    private static int executeZoneRemove(FabricClientCommandSource source, ZoneManager zoneManager, String name) {
        if (zoneManager.removeZone(name)) {
            source.sendFeedback(Text.empty()
                    .append(Text.translatable("intruderalerts.prefix").formatted(Formatting.GREEN))
                    .append(Text.translatable("intruderalerts.command.zone.removed",
                            Text.literal(name).formatted(Formatting.YELLOW)).formatted(Formatting.WHITE)));
        } else {
            source.sendError(Text.translatable("intruderalerts.error.zone_not_found", name));
        }

        return 1;
    }

    private static int executeZoneList(FabricClientCommandSource source, ZoneManager zoneManager) {
        var zones = zoneManager.getZones();
        if (zones.isEmpty()) {
            source.sendFeedback(Text.empty()
                    .append(Text.translatable("intruderalerts.prefix").formatted(Formatting.GREEN))
                    .append(Text.translatable("intruderalerts.command.zone.list_empty").formatted(Formatting.WHITE)));
            return 1;
        }

        source.sendFeedback(Text.empty()
                .append(Text.translatable("intruderalerts.prefix").formatted(Formatting.GREEN))
                .append(Text.translatable("intruderalerts.command.zone.list_header").formatted(Formatting.WHITE)));

        for (ZoneManager.ZoneEntry zone : zones) {
            source.sendFeedback(Text.empty()
                    .append(Text.literal("  - ").formatted(Formatting.GRAY))
                    .append(Text.literal(zone.name).formatted(Formatting.YELLOW))
                    .append(Text.literal(String.format(" (%.0f, %.0f, %.0f in %s)", zone.x, zone.y, zone.z, zone.dimension)).formatted(Formatting.GRAY)));
        }

        return 1;
    }

    private static int executeHistoryList(FabricClientCommandSource source, HistoryManager historyManager, String filterName) {
        List<HistoryManager.HistoryEntry> all = historyManager.getEntries();
        List<HistoryManager.HistoryEntry> filtered = new ArrayList<>();
        for (HistoryManager.HistoryEntry entry : all) {
            if (filterName == null || (entry.name != null && entry.name.equalsIgnoreCase(filterName))) {
                filtered.add(entry);
            }
        }

        if (filtered.isEmpty()) {
            Text body = filterName == null
                    ? Text.translatable("intruderalerts.command.history.empty")
                    : Text.translatable("intruderalerts.command.history.not_found", Text.literal(filterName).formatted(Formatting.YELLOW));
            source.sendFeedback(Text.empty()
                    .append(Text.translatable("intruderalerts.prefix").formatted(Formatting.GREEN))
                    .append(body.copy().formatted(Formatting.WHITE)));
            return 1;
        }

        source.sendFeedback(Text.empty()
                .append(Text.translatable("intruderalerts.prefix").formatted(Formatting.GREEN))
                .append(Text.translatable("intruderalerts.command.history.header").formatted(Formatting.WHITE)));

        int start = filterName == null ? Math.max(0, filtered.size() - 20) : 0;
        for (int i = filtered.size() - 1; i >= start; i--) {
            HistoryManager.HistoryEntry entry = filtered.get(i);
            source.sendFeedback(formatHistoryRow(entry));
        }

        return 1;
    }

    private static Text formatHistoryRow(HistoryManager.HistoryEntry entry) {
        String enteredAt = formatTime(entry.enteredAt);
        String enterCoords = String.format("%.0f, %.0f, %.0f in %s",
                entry.enterX, entry.enterY, entry.enterZ, entry.enterDimension);
        String lastCoords = String.format("%.0f, %.0f, %.0f in %s",
                entry.lastX, entry.lastY, entry.lastZ, entry.lastDimension);

        Text base = Text.empty()
                .append(Text.literal("  - ").formatted(Formatting.GRAY))
                .append(Text.literal(entry.name == null ? entry.uuid : entry.name).formatted(Formatting.YELLOW))
                .append(Text.literal(" entered ").formatted(Formatting.GRAY))
                .append(Text.literal(enteredAt).formatted(Formatting.WHITE))
                .append(Text.literal(" at ").formatted(Formatting.GRAY))
                .append(Text.literal(enterCoords).formatted(Formatting.WHITE));

        if (entry.exitedAt == null) {
            return base.copy()
                    .append(Text.literal(" · ").formatted(Formatting.GRAY))
                    .append(Text.translatable("intruderalerts.command.history.row_open").formatted(Formatting.GREEN));
        }

        String exitedAt = formatTime(entry.exitedAt);
        Formatting reasonColor = HistoryManager.EXIT_LOGGED_OUT.equals(entry.exitReason) ? Formatting.RED : Formatting.AQUA;
        String reasonKey = HistoryManager.EXIT_LOGGED_OUT.equals(entry.exitReason)
                ? "intruderalerts.command.history.exit_logout"
                : "intruderalerts.command.history.exit_range";

        return base.copy()
                .append(Text.literal(" · ").formatted(Formatting.GRAY))
                .append(Text.translatable(reasonKey).formatted(reasonColor))
                .append(Text.literal(" ").formatted(Formatting.GRAY))
                .append(Text.literal(exitedAt).formatted(Formatting.WHITE))
                .append(Text.literal(" at ").formatted(Formatting.GRAY))
                .append(Text.literal(lastCoords).formatted(Formatting.WHITE));
    }

    private static String formatTime(long epochMillis) {
        return TIME_FORMATTER.format(Instant.ofEpochMilli(epochMillis).atZone(ZoneId.systemDefault()));
    }

    private static int executeHistoryClear(FabricClientCommandSource source, HistoryManager historyManager) {
        historyManager.clear();
        source.sendFeedback(Text.empty()
                .append(Text.translatable("intruderalerts.prefix").formatted(Formatting.GREEN))
                .append(Text.translatable("intruderalerts.command.history.cleared").formatted(Formatting.WHITE)));
        return 1;
    }

    private static SuggestionProvider<FabricClientCommandSource> suggestHistoryNames(HistoryManager historyManager) {
        return (ctx, builder) -> {
            for (String name : historyManager.getKnownNames()) {
                if (name.toLowerCase().startsWith(builder.getRemainingLowerCase())) {
                    builder.suggest(name);
                }
            }
            return builder.buildFuture();
        };
    }
}
