package com.intruderalerts.client;

import net.fabricmc.api.ClientModInitializer;

public class IntruderAlertsClient implements ClientModInitializer {
    private static boolean enabled = true;

    @Override
    public void onInitializeClient() {
        TrustManager trustManager = new TrustManager();
        SettingsManager settingsManager = new SettingsManager();
        AlertManager alertManager = new AlertManager(settingsManager);
        ZoneManager zoneManager = new ZoneManager();
        HistoryManager historyManager = new HistoryManager();
        IgnoreManager ignoreManager = new IgnoreManager();
        PlayerTracker playerTracker = new PlayerTracker(trustManager, alertManager, zoneManager, historyManager, ignoreManager);
        playerTracker.register();
        CommandRegistrar.register(trustManager, playerTracker, zoneManager, alertManager, settingsManager, historyManager, ignoreManager);
    }

    public static boolean isEnabled() {
        return enabled;
    }

    public static boolean toggleEnabled() {
        enabled = !enabled;
        return enabled;
    }
}
