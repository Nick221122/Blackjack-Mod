package com.hostalwayswins;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.decoration.ItemFrameEntity;
import net.minecraft.util.math.BlockPos;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.LinkedHashSet;
import java.util.Set;

public final class BlackjackConfig {
    public enum Role { HOST, VIEWER }
    public record FrameSelection(String firstKey, String secondKey) {}
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final String FILE_NAME = "blackjackcalculator.json";
    private static int hostX = -1, hostY = 8, viewerX = 8, viewerY = 8;
    private static float hostScale = 1.0f, viewerScale = 1.0f;
    private static String hostName = "Host", viewerName = "Viewer";
    private static final Map<String, Integer> SHARED_SCANNED_MAPS = new LinkedHashMap<>();
    private static String hostFirstFrame, hostSecondFrame, viewerFirstFrame, viewerSecondFrame;
    private static final Map<Role, Set<String>> AUTO_SELECTED_FRAMES = new java.util.EnumMap<>(Role.class);
    private static boolean loaded;

    private BlackjackConfig() {}

    public static void load(MinecraftClient client) {
        if (loaded) return;
        loaded = true;
        Path path = client.runDirectory.toPath().resolve(FILE_NAME);
        if (!Files.exists(path)) return;
        try {
            JsonObject root = JsonParser.parseReader(Files.newBufferedReader(path)).getAsJsonObject();
            hostX = root.has("hostX") ? root.get("hostX").getAsInt() : hostX;
            hostY = root.has("hostY") ? root.get("hostY").getAsInt() : hostY;
            viewerX = root.has("viewerX") ? root.get("viewerX").getAsInt() : viewerX;
            viewerY = root.has("viewerY") ? root.get("viewerY").getAsInt() : viewerY;
            hostScale = root.has("hostScale") ? root.get("hostScale").getAsFloat() : hostScale;
            viewerScale = root.has("viewerScale") ? root.get("viewerScale").getAsFloat() : viewerScale;
            hostName = root.has("hostName") ? root.get("hostName").getAsString() : hostName;
            viewerName = root.has("viewerName") ? root.get("viewerName").getAsString() : viewerName;
            hostFirstFrame = getString(root, "hostFirstFrame");
            hostSecondFrame = getString(root, "hostSecondFrame");
            viewerFirstFrame = getString(root, "viewerFirstFrame");
            viewerSecondFrame = getString(root, "viewerSecondFrame");
            loadSelectedFrames(root, "hostSelectedFrames", Role.HOST);
            loadSelectedFrames(root, "viewerSelectedFrames", Role.VIEWER);
            if (root.has("sharedScannedMaps") && root.get("sharedScannedMaps").isJsonObject()) loadMap(root, "sharedScannedMaps", SHARED_SCANNED_MAPS);
        } catch (Exception ignored) {}
    }

    private static String getString(JsonObject root, String key) {
        return root.has(key) && !root.get(key).isJsonNull() ? root.get(key).getAsString() : null;
    }

    private static void loadSelectedFrames(JsonObject root, String key, Role role) {
        if (!root.has(key) || !root.get(key).isJsonArray()) return;
        Set<String> positions = new LinkedHashSet<>();
        root.getAsJsonArray(key).forEach(value -> { if (value.isJsonPrimitive()) positions.add(value.getAsString()); });
        AUTO_SELECTED_FRAMES.put(role, positions);
    }

    private static void loadMap(JsonObject root, String key, Map<String, Integer> target) {
        if (!root.has(key) || !root.get(key).isJsonObject()) return;
        root.getAsJsonObject(key).entrySet().forEach(e -> {
            try {
                String mapKey = e.getKey();
                // Older builds persisted MapIdComponent.toString() instead of the numeric ID.
                int marker = mapKey.lastIndexOf("id=");
                if (marker >= 0 && mapKey.endsWith("]")) {
                    String numericId = mapKey.substring(marker + 3, mapKey.length() - 1);
                    try {
                        Integer.parseInt(numericId);
                        mapKey = numericId;
                    } catch (NumberFormatException ignored) {
                        // Preserve unknown keys; they will be ignored by the winning strategy.
                    }
                }
                target.put(mapKey, e.getValue().getAsInt());
            } catch (Exception ignored) {}
        });
    }

