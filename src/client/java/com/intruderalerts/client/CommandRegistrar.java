package com.intruderalerts.client;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.network.chat.Component;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Consumer;

import static net.fabricmc.fabric.api.client.command.v2.ClientCommands.argument;
import static net.fabricmc.fabric.api.client.command.v2.ClientCommands.literal;

public class CommandRegistrar {

    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    public static void register(TrustManager trustManager, PlayerTracker playerTracker, ZoneManager zoneManager, AlertManager alertManager, SettingsManager settingsManager, HistoryManager historyManager, IgnoreManager ignoreManager) {
        ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) -> {
            dispatcher.register(literal("intruder")
                    .executes(ctx -> {
                        Minecraft client = ctx.getSource().getClient();
                        client.execute(() -> client.setScreen(new IntruderScreen(
                                trustManager, zoneManager, historyManager, ignoreManager,
                                settingsManager, playerTracker, alertManager)));
                        return 1;
                    })
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
                    .then(literal("ignore")
                            .then(literal("add")
                                    .then(argument("name", StringArgumentType.word())
                                            .suggests(suggestOnlinePlayers())
                                            .executes(ctx -> executeIgnoreAdd(ctx.getSource(), ignoreManager, StringArgumentType.getString(ctx, "name")))
                                    )
                            )
                            .then(literal("remove")
                                    .then(argument("name", StringArgumentType.word())
                                            .suggests(suggestIgnoredNames(ignoreManager))
                                            .executes(ctx -> executeIgnoreRemove(ctx.getSource(), ignoreManager, StringArgumentType.getString(ctx, "name")))
                                    )
                            )
                            .then(literal("list")
                                    .executes(ctx -> executeIgnoreList(ctx.getSource(), ignoreManager))
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
        Minecraft client = source.getClient();
        if (client.getConnection() == null) {
            source.sendError(Component.translatable("intruderalerts.error.not_connected"));
            return 0;
        }

        PlayerInfo target = null;
        for (PlayerInfo entry : client.getConnection().getOnlinePlayers()) {
            if (entry.getProfile().name().equalsIgnoreCase(name)) {
                target = entry;
                break;
            }
        }

        if (target == null) {
            source.sendError(Component.translatable("intruderalerts.error.player_not_in_tab", name));
            return 0;
        }

        UUID uuid = target.getProfile().id();
        String actualName = target.getProfile().name();

        if (trustManager.trust(uuid, actualName)) {
            source.sendFeedback(Component.empty()
                    .append(Component.translatable("intruderalerts.prefix").withStyle(ChatFormatting.GREEN))
                    .append(Component.translatable("intruderalerts.command.trust.success",
                            Component.literal(actualName).withStyle(ChatFormatting.YELLOW)).withStyle(ChatFormatting.WHITE))
                    .append(Component.literal(" (" + uuid + ")").withStyle(ChatFormatting.GRAY)));
        } else {
            source.sendFeedback(Component.empty()
                    .append(Component.translatable("intruderalerts.prefix").withStyle(ChatFormatting.YELLOW))
                    .append(Component.translatable("intruderalerts.command.trust.already",
                            Component.literal(actualName).withStyle(ChatFormatting.YELLOW)).withStyle(ChatFormatting.WHITE)));
        }

        return 1;
    }

    private static int executeUntrust(FabricClientCommandSource source, TrustManager trustManager, String name) {
        if (trustManager.untrust(name)) {
            source.sendFeedback(Component.empty()
                    .append(Component.translatable("intruderalerts.prefix").withStyle(ChatFormatting.GREEN))
                    .append(Component.translatable("intruderalerts.command.untrust.success",
                            Component.literal(name).withStyle(ChatFormatting.YELLOW)).withStyle(ChatFormatting.WHITE)));
        } else {
            source.sendError(Component.translatable("intruderalerts.error.player_not_trusted", name));
        }

        return 1;
    }

    private static int executeList(FabricClientCommandSource source, TrustManager trustManager) {
        printTrustList(trustManager, source::sendFeedback);
        return 1;
    }

    static void printTrustList(TrustManager trustManager, Consumer<Component> feedback) {
        var entries = trustManager.getTrustedEntries();
        if (entries.isEmpty()) {
            feedback.accept(Component.empty()
                    .append(Component.translatable("intruderalerts.prefix").withStyle(ChatFormatting.GREEN))
                    .append(Component.translatable("intruderalerts.command.list.empty").withStyle(ChatFormatting.WHITE)));
            return;
        }

        feedback.accept(Component.empty()
                .append(Component.translatable("intruderalerts.prefix").withStyle(ChatFormatting.GREEN))
                .append(Component.translatable("intruderalerts.command.list.header").withStyle(ChatFormatting.WHITE)));

        for (Map.Entry<UUID, String> entry : entries) {
            feedback.accept(Component.empty()
                    .append(Component.literal("  - ").withStyle(ChatFormatting.GRAY))
                    .append(Component.literal(entry.getValue()).withStyle(ChatFormatting.YELLOW))
                    .append(Component.literal(" (" + entry.getKey() + ")").withStyle(ChatFormatting.GRAY)));
        }
    }

    private static int executeToggle(FabricClientCommandSource source, PlayerTracker playerTracker) {
        boolean enabled = IntruderAlertsClient.toggleEnabled();

        if (!enabled) {
            playerTracker.clearTracking();
        }

        source.sendFeedback(Component.empty()
                .append(Component.translatable("intruderalerts.prefix").withStyle(ChatFormatting.GREEN))
                .append(enabled
                        ? Component.translatable("intruderalerts.command.toggle.enabled").withStyle(ChatFormatting.GREEN)
                        : Component.translatable("intruderalerts.command.toggle.disabled").withStyle(ChatFormatting.RED)));

        return 1;
    }

    private static SuggestionProvider<FabricClientCommandSource> suggestOnlinePlayers() {
        return (ctx, builder) -> {
            Minecraft client = ctx.getSource().getClient();
            if (client.getConnection() != null) {
                for (PlayerInfo entry : client.getConnection().getOnlinePlayers()) {
                    String name = entry.getProfile().name();
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

        source.sendFeedback(Component.empty()
                .append(Component.translatable("intruderalerts.prefix").withStyle(ChatFormatting.GREEN))
                .append(enabled
                        ? Component.translatable("intruderalerts.command.settings.sounds.enabled").withStyle(ChatFormatting.GREEN)
                        : Component.translatable("intruderalerts.command.settings.sounds.disabled").withStyle(ChatFormatting.RED)));

        return 1;
    }

    private static int executeZoneAdd(FabricClientCommandSource source, ZoneManager zoneManager, String name) {
        Minecraft client = source.getClient();
        if (client.player == null) {
            source.sendError(Component.translatable("intruderalerts.error.not_in_world"));
            return 0;
        }

        double x = client.player.getX();
        double y = client.player.getY();
        double z = client.player.getZ();
        String dimension = client.level.dimension().identifier().toString();

        if (zoneManager.addZone(name, x, y, z, dimension)) {
            source.sendFeedback(Component.empty()
                    .append(Component.translatable("intruderalerts.prefix").withStyle(ChatFormatting.GREEN))
                    .append(Component.translatable("intruderalerts.command.zone.added",
                            Component.literal(name).withStyle(ChatFormatting.YELLOW)).withStyle(ChatFormatting.WHITE))
                    .append(Component.literal(String.format(" at %.0f, %.0f, %.0f in %s", x, y, z, dimension)).withStyle(ChatFormatting.GRAY)));
        } else {
            source.sendFeedback(Component.empty()
                    .append(Component.translatable("intruderalerts.prefix").withStyle(ChatFormatting.YELLOW))
                    .append(Component.translatable("intruderalerts.command.zone.exists",
                            Component.literal(name).withStyle(ChatFormatting.YELLOW)).withStyle(ChatFormatting.WHITE)));
        }

        return 1;
    }

    private static int executeZoneRemove(FabricClientCommandSource source, ZoneManager zoneManager, String name) {
        if (zoneManager.removeZone(name)) {
            source.sendFeedback(Component.empty()
                    .append(Component.translatable("intruderalerts.prefix").withStyle(ChatFormatting.GREEN))
                    .append(Component.translatable("intruderalerts.command.zone.removed",
                            Component.literal(name).withStyle(ChatFormatting.YELLOW)).withStyle(ChatFormatting.WHITE)));
        } else {
            source.sendError(Component.translatable("intruderalerts.error.zone_not_found", name));
        }

        return 1;
    }

    private static int executeZoneList(FabricClientCommandSource source, ZoneManager zoneManager) {
        printZoneList(zoneManager, source::sendFeedback);
        return 1;
    }

    static void printZoneList(ZoneManager zoneManager, Consumer<Component> feedback) {
        var zones = zoneManager.getZones();
        if (zones.isEmpty()) {
            feedback.accept(Component.empty()
                    .append(Component.translatable("intruderalerts.prefix").withStyle(ChatFormatting.GREEN))
                    .append(Component.translatable("intruderalerts.command.zone.list_empty").withStyle(ChatFormatting.WHITE)));
            return;
        }

        feedback.accept(Component.empty()
                .append(Component.translatable("intruderalerts.prefix").withStyle(ChatFormatting.GREEN))
                .append(Component.translatable("intruderalerts.command.zone.list_header").withStyle(ChatFormatting.WHITE)));

        for (ZoneManager.ZoneEntry zone : zones) {
            feedback.accept(Component.empty()
                    .append(Component.literal("  - ").withStyle(ChatFormatting.GRAY))
                    .append(Component.literal(zone.name).withStyle(ChatFormatting.YELLOW))
                    .append(Component.literal(String.format(" (%.0f, %.0f, %.0f in %s)", zone.x, zone.y, zone.z, zone.dimension)).withStyle(ChatFormatting.GRAY)));
        }
    }

    private static int executeIgnoreAdd(FabricClientCommandSource source, IgnoreManager ignoreManager, String name) {
        if (ignoreManager.addName(name)) {
            source.sendFeedback(Component.empty()
                    .append(Component.translatable("intruderalerts.prefix").withStyle(ChatFormatting.GREEN))
                    .append(Component.translatable("intruderalerts.command.ignore.added",
                            Component.literal(name).withStyle(ChatFormatting.YELLOW)).withStyle(ChatFormatting.WHITE)));
        } else {
            source.sendFeedback(Component.empty()
                    .append(Component.translatable("intruderalerts.prefix").withStyle(ChatFormatting.YELLOW))
                    .append(Component.translatable("intruderalerts.command.ignore.exists",
                            Component.literal(name).withStyle(ChatFormatting.YELLOW)).withStyle(ChatFormatting.WHITE)));
        }
        return 1;
    }

    private static int executeIgnoreRemove(FabricClientCommandSource source, IgnoreManager ignoreManager, String name) {
        if (ignoreManager.removeName(name)) {
            source.sendFeedback(Component.empty()
                    .append(Component.translatable("intruderalerts.prefix").withStyle(ChatFormatting.GREEN))
                    .append(Component.translatable("intruderalerts.command.ignore.removed",
                            Component.literal(name).withStyle(ChatFormatting.YELLOW)).withStyle(ChatFormatting.WHITE)));
        } else {
            source.sendError(Component.translatable("intruderalerts.error.ignore_not_found", name));
        }
        return 1;
    }

    private static int executeIgnoreList(FabricClientCommandSource source, IgnoreManager ignoreManager) {
        printIgnoreList(ignoreManager, source::sendFeedback);
        return 1;
    }

    static void printIgnoreList(IgnoreManager ignoreManager, Consumer<Component> feedback) {
        List<String> names = ignoreManager.getNames();
        if (names.isEmpty()) {
            feedback.accept(Component.empty()
                    .append(Component.translatable("intruderalerts.prefix").withStyle(ChatFormatting.GREEN))
                    .append(Component.translatable("intruderalerts.command.ignore.list_empty").withStyle(ChatFormatting.WHITE)));
            return;
        }

        feedback.accept(Component.empty()
                .append(Component.translatable("intruderalerts.prefix").withStyle(ChatFormatting.GREEN))
                .append(Component.translatable("intruderalerts.command.ignore.list_header").withStyle(ChatFormatting.WHITE)));

        for (String name : names) {
            feedback.accept(Component.empty()
                    .append(Component.literal("  - ").withStyle(ChatFormatting.GRAY))
                    .append(Component.literal(name).withStyle(ChatFormatting.YELLOW)));
        }
    }

    private static SuggestionProvider<FabricClientCommandSource> suggestIgnoredNames(IgnoreManager ignoreManager) {
        return (ctx, builder) -> {
            for (String name : ignoreManager.getNames()) {
                if (name.toLowerCase().startsWith(builder.getRemainingLowerCase())) {
                    builder.suggest(name);
                }
            }
            return builder.buildFuture();
        };
    }

    private static int executeHistoryList(FabricClientCommandSource source, HistoryManager historyManager, String filterName) {
        printHistoryList(historyManager, filterName, source::sendFeedback);
        return 1;
    }

    static void printHistoryList(HistoryManager historyManager, String filterName, Consumer<Component> feedback) {
        List<HistoryManager.HistoryEntry> all = historyManager.getEntries();
        List<HistoryManager.HistoryEntry> filtered = new ArrayList<>();
        for (HistoryManager.HistoryEntry entry : all) {
            if (filterName == null || (entry.name != null && entry.name.equalsIgnoreCase(filterName))) {
                filtered.add(entry);
            }
        }

        if (filtered.isEmpty()) {
            Component body = filterName == null
                    ? Component.translatable("intruderalerts.command.history.empty")
                    : Component.translatable("intruderalerts.command.history.not_found", Component.literal(filterName).withStyle(ChatFormatting.YELLOW));
            feedback.accept(Component.empty()
                    .append(Component.translatable("intruderalerts.prefix").withStyle(ChatFormatting.GREEN))
                    .append(body.copy().withStyle(ChatFormatting.WHITE)));
            return;
        }

        feedback.accept(Component.empty()
                .append(Component.translatable("intruderalerts.prefix").withStyle(ChatFormatting.GREEN))
                .append(Component.translatable("intruderalerts.command.history.header").withStyle(ChatFormatting.WHITE)));

        int start = filterName == null ? Math.max(0, filtered.size() - 20) : 0;
        for (int i = filtered.size() - 1; i >= start; i--) {
            HistoryManager.HistoryEntry entry = filtered.get(i);
            feedback.accept(formatHistoryRow(entry));
        }
    }

    private static Component formatHistoryRow(HistoryManager.HistoryEntry entry) {
        String enteredAt = formatTime(entry.enteredAt);
        String enterCoords = String.format("%.0f, %.0f, %.0f in %s",
                entry.enterX, entry.enterY, entry.enterZ, entry.enterDimension);
        String lastCoords = String.format("%.0f, %.0f, %.0f in %s",
                entry.lastX, entry.lastY, entry.lastZ, entry.lastDimension);

        Component base = Component.empty()
                .append(Component.literal("  - ").withStyle(ChatFormatting.GRAY))
                .append(Component.literal(entry.name == null ? entry.uuid : entry.name).withStyle(ChatFormatting.YELLOW))
                .append(Component.literal(" entered ").withStyle(ChatFormatting.GRAY))
                .append(Component.literal(enteredAt).withStyle(ChatFormatting.WHITE))
                .append(Component.literal(" at ").withStyle(ChatFormatting.GRAY))
                .append(Component.literal(enterCoords).withStyle(ChatFormatting.WHITE));

        if (entry.exitedAt == null) {
            return base.copy()
                    .append(Component.literal(" · ").withStyle(ChatFormatting.GRAY))
                    .append(Component.translatable("intruderalerts.command.history.row_open").withStyle(ChatFormatting.GREEN));
        }

        String exitedAt = formatTime(entry.exitedAt);
        ChatFormatting reasonColor = HistoryManager.EXIT_LOGGED_OUT.equals(entry.exitReason) ? ChatFormatting.RED : ChatFormatting.AQUA;
        String reasonKey = HistoryManager.EXIT_LOGGED_OUT.equals(entry.exitReason)
                ? "intruderalerts.command.history.exit_logout"
                : "intruderalerts.command.history.exit_range";

        return base.copy()
                .append(Component.literal(" · ").withStyle(ChatFormatting.GRAY))
                .append(Component.translatable(reasonKey).withStyle(reasonColor))
                .append(Component.literal(" ").withStyle(ChatFormatting.GRAY))
                .append(Component.literal(exitedAt).withStyle(ChatFormatting.WHITE))
                .append(Component.literal(" at ").withStyle(ChatFormatting.GRAY))
                .append(Component.literal(lastCoords).withStyle(ChatFormatting.WHITE));
    }

    private static String formatTime(long epochMillis) {
        return TIME_FORMATTER.format(Instant.ofEpochMilli(epochMillis).atZone(ZoneId.systemDefault()));
    }

    private static int executeHistoryClear(FabricClientCommandSource source, HistoryManager historyManager) {
        historyManager.clear();
        source.sendFeedback(Component.empty()
                .append(Component.translatable("intruderalerts.prefix").withStyle(ChatFormatting.GREEN))
                .append(Component.translatable("intruderalerts.command.history.cleared").withStyle(ChatFormatting.WHITE)));
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
