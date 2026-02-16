package com.intruderalerts.client;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.PlayerListEntry;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

import java.util.Map;
import java.util.UUID;

import static net.fabricmc.fabric.api.client.command.v2.ClientCommandManager.argument;
import static net.fabricmc.fabric.api.client.command.v2.ClientCommandManager.literal;

public class CommandRegistrar {

    public static void register(TrustManager trustManager, PlayerTracker playerTracker, ZoneManager zoneManager) {
        ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) -> {
            dispatcher.register(literal("intruder")
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
            source.sendError(Text.literal("Not connected to a server."));
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
            source.sendError(Text.literal("Player not found in tab list: " + name));
            return 0;
        }

        UUID uuid = ProfileUtil.getId(target.getProfile());
        String actualName = ProfileUtil.getName(target.getProfile());

        if (trustManager.trust(uuid, actualName)) {
            source.sendFeedback(Text.empty()
                    .append(Text.literal("[IntruderAlerts] ").formatted(Formatting.GREEN))
                    .append(Text.literal("Trusted ").formatted(Formatting.WHITE))
                    .append(Text.literal(actualName).formatted(Formatting.YELLOW))
                    .append(Text.literal(" (" + uuid + ")").formatted(Formatting.GRAY)));
        } else {
            source.sendFeedback(Text.empty()
                    .append(Text.literal("[IntruderAlerts] ").formatted(Formatting.YELLOW))
                    .append(Text.literal(actualName + " is already trusted.").formatted(Formatting.WHITE)));
        }

        return 1;
    }

    private static int executeUntrust(FabricClientCommandSource source, TrustManager trustManager, String name) {
        if (trustManager.untrust(name)) {
            source.sendFeedback(Text.empty()
                    .append(Text.literal("[IntruderAlerts] ").formatted(Formatting.GREEN))
                    .append(Text.literal("Removed ").formatted(Formatting.WHITE))
                    .append(Text.literal(name).formatted(Formatting.YELLOW))
                    .append(Text.literal(" from trust list.").formatted(Formatting.WHITE)));
        } else {
            source.sendError(Text.literal("Player not found in trust list: " + name));
        }

        return 1;
    }

    private static int executeList(FabricClientCommandSource source, TrustManager trustManager) {
        var entries = trustManager.getTrustedEntries();
        if (entries.isEmpty()) {
            source.sendFeedback(Text.empty()
                    .append(Text.literal("[IntruderAlerts] ").formatted(Formatting.GREEN))
                    .append(Text.literal("No trusted players.").formatted(Formatting.WHITE)));
            return 1;
        }

        source.sendFeedback(Text.empty()
                .append(Text.literal("[IntruderAlerts] ").formatted(Formatting.GREEN))
                .append(Text.literal("Trusted players:").formatted(Formatting.WHITE)));

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
                .append(Text.literal("[IntruderAlerts] ").formatted(Formatting.GREEN))
                .append(Text.literal(enabled ? "Enabled" : "Disabled")
                        .formatted(enabled ? Formatting.GREEN : Formatting.RED)));

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

    private static int executeZoneAdd(FabricClientCommandSource source, ZoneManager zoneManager, String name) {
        MinecraftClient client = source.getClient();
        if (client.player == null) {
            source.sendError(Text.literal("Not in a world."));
            return 0;
        }

        double x = client.player.getX();
        double y = client.player.getY();
        double z = client.player.getZ();
        String dimension = client.world.getRegistryKey().getValue().toString();

        if (zoneManager.addZone(name, x, y, z, dimension)) {
            source.sendFeedback(Text.empty()
                    .append(Text.literal("[IntruderAlerts] ").formatted(Formatting.GREEN))
                    .append(Text.literal("Added ignore zone ").formatted(Formatting.WHITE))
                    .append(Text.literal(name).formatted(Formatting.YELLOW))
                    .append(Text.literal(String.format(" at %.0f, %.0f, %.0f in %s", x, y, z, dimension)).formatted(Formatting.GRAY)));
        } else {
            source.sendFeedback(Text.empty()
                    .append(Text.literal("[IntruderAlerts] ").formatted(Formatting.YELLOW))
                    .append(Text.literal("A zone named ").formatted(Formatting.WHITE))
                    .append(Text.literal(name).formatted(Formatting.YELLOW))
                    .append(Text.literal(" already exists.").formatted(Formatting.WHITE)));
        }

        return 1;
    }

    private static int executeZoneRemove(FabricClientCommandSource source, ZoneManager zoneManager, String name) {
        if (zoneManager.removeZone(name)) {
            source.sendFeedback(Text.empty()
                    .append(Text.literal("[IntruderAlerts] ").formatted(Formatting.GREEN))
                    .append(Text.literal("Removed ignore zone ").formatted(Formatting.WHITE))
                    .append(Text.literal(name).formatted(Formatting.YELLOW)));
        } else {
            source.sendError(Text.literal("Zone not found: " + name));
        }

        return 1;
    }

    private static int executeZoneList(FabricClientCommandSource source, ZoneManager zoneManager) {
        var zones = zoneManager.getZones();
        if (zones.isEmpty()) {
            source.sendFeedback(Text.empty()
                    .append(Text.literal("[IntruderAlerts] ").formatted(Formatting.GREEN))
                    .append(Text.literal("No ignore zones.").formatted(Formatting.WHITE)));
            return 1;
        }

        source.sendFeedback(Text.empty()
                .append(Text.literal("[IntruderAlerts] ").formatted(Formatting.GREEN))
                .append(Text.literal("Ignore zones:").formatted(Formatting.WHITE)));

        for (ZoneManager.ZoneEntry zone : zones) {
            source.sendFeedback(Text.empty()
                    .append(Text.literal("  - ").formatted(Formatting.GRAY))
                    .append(Text.literal(zone.name).formatted(Formatting.YELLOW))
                    .append(Text.literal(String.format(" (%.0f, %.0f, %.0f in %s)", zone.x, zone.y, zone.z, zone.dimension)).formatted(Formatting.GRAY)));
        }

        return 1;
    }
}
