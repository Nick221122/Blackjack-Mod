package com.blackjackcalculator;

import net.minecraft.client.MinecraftClient;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.MapIdComponent;
import net.minecraft.item.ItemStack;
import net.minecraft.item.map.MapState;
import net.minecraft.block.MapColor;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Client-only store of scanned map visual representations.
 *
 * The server-authoritative ItemStack is never changed. The scanned MapState
 * is only used as a local rendering source when simulation is enabled.
 */
public final class MapSimulationStore {
    private static final Map<Integer, MapState> REPRESENTATIONS = new LinkedHashMap<>();
    private static boolean enabled;

    private MapSimulationStore() {}

    public static boolean isEnabled() {
        return enabled;
    }

    public static void toggle() {
        enabled = !enabled;
    }

    public static void setEnabled(boolean value) {
        enabled = value;
    }

    public static void clear() {
        REPRESENTATIONS.clear();
    }

    public static MapState resolve(MapIdComponent actualMapId) {
        if (!enabled || actualMapId == null || REPRESENTATIONS.isEmpty()) return null;
        Integer displayId = MapSimulationSelection.selectDisplayId(actualMapId.id(), List.copyOf(REPRESENTATIONS.keySet()));
        return displayId == null ? null : REPRESENTATIONS.get(displayId);
    }

    public static int scanContainer(MinecraftClient client, Iterable<ItemStack> stacks) {
        if (client == null || client.world == null) return 0;

        int scanned = 0;
        for (ItemStack stack : stacks) {
            if (stack == null || stack.isEmpty()) continue;
            MapIdComponent mapId = stack.get(DataComponentTypes.MAP_ID);
            if (mapId == null) continue;

            MapState state = client.world.getMapState(mapId);
            if (state == null) continue;

            REPRESENTATIONS.put(mapId.id(), state);
            saveMapImage(client, mapId.id(), state);
            scanned++;
        }
        return scanned;
    }

    private static void saveMapImage(MinecraftClient client, int mapId, MapState state) {
        try {
            Path directory = client.runDirectory.toPath().resolve("blackjack-maps");
            Files.createDirectories(directory);
            BufferedImage image = new BufferedImage(128, 128, BufferedImage.TYPE_INT_ARGB);
            for (int y = 0; y < 128; y++) {
                for (int x = 0; x < 128; x++) {
                    int value = state.colors[y * 128 + x] & 0xFF;
                    if (value == 0) {
                        image.setRGB(x, y, 0x00000000);
                        continue;
                    }
                    image.setRGB(x, y, 0xFF000000 | (MapColor.getRenderColor(value) & 0xFFFFFF));
                }
            }
            ImageIO.write(image, "png", directory.resolve("map-" + mapId + ".png").toFile());
        } catch (IOException ignored) {
            // Map scanning should continue even if the local PNG cannot be written.
        }
    }
}
