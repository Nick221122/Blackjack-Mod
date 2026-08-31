package com.blackjackcalculator;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.decoration.ItemFrameEntity;
import net.minecraft.registry.RegistryKey;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.HashMap;
import java.util.Map;

/** Persistent client-only state. Host and Viewer state are deliberately separate. */
public final class BlackjackConfig {
    public enum Role { HOST, VIEWER }
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final String FILE_NAME = "blackjackcalculator.json";

    private static int hostX = 8, hostY = 8, viewerX = -1, viewerY = 8;
    private static float hostScale = 1.0f, viewerScale = 1.0f;
    private static String hostName = "Host", viewerName = "Viewer";
    private static Role activeRole = Role.HOST;
    private static final Map<String, Role> FRAME_ROLES = new HashMap<>();
    private static final Map<String, Integer> HOST_SCANNED_ITEMS = new LinkedHashMap<>();
    private static final Map<String, Integer> VIEWER_SCANNED_ITEMS = new LinkedHashMap<>();
    private static final Map<String, Map<Integer, Integer>> HOST_CONTAINER_SCANS = new LinkedHashMap<>();
    private static final Map<String, Map<Integer, Integer>> VIEWER_CONTAINER_SCANS = new LinkedHashMap<>();
    private static String hostActiveContainer = "", viewerActiveContainer = "";
    private static boolean loaded;

    private BlackjackConfig() {}

    public static void load(MinecraftClient client) {
        if (loaded) return;
        loaded = true;
        Path path = client.runDirectory.toPath().resolve(FILE_NAME);
        if (!Files.exists(path)) return;
        try {
            JsonObject root = JsonParser.parseReader(Files.newBufferedReader(path)).getAsJsonObject();
            hostX = intValue(root, "hostX", hostX); hostY = intValue(root, "hostY", hostY);
            viewerX = intValue(root, "viewerX", viewerX); viewerY = intValue(root, "viewerY", viewerY);
            hostScale = floatValue(root, "hostScale", hostScale); viewerScale = floatValue(root, "viewerScale", viewerScale);
            hostName = stringValue(root, "hostName", hostName); viewerName = stringValue(root, "viewerName", viewerName);
            if (root.has("activeRole")) try { activeRole = Role.valueOf(root.get("activeRole").getAsString()); } catch (IllegalArgumentException ignored) {}
            hostActiveContainer = stringValue(root, "hostActiveContainer", ""); viewerActiveContainer = stringValue(root, "viewerActiveContainer", "");
            readStringIntMap(root, "hostScannedItems", HOST_SCANNED_ITEMS); readStringIntMap(root, "viewerScannedItems", VIEWER_SCANNED_ITEMS);
            readContainerMap(root, "hostContainerScans", HOST_CONTAINER_SCANS); readContainerMap(root, "viewerContainerScans", VIEWER_CONTAINER_SCANS);
            if (root.has("frameRoles") && root.get("frameRoles").isJsonObject()) {
                for (var entry : root.getAsJsonObject("frameRoles").entrySet()) {
                    try { FRAME_ROLES.put(entry.getKey(), Role.valueOf(entry.getValue().getAsString())); } catch (IllegalArgumentException ignored) {}
                }
            }
        } catch (Exception ignored) {}
    }

    private static int intValue(JsonObject root, String key, int fallback) { try { return root.has(key) ? root.get(key).getAsInt() : fallback; } catch (Exception e) { return fallback; } }
    private static float floatValue(JsonObject root, String key, float fallback) { try { return root.has(key) ? root.get(key).getAsFloat() : fallback; } catch (Exception e) { return fallback; } }
    private static String stringValue(JsonObject root, String key, String fallback) { try { return root.has(key) ? root.get(key).getAsString() : fallback; } catch (Exception e) { return fallback; } }

    private static void readStringIntMap(JsonObject root, String key, Map<String, Integer> target) {
        if (!root.has(key) || !root.get(key).isJsonObject()) return;
        for (var e : root.getAsJsonObject(key).entrySet()) try { target.put(e.getKey(), e.getValue().getAsInt()); } catch (Exception ignored) {}
    }
    private static void readContainerMap(JsonObject root, String key, Map<String, Map<Integer, Integer>> target) {
        if (!root.has(key) || !root.get(key).isJsonObject()) return;
        for (var ce : root.getAsJsonObject(key).entrySet()) {
            if (!ce.getValue().isJsonObject()) continue;
            Map<Integer, Integer> values = new LinkedHashMap<>();
            for (var e : ce.getValue().getAsJsonObject().entrySet()) try { values.put(Integer.parseInt(e.getKey()), e.getValue().getAsInt()); } catch (Exception ignored) {}
            target.put(ce.getKey(), values);
        }
    }

