package com.blackjackcalculator;

import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class HostAlwaysWinsStrategyTest {
    private static Map<Integer, Integer> values(int... pairs) {
        Map<Integer, Integer> result = new LinkedHashMap<>();
        for (int i = 0; i < pairs.length; i += 2) result.put(pairs[i], pairs[i + 1]);
        return result;
    }

    @Test
    void choosesFakeMapsThatBeatViewerWithoutBustWhenPossible() {
        Map<Integer, Integer> choices = HostAlwaysWinsStrategy.chooseDisplayIds(
                List.of(1, 2), values(1, 8, 2, 9, 3, 10, 4, 11), 18, true);

        assertEquals(2, choices.size());
        int total = 0;
        int aces = 0;
        for (int displayId : choices.values()) {
            int value = Map.of(1, 8, 2, 9, 3, 10, 4, 11).get(displayId);
            if (value == 11) aces++;
            else total += value;
        }
        assertTrue(BlackjackScore.score(total, aces) > 18);
        assertTrue(BlackjackScore.score(total, aces) <= 21);
    }

    @Test
    void choosesDealerMinimumWhenViewerHasALowTotal() {
        Map<Integer, Integer> choices = HostAlwaysWinsStrategy.chooseDisplayIds(
                List.of(1, 2), values(1, 3, 2, 8, 3, 9), 10, true);

        int total = 0;
        for (int id : choices.values()) total += values(1, 3, 2, 8, 3, 9).get(id);
        assertTrue(total >= 17 && total <= 21);
    }

    @Test
    void doesNotAssignUnscannedHostMaps() {
        assertTrue(HostAlwaysWinsStrategy.chooseDisplayIds(
                List.of(999), values(1, 8, 2, 9), 18, true).isEmpty());
    }

    @Test
    void withNoViewerCardsStillTargetsDealerStandingRange() {
        Map<Integer, Integer> choices = HostAlwaysWinsStrategy.chooseDisplayIds(
                List.of(1, 2), values(1, 8, 2, 9, 3, 10), 0, false);
        int total = 0;
        for (int id : choices.values()) total += values(1, 8, 2, 9, 3, 10).get(id);
        assertTrue(total >= 17 && total <= 21);
    }
}
