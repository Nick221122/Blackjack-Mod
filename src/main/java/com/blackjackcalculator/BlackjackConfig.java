package com.blackjackcalculator;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.decoration.ItemFrameEntity;
import net.minecraft.registry.RegistryKey;
import net.minecraft.world.World;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

/** Client-only persistent settings and frame role assignments. */
public final class BlackjackConfig {
    public enum Role { HOST, VIEWER }

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final String FILE_NAME = "blackjackcalculator.json";

    private static int hostX = 8;
    private static int hostY = 8;
    private static int viewerX = -1;
    private static int viewerY = 8;
    private static final Map<String, Role> FRAME_ROLES = new HashMap<>();
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
            if (root.has("frameRoles") && root.get("frameRoles").isJsonObject()) {
                for (var entry : root.getAsJsonObject("frameRoles").entrySet()) {
                    String value = entry.getValue().getAsString();
                    try {
                        FRAME_ROLES.put(entry.getKey(), Role.valueOf(value));
                    } catch (IllegalArgumentException ignored) {
                        // Ignore unknown roles from an older/newer config.
                    }
                }
            }
        } catch (Exception ignored) {
            // A broken client config should never crash Minecraft.
        }
    }

    public static void save(MinecraftClient client) {
        if (client == null) return;
        Path path = client.runDirectory.toPath().resolve(FILE_NAME);
        JsonObject root = new JsonObject();
        root.addProperty("hostX", hostX);
        root.addProperty("hostY", hostY);
        root.addProperty("viewerX", viewerX);
        root.addProperty("viewerY", viewerY);
        JsonObject roles = new JsonObject();
        FRAME_ROLES.forEach((key, role) -> roles.addProperty(key, role.name()));
        root.add("frameRoles", roles);
        try {
            Files.writeString(path, GSON.toJson(root));
        } catch (IOException ignored) {
            // Config saving failure should never crash the game.
        }
    }

    public static Role getRole(ItemFrameEntity frame, RegistryKey<World> dimension) {
        return FRAME_ROLES.get(roleKey(frame, dimension));
    }

    public static void setRole(ItemFrameEntity frame, RegistryKey<World> dimension, Role role) {
        FRAME_ROLES.put(roleKey(frame, dimension), role);
    }

    public static void clearRole(ItemFrameEntity frame, RegistryKey<World> dimension) {
        FRAME_ROLES.remove(roleKey(frame, dimension));
    }

    private static String roleKey(ItemFrameEntity frame, RegistryKey<World> dimension) {
        return dimension.getValue() + ":" + frame.getUuid();
    }

    public static int getHostX() { return hostX; }
    public static int getHostY() { return hostY; }
    public static int getViewerX() { return viewerX; }
    public static int getViewerY() { return viewerY; }

    public static void setHostPosition(int x, int y) {
        hostX = x;
        hostY = y;
    }

    public static void setViewerPosition(int x, int y) {
        viewerX = x;
        viewerY = y;
    }

    public static void resetPositions() {
        hostX = 8;
        hostY = 8;
        viewerX = -1;
        viewerY = 8;
    }
}
