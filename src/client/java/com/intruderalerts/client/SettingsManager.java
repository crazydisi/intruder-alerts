package com.intruderalerts.client;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public class SettingsManager {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    private final Path configPath;
    private boolean soundEnabled = true;

    public SettingsManager() {
        this.configPath = FabricLoader.getInstance().getConfigDir().resolve("intruderalerts_settings.json");
        load();
    }

    public boolean isSoundEnabled() {
        return soundEnabled;
    }

    public boolean toggleSound() {
        soundEnabled = !soundEnabled;
        save();
        return soundEnabled;
    }

    private void load() {
        if (!Files.exists(configPath)) {
            return;
        }
        try {
            String json = Files.readString(configPath);
            SettingsData data = GSON.fromJson(json, SettingsData.class);
            if (data != null) {
                soundEnabled = data.soundEnabled;
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void save() {
        SettingsData data = new SettingsData();
        data.soundEnabled = soundEnabled;
        try {
            Files.createDirectories(configPath.getParent());
            Files.writeString(configPath, GSON.toJson(data));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static class SettingsData {
        boolean soundEnabled = true;
    }
}
