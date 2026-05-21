package com.intruderalerts.client;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.lang.reflect.Type;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public class HistoryManager {
    public static final String EXIT_OUT_OF_RANGE = "OUT_OF_RANGE";
    public static final String EXIT_LOGGED_OUT = "LOGGED_OUT";

    private static final int MAX_ENTRIES = 500;
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Type ENTRY_LIST_TYPE = new TypeToken<List<HistoryEntry>>() {}.getType();

    private final Path configPath;
    private final List<HistoryEntry> entries = new ArrayList<>();
    private final Map<UUID, HistoryEntry> openEncounters = new HashMap<>();

    public HistoryManager() {
        this.configPath = FabricLoader.getInstance().getConfigDir().resolve("intruderalerts_history.json");
        load();
    }

    public void recordSeen(UUID uuid, String name, double x, double y, double z, String dimension) {
        long now = System.currentTimeMillis();
        HistoryEntry open = openEncounters.get(uuid);
        if (open == null) {
            HistoryEntry entry = new HistoryEntry();
            entry.uuid = uuid.toString();
            entry.name = name;
            entry.enteredAt = now;
            entry.enterX = x;
            entry.enterY = y;
            entry.enterZ = z;
            entry.enterDimension = dimension;
            entry.lastSeenAt = now;
            entry.lastX = x;
            entry.lastY = y;
            entry.lastZ = z;
            entry.lastDimension = dimension;
            entries.add(entry);
            openEncounters.put(uuid, entry);
            trim();
            save();
        } else {
            open.name = name;
            open.lastSeenAt = now;
            open.lastX = x;
            open.lastY = y;
            open.lastZ = z;
            open.lastDimension = dimension;
        }
    }

    public void recordExit(UUID uuid, boolean stillInTabList) {
        HistoryEntry open = openEncounters.remove(uuid);
        if (open == null) {
            return;
        }
        open.exitedAt = System.currentTimeMillis();
        open.exitReason = stillInTabList ? EXIT_OUT_OF_RANGE : EXIT_LOGGED_OUT;
        save();
    }

    public void closeAllOpen(String reason) {
        if (openEncounters.isEmpty()) {
            return;
        }
        long now = System.currentTimeMillis();
        for (HistoryEntry entry : openEncounters.values()) {
            entry.exitedAt = now;
            entry.exitReason = reason;
        }
        openEncounters.clear();
        save();
    }

    public List<HistoryEntry> getEntries() {
        return Collections.unmodifiableList(entries);
    }

    public Set<String> getKnownNames() {
        Set<String> names = new LinkedHashSet<>();
        for (HistoryEntry entry : entries) {
            if (entry.name != null) {
                names.add(entry.name);
            }
        }
        return Collections.unmodifiableSet(names);
    }

    public void clear() {
        entries.clear();
        openEncounters.clear();
        save();
    }

    private void trim() {
        while (entries.size() > MAX_ENTRIES) {
            HistoryEntry removed = entries.remove(0);
            if (removed.exitedAt == null) {
                try {
                    openEncounters.remove(UUID.fromString(removed.uuid));
                } catch (IllegalArgumentException ignored) {
                }
            }
        }
    }

    private void load() {
        if (!Files.exists(configPath)) {
            return;
        }
        try {
            String json = Files.readString(configPath);
            List<HistoryEntry> loaded = GSON.fromJson(json, ENTRY_LIST_TYPE);
            if (loaded != null) {
                for (HistoryEntry entry : loaded) {
                    if (entry.exitedAt == null) {
                        entry.exitedAt = entry.lastSeenAt;
                        entry.exitReason = EXIT_OUT_OF_RANGE;
                    }
                    entries.add(entry);
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void save() {
        try {
            Files.createDirectories(configPath.getParent());
            Files.writeString(configPath, GSON.toJson(entries));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static class HistoryEntry {
        public String uuid;
        public String name;
        public long enteredAt;
        public double enterX;
        public double enterY;
        public double enterZ;
        public String enterDimension;
        public long lastSeenAt;
        public double lastX;
        public double lastY;
        public double lastZ;
        public String lastDimension;
        public Long exitedAt;
        public String exitReason;
    }
}