    public static void save(MinecraftClient client) {
        if (client == null) return;
        JsonObject root = new JsonObject();
        root.addProperty("hostX", hostX); root.addProperty("hostY", hostY); root.addProperty("viewerX", viewerX); root.addProperty("viewerY", viewerY);
        root.addProperty("hostScale", hostScale); root.addProperty("viewerScale", viewerScale);
        root.addProperty("hostName", hostName); root.addProperty("viewerName", viewerName); root.addProperty("activeRole", activeRole.name());
        root.addProperty("hostActiveContainer", hostActiveContainer); root.addProperty("viewerActiveContainer", viewerActiveContainer);
        JsonObject roles = new JsonObject(); FRAME_ROLES.forEach((k, v) -> roles.addProperty(k, v.name())); root.add("frameRoles", roles);
        writeStringIntMap(root, "hostScannedItems", HOST_SCANNED_ITEMS); writeStringIntMap(root, "viewerScannedItems", VIEWER_SCANNED_ITEMS);
        writeContainerMap(root, "hostContainerScans", HOST_CONTAINER_SCANS); writeContainerMap(root, "viewerContainerScans", VIEWER_CONTAINER_SCANS);
        try { Files.writeString(client.runDirectory.toPath().resolve(FILE_NAME), GSON.toJson(root)); } catch (IOException ignored) {}
    }

    private static void writeStringIntMap(JsonObject root, String key, Map<String, Integer> map) { JsonObject out = new JsonObject(); map.forEach(out::addProperty); root.add(key, out); }
    private static void writeContainerMap(JsonObject root, String key, Map<String, Map<Integer, Integer>> map) {
        JsonObject out = new JsonObject();
        map.forEach((container, values) -> { JsonObject slots = new JsonObject(); values.forEach((slot, value) -> slots.addProperty(Integer.toString(slot), value)); out.add(container, slots); });
        root.add(key, out);
    }

    public static Role getRole(ItemFrameEntity frame, RegistryKey<World> dimension) { return FRAME_ROLES.get(roleKey(frame, dimension)); }
    public static void setRole(ItemFrameEntity frame, RegistryKey<World> dimension, Role role) { FRAME_ROLES.put(roleKey(frame, dimension), role); }
    public static void clearRole(ItemFrameEntity frame, RegistryKey<World> dimension) { FRAME_ROLES.remove(roleKey(frame, dimension)); }
    private static String roleKey(ItemFrameEntity frame, RegistryKey<World> dimension) { return dimension.getValue() + ":" + frame.getUuid(); }

    public static void saveContainerScan(String containerKey, Role role, Map<Integer, Integer> values) {
        Map<Integer, Integer> copy = new LinkedHashMap<>(values);
        Map<String, Map<Integer, Integer>> target = role == Role.HOST ? HOST_CONTAINER_SCANS : VIEWER_CONTAINER_SCANS;
        target.put(containerKey, copy); setActiveContainer(role, containerKey);
        Map<String, Integer> simple = role == Role.HOST ? HOST_SCANNED_ITEMS : VIEWER_SCANNED_ITEMS;
        simple.clear(); copy.forEach((slot, value) -> simple.put(Integer.toString(slot), value));
    }
    public static Map<Integer, Integer> getActiveContainerScan(Role role) {
        String key = getActiveContainer(role); if (key.isEmpty()) return Map.of();
        Map<Integer, Integer> map = (role == Role.HOST ? HOST_CONTAINER_SCANS : VIEWER_CONTAINER_SCANS).get(key);
        return map == null ? Map.of() : Map.copyOf(map);
    }
    public static String getActiveContainer(Role role) { return role == Role.HOST ? hostActiveContainer : viewerActiveContainer; }
    private static void setActiveContainer(Role role, String key) { if (role == Role.HOST) hostActiveContainer = key; else viewerActiveContainer = key; }

    public static String getHostName() { return hostName; }
    public static String getViewerName() { return viewerName; }
    public static void setHostName(String value) { hostName = cleanName(value, "Host"); }
    public static void setViewerName(String value) { viewerName = cleanName(value, "Viewer"); }
    private static String cleanName(String value, String fallback) { String v = value == null ? "" : value.trim(); return v.isEmpty() ? fallback : v.substring(0, Math.min(24, v.length())); }

    public static String containerKey(RegistryKey<World> dimension, BlockPos pos) { return dimension.getValue() + ":" + pos.asLong(); }
    public static Role getActiveRole() { return activeRole; }
    public static void setActiveRole(Role role) { activeRole = role; }
    public static float getHostScale() { return hostScale; }
    public static float getViewerScale() { return viewerScale; }
    public static void setHostScale(float scale) { hostScale = Math.max(0.5f, Math.min(2.0f, scale)); }
    public static void setViewerScale(float scale) { viewerScale = Math.max(0.5f, Math.min(2.0f, scale)); }
    public static int getHostX() { return hostX; }
    public static int getHostY() { return hostY; }
    public static int getViewerX() { return viewerX; }
    public static int getViewerY() { return viewerY; }
    public static void setHostPosition(int x, int y) { hostX = x; hostY = y; }
    public static void setViewerPosition(int x, int y) { viewerX = x; viewerY = y; }
    public static void resetPositions() { hostX = 8; hostY = 8; viewerX = -1; viewerY = 8; hostScale = 1.0f; viewerScale = 1.0f; }
}