    public static void save(MinecraftClient client) {
        if (client == null) return;
        JsonObject root = new JsonObject();
        root.addProperty("hostX", hostX); root.addProperty("hostY", hostY);
        root.addProperty("viewerX", viewerX); root.addProperty("viewerY", viewerY);
        root.addProperty("hostScale", hostScale); root.addProperty("viewerScale", viewerScale);
        root.addProperty("hostName", hostName); root.addProperty("viewerName", viewerName);
        if (hostFirstFrame != null) root.addProperty("hostFirstFrame", hostFirstFrame);
        if (hostSecondFrame != null) root.addProperty("hostSecondFrame", hostSecondFrame);
        if (viewerFirstFrame != null) root.addProperty("viewerFirstFrame", viewerFirstFrame);
        if (viewerSecondFrame != null) root.addProperty("viewerSecondFrame", viewerSecondFrame);
        JsonObject scanned = new JsonObject(); SHARED_SCANNED_MAPS.forEach(scanned::addProperty); root.add("sharedScannedMaps", scanned);
        addSelectedFrames(root, "hostSelectedFrames", Role.HOST);
        addSelectedFrames(root, "viewerSelectedFrames", Role.VIEWER);
        try { Files.writeString(client.runDirectory.toPath().resolve(FILE_NAME), GSON.toJson(root)); } catch (IOException ignored) {}
    }

    /** Stable selection key based on block position instead of an entity UUID. */
    public static String selectionKey(ItemFrameEntity frame) {
        BlockPos p = frame.getBlockPos();
        return p.getX() + "|" + p.getY() + "|" + p.getZ();
    }

    public static BlockPos parseSelectionPos(String key) {
        if (key == null) return null;
        String[] parts = key.split("\\|");
        if (parts.length != 3) return null;
        try { return new BlockPos(Integer.parseInt(parts[0]), Integer.parseInt(parts[1]), Integer.parseInt(parts[2])); }
        catch (NumberFormatException ignored) { return null; }
    }

    private static void addSelectedFrames(JsonObject root, String key, Role role) {
        Set<String> positions = AUTO_SELECTED_FRAMES.get(role);
        if (positions != null) { com.google.gson.JsonArray array = new com.google.gson.JsonArray(); positions.forEach(array::add); root.add(key, array); }
    }

    public static void setAutoSelectedFrames(Role role, Set<String> positions) { AUTO_SELECTED_FRAMES.put(role, new LinkedHashSet<>(positions)); }
    public static boolean isAutoSelectedFrame(Role role, BlockPos pos) {
        Set<String> positions = AUTO_SELECTED_FRAMES.get(role);
        return positions == null || positions.contains(pos.getX() + "|" + pos.getY() + "|" + pos.getZ());
    }

    public static void setFirstSelection(Role role, String key) { if (role == Role.HOST) hostFirstFrame = key; else viewerFirstFrame = key; }
    public static void setSecondSelection(Role role, String key) { if (role == Role.HOST) hostSecondFrame = key; else viewerSecondFrame = key; }
    public static FrameSelection getSelection(Role role) { return role == Role.HOST ? new FrameSelection(hostFirstFrame, hostSecondFrame) : new FrameSelection(viewerFirstFrame, viewerSecondFrame); }
    public static void clearSelection(Role role) { AUTO_SELECTED_FRAMES.remove(role); if (role == Role.HOST) { hostFirstFrame = null; hostSecondFrame = null; } else { viewerFirstFrame = null; viewerSecondFrame = null; } }

    public static int getScannedMapValue(String mapId) { Integer value = SHARED_SCANNED_MAPS.get(mapId); return value == null ? -1 : value; }
    public static Map<String, Integer> getSharedScannedMaps() { return new LinkedHashMap<>(SHARED_SCANNED_MAPS); }
    public static void replaceSharedScan(Map<String, Integer> values) { SHARED_SCANNED_MAPS.clear(); SHARED_SCANNED_MAPS.putAll(values); }
    public static String getHostName() { return hostName; }
    public static String getViewerName() { return viewerName; }
    public static void setHostName(String name) { hostName = name == null ? "" : name; }
    public static void setViewerName(String name) { viewerName = name == null ? "" : name; }
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

    public static void resetPositions() {
        hostX = -1;
        hostY = 8;
        viewerX = 8;
        viewerY = 8;
        hostScale = 1.0f;
        viewerScale = 1.0f;
    }

    public static void resetHudDefaults() {
        resetPositions();
        hostName = "Host";
        viewerName = "Viewer";
    }
}
