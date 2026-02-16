package com.intruderalerts.client;

import net.fabricmc.api.ClientModInitializer;

public class IntruderAlertsClient implements ClientModInitializer {
    private static boolean enabled = true;

    @Override
    public void onInitializeClient() {
        TrustManager trustManager = new TrustManager();
        AlertManager alertManager = new AlertManager();
        ZoneManager zoneManager = new ZoneManager();
        PlayerTracker playerTracker = new PlayerTracker(trustManager, alertManager, zoneManager);
        playerTracker.register();
        CommandRegistrar.register(trustManager, playerTracker, zoneManager);
    }

    public static boolean isEnabled() {
        return enabled;
    }

    public static boolean toggleEnabled() {
        enabled = !enabled;
        return enabled;
    }
}
