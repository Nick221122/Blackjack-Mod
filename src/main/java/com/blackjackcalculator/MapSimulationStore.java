package com.blackjackcalculator;

import net.minecraft.client.MinecraftClient;
import net.minecraft.component.type.MapIdComponent;
import net.minecraft.item.map.MapState;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Client-only collection of scanned map representations.
 *
 * The key is the server-provided map id captured during scanning; the value is
 * a copy of that map's visual state. The server item is never modified.
 */
public final class MapSimulationStore {
    private static final Map<MapIdComponent, MapState> REPRESENTATIONS = new LinkedHashMap<>();
    private static boolean enabled;

    private MapSimulationStore() {}

    public static void setEnabled(boolean value) {
        enabled = value;
    }

    public static boolean isEnabled() {
        return enabled;
    }

    public static void toggle() {
        enabled = !enabled;
    }

    public static void clear() {
        REPRESENTATIONS.clear();
    }

    public static int size() {
        return REPRESENTATIONS.size();
    }

    public static void scan(MapIdComponent mapId, MapState state) {
        if (mapId == null || state == null) return;
        REPRESENTATIONS.put(mapId, state.copy());
    }

    public static MapState resolve(MapIdComponent actualMapId) {
        if (!enabled || actualMapId == null || REPRESENTATIONS.isEmpty()) return null;
        List<Integer> ids = REPRESENTATIONS.keySet().stream().map(MapIdComponent::id).toList();
        Integer displayId = MapSimulationSelection.selectDisplayId(actualMapId.id(), ids);
        if (displayId == null) return null;
        for (Map.Entry<MapIdComponent, MapState> entry : REPRESENTATIONS.entrySet()) {
            if (entry.getKey().id() == displayId) return entry.getValue();
        }
        return null;
    }

    /** Captures the map representations currently present in a dispenser/dropper. */
    public static int scanContainer(MinecraftClient client, Iterable<net.minecraft.item.ItemStack> stacks) {
        if (client == null || client.world == null) return 0;
        int scanned = 0;
        for (net.minecraft.item.ItemStack stack : stacks) {
            MapIdComponent id = stack.get(net.minecraft.component.DataComponentTypes.MAP_ID);
            if (id == null) continue;
            MapState state = client.world.getMapState(id);
            if (state == null) continue;
            scan(id, state);
            scanned++;
        }
        return scanned;
    }
}
