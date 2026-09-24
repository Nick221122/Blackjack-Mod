package com.blackjackcalculator;

import net.minecraft.client.MinecraftClient;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.MapIdComponent;
import net.minecraft.item.ItemStack;
import net.minecraft.item.map.MapState;

import java.util.LinkedHashMap;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Client-only store of scanned map visual representations.
 *
 * The server-authoritative ItemStack is never changed. The scanned MapState
 * is only used as a local rendering source when simulation is enabled.
 */
public final class MapSimulationStore {
    private static final Map<Integer, MapState> REPRESENTATIONS = new LinkedHashMap<>();
    private static Map<Integer, Integer> SIMULATION_MAPPING = Map.of();
    private static boolean enabled;

    private MapSimulationStore() {}

    public static boolean isEnabled() {
        return enabled;
    }

    public static void toggle() {
        enabled = !enabled;
        if (enabled) rebuildSimulationMapping();
        else SIMULATION_MAPPING = Map.of();
    }

    public static void clear() {
        REPRESENTATIONS.clear();
        SIMULATION_MAPPING = Map.of();
    }

    public static Integer resolveDisplayId(MapIdComponent actualMapId) {
        if (!enabled || actualMapId == null || REPRESENTATIONS.isEmpty()) return null;
        return MapSimulationSelection.selectMappedDisplayId(actualMapId.id(), SIMULATION_MAPPING);
    }

    public static MapState resolve(MapIdComponent actualMapId) {
        Integer displayId = resolveDisplayId(actualMapId);
        return displayId == null ? null : REPRESENTATIONS.get(displayId);
    }

    /** Returns the fake card value that corresponds to the simulated map shown for this frame. */
    public static Integer resolveValue(MapIdComponent actualMapId) {
        if (!enabled || actualMapId == null) return null;
        Integer simulatedMapId = MapSimulationSelection.selectMappedDisplayId(
                actualMapId.id(), SIMULATION_MAPPING);
        if (simulatedMapId == null) return null;
        int value = BlackjackConfig.getScannedMapValue(simulatedMapId);
        return value < 0 ? null : value;
    }

    private static void rebuildSimulationMapping() {
        List<Integer> knownIds = new ArrayList<>(BlackjackConfig.getScannedMapValuesById().keySet());
        for (Integer id : REPRESENTATIONS.keySet()) {
            if (!knownIds.contains(id)) knownIds.add(id);
        }
        SIMULATION_MAPPING = MapSimulationSelection.buildPermutation(
                knownIds,
                ThreadLocalRandom.current().nextLong()
        );
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
            scanned++;
        }
        if (enabled) rebuildSimulationMapping();
        return scanned;
    }
}
