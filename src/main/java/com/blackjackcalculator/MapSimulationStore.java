package com.hostalwayswins;

import net.minecraft.component.type.MapIdComponent;
import net.minecraft.item.map.MapState;

import java.util.LinkedHashMap;
import java.util.Map;

/** Client-only map-state substitutions. The real map IDs and server data stay untouched. */
public final class MapSimulationStore {
    private static final Map<Integer, MapState> REPRESENTATIONS = new LinkedHashMap<>();
    private static final Map<Integer, Integer> HOST_DISPLAY_OVERRIDES = new LinkedHashMap<>();
    private static boolean enabled;

    private MapSimulationStore() {}

    public static void capture(int mapId, MapState state) {
        if (state != null) REPRESENTATIONS.put(mapId, state);
    }

    public static boolean hasRepresentation(int mapId) {
        return REPRESENTATIONS.containsKey(mapId);
    }

    public static void setEnabled(boolean value) {
        enabled = value;
        if (!value) HOST_DISPLAY_OVERRIDES.clear();
    }

    public static boolean isEnabled() {
        return enabled;
    }

    public static void setHostDisplayOverrides(Map<Integer, Integer> overrides) {
        HOST_DISPLAY_OVERRIDES.clear();
        if (!enabled || overrides == null) return;
        overrides.forEach((actualId, displayId) -> {
            if (actualId != null && displayId != null
                    && !actualId.equals(displayId)
                    && REPRESENTATIONS.containsKey(actualId)
                    && REPRESENTATIONS.containsKey(displayId)) {
                HOST_DISPLAY_OVERRIDES.put(actualId, displayId);
            }
        });
    }

    public static MapState resolve(MapIdComponent actualMapId) {
        if (!enabled || actualMapId == null) return null;
        Integer displayId = HOST_DISPLAY_OVERRIDES.get(actualMapId.id());
        return displayId == null ? null : REPRESENTATIONS.get(displayId);
    }

    public static void clear() {
        REPRESENTATIONS.clear();
        HOST_DISPLAY_OVERRIDES.clear();
        enabled = false;
    }
}
