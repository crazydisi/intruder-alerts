package com.intruderalerts.client;

import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.network.PlayerListEntry;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public class PlayerTracker {
    private static final int SCAN_INTERVAL = 20; // ticks (1 second)

    private final TrustManager trustManager;
    private final AlertManager alertManager;
    private final ZoneManager zoneManager;
    private final HistoryManager historyManager;
    private final IgnoreManager ignoreManager;
    private final Set<UUID> knownNearbyPlayers = new HashSet<>();
    private int tickCounter = 0;

    public PlayerTracker(TrustManager trustManager, AlertManager alertManager, ZoneManager zoneManager, HistoryManager historyManager, IgnoreManager ignoreManager) {
        this.trustManager = trustManager;
        this.alertManager = alertManager;
        this.zoneManager = zoneManager;
        this.historyManager = historyManager;
        this.ignoreManager = ignoreManager;
    }

    public void register() {
        ClientTickEvents.END_CLIENT_TICK.register(this::onTick);
    }

    public void clearTracking() {
        knownNearbyPlayers.clear();
        tickCounter = 0;
        historyManager.closeAllOpen(HistoryManager.EXIT_OUT_OF_RANGE);
    }

    private void onTick(MinecraftClient client) {
        if (!IntruderAlertsClient.isEnabled()) {
            return;
        }

        if (client.world == null || client.player == null) {
            if (!knownNearbyPlayers.isEmpty()) {
                historyManager.closeAllOpen(HistoryManager.EXIT_OUT_OF_RANGE);
            }
            knownNearbyPlayers.clear();
            return;
        }

        tickCounter++;
        if (tickCounter < SCAN_INTERVAL) {
            return;
        }
        tickCounter = 0;

        double playerX = client.player.getX();
        double playerY = client.player.getY();
        double playerZ = client.player.getZ();
        String dimension = client.world.getRegistryKey().getValue().toString();
        double radius = client.options.getViewDistance().getValue() * 16.0;

        if (zoneManager.isInAnyZone(playerX, playerY, playerZ, dimension, radius)) {
            if (!knownNearbyPlayers.isEmpty()) {
                historyManager.closeAllOpen(HistoryManager.EXIT_OUT_OF_RANGE);
            }
            knownNearbyPlayers.clear();
            return;
        }

        Set<UUID> serverPlayers = new HashSet<>();
        if (client.getNetworkHandler() != null) {
            for (PlayerListEntry entry : client.getNetworkHandler().getPlayerList()) {
                serverPlayers.add(ProfileUtil.getId(entry.getProfile()));
            }
        }

        Set<UUID> currentPlayers = new HashSet<>();

        for (AbstractClientPlayerEntity player : client.world.getPlayers()) {
            UUID uuid = player.getUuid();

            if (uuid.equals(client.player.getUuid())) {
                continue;
            }

            if (!serverPlayers.contains(uuid)) {
                continue;
            }

            if (trustManager.isTrusted(uuid)) {
                continue;
            }

            String name = player.getName().getString();

            if (ignoreManager.isIgnoredName(name)) {
                continue;
            }

            if (player.getY() < client.world.getBottomY()) {
                continue;
            }

            currentPlayers.add(uuid);

            if (!knownNearbyPlayers.contains(uuid)) {
                alertManager.alert(name);
            }

            historyManager.recordSeen(uuid, name, player.getX(), player.getY(), player.getZ(), dimension);
        }

        for (UUID uuid : knownNearbyPlayers) {
            if (!currentPlayers.contains(uuid)) {
                historyManager.recordExit(uuid, serverPlayers.contains(uuid));
            }
        }

        knownNearbyPlayers.clear();
        knownNearbyPlayers.addAll(currentPlayers);
    }
}
