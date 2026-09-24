package com.blackjackcalculator;

import net.minecraft.client.MinecraftClient;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.MapIdComponent;
import net.minecraft.item.ItemStack;
import net.minecraft.item.map.MapState;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

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

    public static void clear() {
        REPRESENTATIONS.clear();
    }

    static boolean shouldSimulateForFrame(boolean simulationEnabled, boolean selectedFrame, boolean hasRepresentation) {
        return simulationEnabled && selectedFrame && hasRepresentation;
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

            REPRESENTATIONS.putIfAbsent(mapId.id(), state.copy());
            scanned++;
        }
        return scanned;
    }
}
