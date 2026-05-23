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
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

public class IgnoreManager {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Type STRING_LIST_TYPE = new TypeToken<List<String>>() {}.getType();
    private static final List<String> DEFAULT_NAMES = List.of("MineProbe");

    private final Path configPath;
    private final Set<String> ignoredNamesLower = new LinkedHashSet<>();
    private final Set<String> ignoredNamesDisplay = new LinkedHashSet<>();

    public IgnoreManager() {
        this.configPath = FabricLoader.getInstance().getConfigDir().resolve("intruderalerts_ignore.json");
        load();
    }

    public boolean isIgnoredName(String name) {
        return name != null && ignoredNamesLower.contains(name.toLowerCase(Locale.ROOT));
    }

    public boolean addName(String name) {
        if (name == null || name.isEmpty()) {
            return false;
        }
        if (!ignoredNamesLower.add(name.toLowerCase(Locale.ROOT))) {
            return false;
        }
        ignoredNamesDisplay.add(name);
        save();
        return true;
    }

    public boolean removeName(String name) {
        if (name == null) {
            return false;
        }
        String lower = name.toLowerCase(Locale.ROOT);
        if (!ignoredNamesLower.remove(lower)) {
            return false;
        }
        ignoredNamesDisplay.removeIf(n -> n.toLowerCase(Locale.ROOT).equals(lower));
        save();
        return true;
    }

    public List<String> getNames() {
        return Collections.unmodifiableList(new ArrayList<>(ignoredNamesDisplay));
    }

    private void load() {
        if (!Files.exists(configPath)) {
            for (String name : DEFAULT_NAMES) {
                ignoredNamesLower.add(name.toLowerCase(Locale.ROOT));
                ignoredNamesDisplay.add(name);
            }
            save();
            return;
        }
        try {
            String json = Files.readString(configPath);
            List<String> names = GSON.fromJson(json, STRING_LIST_TYPE);
            if (names != null) {
                for (String name : names) {
                    if (name == null || name.isEmpty()) continue;
                    if (ignoredNamesLower.add(name.toLowerCase(Locale.ROOT))) {
                        ignoredNamesDisplay.add(name);
                    }
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void save() {
        try {
            Files.createDirectories(configPath.getParent());
            Files.writeString(configPath, GSON.toJson(new ArrayList<>(ignoredNamesDisplay)));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
