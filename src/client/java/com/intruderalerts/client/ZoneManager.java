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

public class ZoneManager {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Type ZONE_LIST_TYPE = new TypeToken<List<ZoneEntry>>() {}.getType();

    private final Path configPath;
    private final List<ZoneEntry> zones = new ArrayList<>();

    public ZoneManager() {
        this.configPath = FabricLoader.getInstance().getConfigDir().resolve("intruderalerts_zones.json");
        load();
    }

    public boolean addZone(String name, double x, double y, double z, String dimension) {
        for (ZoneEntry zone : zones) {
            if (zone.name.equalsIgnoreCase(name)) {
                return false;
            }
        }
        zones.add(new ZoneEntry(name, x, y, z, dimension));
        save();
        return true;
    }

    public boolean removeZone(String name) {
        boolean removed = zones.removeIf(zone -> zone.name.equalsIgnoreCase(name));
        if (removed) {
            save();
        }
        return removed;
    }

    public List<ZoneEntry> getZones() {
        return Collections.unmodifiableList(zones);
    }

    public Collection<String> getZoneNames() {
        List<String> names = new ArrayList<>();
        for (ZoneEntry zone : zones) {
            names.add(zone.name);
        }
        return Collections.unmodifiableList(names);
    }

    public boolean isInAnyZone(double playerX, double playerY, double playerZ, String dimension, double radiusBlocks) {
        for (ZoneEntry zone : zones) {
            if (!zone.dimension.equals(dimension)) {
                continue;
            }
            double dx = playerX - zone.x;
            double dy = playerY - zone.y;
            double dz = playerZ - zone.z;
            double distSq = dx * dx + dy * dy + dz * dz;
            if (distSq <= radiusBlocks * radiusBlocks) {
                return true;
            }
        }
        return false;
    }

    private void load() {
        if (!Files.exists(configPath)) {
            return;
        }
        try {
            String json = Files.readString(configPath);
            List<ZoneEntry> entries = GSON.fromJson(json, ZONE_LIST_TYPE);
            if (entries != null) {
                zones.addAll(entries);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void save() {
        try {
            Files.createDirectories(configPath.getParent());
            Files.writeString(configPath, GSON.toJson(zones));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static class ZoneEntry {
        public String name;
        public double x;
        public double y;
        public double z;
        public String dimension;

        ZoneEntry(String name, double x, double y, double z, String dimension) {
            this.name = name;
            this.x = x;
            this.y = y;
            this.z = z;
            this.dimension = dimension;
        }
    }
}
