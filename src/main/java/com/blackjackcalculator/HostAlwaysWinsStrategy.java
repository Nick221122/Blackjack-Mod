package com.blackjackcalculator;

import java.util.ArrayList;
import java.util.LinkedHashMap;
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

        // Overrides are keyed by actual map ID, so repeated frames containing
        // the same map must share a representation. Count their multiplicity
        // when evaluating totals, otherwise the chosen visuals can bust or
        // disagree with the score calculated from the frames.
        Map<Integer, Integer> occurrences = new LinkedHashMap<>();
        for (int id : actualHostMapIds) {
            occurrences.merge(id, 1, Integer::sum);
        }
        List<Integer> hostIds = new ArrayList<>(occurrences.keySet());
        hostIds.removeIf(id -> !availableMapValues.containsKey(id));
        if (hostIds.isEmpty()) return result;

        Map<State, List<Integer>> states = new LinkedHashMap<>();
        states.put(new State(0, 0), List.of());

        for (int actualId : hostIds) {
            int count = occurrences.get(actualId);
            Map<State, List<Integer>> next = new LinkedHashMap<>();
            for (Map.Entry<State, List<Integer>> entry : states.entrySet()) {
                for (Map.Entry<Integer, Integer> option : availableMapValues.entrySet()) {
                    int value = option.getValue();
                    if (value < 1 || value > 11) continue;
                    State previous = entry.getKey();
                    State state = value == 11
                            ? new State(previous.fixedTotal(), previous.aces() + count)
                            : new State(previous.fixedTotal() + value * count, previous.aces());
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
