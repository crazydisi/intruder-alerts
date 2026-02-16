package com.intruderalerts.client;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.lang.reflect.Type;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

public class TrustManager {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Type ENTRY_LIST_TYPE = new TypeToken<List<TrustEntry>>() {}.getType();

    private final Path configPath;
    private final Map<UUID, String> trustedPlayers = new HashMap<>();

    public TrustManager() {
        this.configPath = FabricLoader.getInstance().getConfigDir().resolve("intruderalerts.json");
        load();
    }

    public boolean isTrusted(UUID uuid) {
        return trustedPlayers.containsKey(uuid);
    }

    public boolean trust(UUID uuid, String name) {
        if (trustedPlayers.containsKey(uuid)) {
            return false;
        }
        trustedPlayers.put(uuid, name);
        save();
        return true;
    }

    public boolean untrust(String name) {
        UUID toRemove = null;
        for (Map.Entry<UUID, String> entry : trustedPlayers.entrySet()) {
            if (entry.getValue().equalsIgnoreCase(name)) {
                toRemove = entry.getKey();
                break;
            }
        }
        if (toRemove == null) {
            return false;
        }
        trustedPlayers.remove(toRemove);
        save();
        return true;
    }

    public Collection<Map.Entry<UUID, String>> getTrustedEntries() {
        return Collections.unmodifiableCollection(trustedPlayers.entrySet());
    }

    public Collection<String> getTrustedNames() {
        return Collections.unmodifiableCollection(trustedPlayers.values());
    }

    private void load() {
        if (!Files.exists(configPath)) {
            return;
        }
        try {
            String json = Files.readString(configPath);
            List<TrustEntry> entries = GSON.fromJson(json, ENTRY_LIST_TYPE);
            if (entries != null) {
                for (TrustEntry entry : entries) {
                    trustedPlayers.put(UUID.fromString(entry.uuid), entry.name);
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void save() {
        List<TrustEntry> entries = new ArrayList<>();
        for (Map.Entry<UUID, String> entry : trustedPlayers.entrySet()) {
            entries.add(new TrustEntry(entry.getKey().toString(), entry.getValue()));
        }
        try {
            Files.createDirectories(configPath.getParent());
            Files.writeString(configPath, GSON.toJson(entries));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static class TrustEntry {
        String uuid;
        String name;

        TrustEntry(String uuid, String name) {
            this.uuid = uuid;
            this.name = name;
        }
    }
}
