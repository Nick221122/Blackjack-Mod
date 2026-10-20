package com.blackjackcalculator;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;

/**
 * Selects local map representations that give the Host a legal winning hand
 * where possible. Actual map IDs are never changed.
 */
public final class HostAlwaysWinsStrategy {
    private HostAlwaysWinsStrategy() {}

    private record State(int fixedTotal, int aces) {}
    private record Solution(int total, List<Integer> displayIds) {}

    public static Map<Integer, Integer> chooseDisplayIds(
            List<Integer> actualHostMapIds,
            Set<Integer> viewerMapIds,
            Map<Integer, Integer> availableMapValues,
            int viewerTotal,
            boolean viewerHasCards,
            long seed) {
        Map<Integer, Integer> unchanged = new LinkedHashMap<>();
        if (actualHostMapIds == null || actualHostMapIds.isEmpty()
                || availableMapValues == null || availableMapValues.isEmpty()) {
            return unchanged;
        }

        Set<Integer> viewerIds = viewerMapIds == null ? Set.of() : new LinkedHashSet<>(viewerMapIds);
        Map<Integer, Integer> occurrences = new LinkedHashMap<>();
        for (int id : actualHostMapIds) {
            if (!availableMapValues.containsKey(id)) return unchanged;
            occurrences.merge(id, 1, Integer::sum);
        }

        List<Integer> hostIds = new ArrayList<>(occurrences.keySet());
        List<Integer> candidates = new ArrayList<>();
        for (Map.Entry<Integer, Integer> entry : availableMapValues.entrySet()) {
            int value = entry.getValue();
            if (value >= 1 && value <= 11 && !viewerIds.contains(entry.getKey())) {
                candidates.add(entry.getKey());
            }
        }
        if (candidates.isEmpty()) return unchanged;
        Collections.sort(candidates);

        Map<State, List<Integer>> states = new LinkedHashMap<>();
        states.put(new State(0, 0), List.of());

        for (int actualId : hostIds) {
            int count = occurrences.get(actualId);
            List<Integer> options;
            if (viewerIds.contains(actualId)) {
                // A map ID shared with the Viewer must never be substituted.
                options = List.of(actualId);
            } else {
                // Prefer a different captured map so Host-exclusive cards visibly use a fake face.
                options = candidates.stream().filter(id -> id != actualId).toList();
            }
            Map<State, List<Integer>> next = new LinkedHashMap<>();
            for (Map.Entry<State, List<Integer>> entry : states.entrySet()) {
                for (int displayId : options) {
                    Integer value = availableMapValues.get(displayId);
                    if (value == null || value < 1 || value > 11) continue;
                    State previous = entry.getKey();
                    State state = value == 11
                            ? new State(previous.fixedTotal(), previous.aces() + count)
                            : new State(previous.fixedTotal() + value * count, previous.aces());
                    if (!next.containsKey(state)) {
                        List<Integer> ids = new ArrayList<>(entry.getValue());
                        ids.add(displayId);
                        next.put(state, List.copyOf(ids));
                    }
                }
            }
            states = next;
        }

        List<Solution> legal = new ArrayList<>();
        List<Solution> fallback = new ArrayList<>();
        List<Solution> anyNonBust = new ArrayList<>();
        for (Map.Entry<State, List<Integer>> entry : states.entrySet()) {
            int total = BlackjackScore.score(entry.getKey().fixedTotal(), entry.getKey().aces());
            if (total > 21) continue;
            Solution solution = new Solution(total, entry.getValue());
            anyNonBust.add(solution);
            if (total >= 17) {
                fallback.add(solution);
                boolean beatsViewer = !viewerHasCards || viewerTotal > 21
                        || (viewerTotal < 21 && total > viewerTotal)
                        || (viewerTotal == 21 && total == 21);
                if (beatsViewer) legal.add(solution);
            }
        }

        // Aim for the lowest winning total or a 21 tie: 17 -> 18-21,
        // 18 -> 19-21, 19 -> 20-21, and 20/21 -> exactly 21.
        List<Solution> choices = legal;
        if (viewerHasCards && viewerTotal >= 17 && viewerTotal <= 21 && !legal.isEmpty()) {
            int targetMinimum = viewerTotal >= 20 ? 21 : viewerTotal + 1;
            choices = legal.stream().filter(solution -> solution.total() >= targetMinimum).toList();
        }
        if (choices.isEmpty()) {
            // If a perfect win is impossible with the captured faces, keep the best
            // non-bust Host face available rather than reverting to the original cards.
            int bestTotal = fallback.stream().mapToInt(Solution::total).max().orElse(-1);
            if (bestTotal >= 17) {
                choices = fallback.stream().filter(solution -> solution.total() == bestTotal).toList();
            } else {
                choices = anyNonBust;
            }
        }
        if (choices.isEmpty()) return unchanged;

        // Stable for the same hand and Viewer score; changes only when that score changes.
        Random random = new Random(seed);
        Solution chosen = choices.get(random.nextInt(choices.size()));
        if (chosen.displayIds().size() != hostIds.size()) return unchanged;

        for (int i = 0; i < hostIds.size(); i++) {
            unchanged.put(hostIds.get(i), chosen.displayIds().get(i));
        }
        return unchanged;
    }
}
