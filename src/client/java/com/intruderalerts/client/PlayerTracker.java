package com.intruderalerts.client;

import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.AbstractClientPlayerEntity;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public class PlayerTracker {
    private static final int SCAN_INTERVAL = 20; // ticks (1 second)

    private final TrustManager trustManager;
    private final AlertManager alertManager;
    private final ZoneManager zoneManager;
    private final Set<UUID> knownNearbyPlayers = new HashSet<>();
    private int tickCounter = 0;

    public PlayerTracker(TrustManager trustManager, AlertManager alertManager, ZoneManager zoneManager) {
        this.trustManager = trustManager;
        this.alertManager = alertManager;
        this.zoneManager = zoneManager;
    }

    public void register() {
        ClientTickEvents.END_CLIENT_TICK.register(this::onTick);
    }

    public void clearTracking() {
        knownNearbyPlayers.clear();
        tickCounter = 0;
    }

    private void onTick(MinecraftClient client) {
        if (!IntruderAlertsClient.isEnabled()) {
            return;
        }

        if (client.world == null || client.player == null) {
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
            knownNearbyPlayers.clear();
            return;
        }

        Set<UUID> currentPlayers = new HashSet<>();

        for (AbstractClientPlayerEntity player : client.world.getPlayers()) {
            UUID uuid = player.getUuid();

            if (uuid.equals(client.player.getUuid())) {
                continue;
            }

            if (trustManager.isTrusted(uuid)) {
                continue;
            }

            currentPlayers.add(uuid);

            if (!knownNearbyPlayers.contains(uuid)) {
                alertManager.alert(player.getName().getString());
            }
        }

        knownNearbyPlayers.clear();
        knownNearbyPlayers.addAll(currentPlayers);
    }
}
