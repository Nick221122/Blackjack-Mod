package com.blackjackcalculator;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;

/**
 * Picks fake map representations for host-side frames so the displayed host
 * hand reaches a dealer-legal total that beats the viewer whenever possible.
 */
public final class HostAlwaysWinsStrategy {
    private HostAlwaysWinsStrategy() {}

    private record State(int fixedTotal, int aces) {}

    public static Map<Integer, Integer> chooseDisplayIds(
            List<Integer> actualHostMapIds,
            Map<Integer, Integer> availableMapValues,
            int viewerTotal,
            boolean viewerHasCards) {
        Map<Integer, Integer> result = new LinkedHashMap<>();
        if (actualHostMapIds == null || actualHostMapIds.isEmpty()
                || availableMapValues == null || availableMapValues.isEmpty()) {
            return result;
        }

        List<Integer> hostIds = new ArrayList<>(new LinkedHashSet<>(actualHostMapIds));
        hostIds.removeIf(id -> !availableMapValues.containsKey(id));
        if (hostIds.isEmpty()) return result;

        Map<State, List<Integer>> states = new LinkedHashMap<>();
        states.put(new State(0, 0), List.of());

        for (int ignored : hostIds) {
            Map<State, List<Integer>> next = new LinkedHashMap<>();
            for (Map.Entry<State, List<Integer>> entry : states.entrySet()) {
                for (Map.Entry<Integer, Integer> option : availableMapValues.entrySet()) {
                    int value = option.getValue();
                    if (value < 1 || value > 11) continue;
                    State previous = entry.getKey();
                    State state = value == 11
                            ? new State(previous.fixedTotal(), previous.aces() + 1)
                            : new State(previous.fixedTotal() + value, previous.aces());
                    if (!next.containsKey(state)) {
                        List<Integer> ids = new ArrayList<>(entry.getValue());
                        ids.add(option.getKey());
                        next.put(state, List.copyOf(ids));
                    }
                }
            }
            states = next;
        }

        int target = !viewerHasCards || viewerTotal > 21
                ? 17
                : Math.max(17, Math.min(21, viewerTotal + 1));

        int bestScore = -1;
        List<Integer> bestIds = List.of();
        for (Map.Entry<State, List<Integer>> entry : states.entrySet()) {
            State state = entry.getKey();
            int score = BlackjackScore.score(state.fixedTotal(), state.aces());
            if (score > 21) continue;
            if (score >= target && (bestScore < target || score < bestScore)) {
                bestScore = score;
                bestIds = entry.getValue();
            } else if (bestScore < target && score > bestScore) {
                bestScore = score;
                bestIds = entry.getValue();
            }
        }

        if (bestIds.size() != hostIds.size()) return result;
        for (int i = 0; i < hostIds.size(); i++) {
            result.put(hostIds.get(i), bestIds.get(i));
        }
        return result;
    }
}
