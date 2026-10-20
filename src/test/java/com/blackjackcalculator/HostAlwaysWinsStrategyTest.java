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
        Map<Integer, Integer> available = values(1, 8, 2, 9, 3, 10, 4, 11);
        Map<Integer, Integer> choices = HostAlwaysWinsStrategy.chooseDisplayIds(
                List.of(1, 2), available, 18, true);

        assertEquals(2, choices.size());
        int total = 0;
        int aces = 0;
        for (int displayId : choices.values()) {
            int value = available.get(displayId);
            if (value == 11) aces++;
            else total += value;
        }
        assertTrue(BlackjackScore.score(total, aces) > 18);
        assertTrue(BlackjackScore.score(total, aces) <= 21);
    }

    @Test
    void targetsTwentyWhenViewerHasNineteen() {
        Map<Integer, Integer> available = values(1, 10, 2, 10, 3, 9, 4, 11);
        Map<Integer, Integer> choices = HostAlwaysWinsStrategy.chooseDisplayIds(
                List.of(1, 2), available, 19, true);

        assertEquals(20, scoreForHostFrames(List.of(1, 2), choices, available));
    }

    @Test
    void repeatedHostMapIdsAreCountedForEveryFrame() {
        List<Integer> hostFrames = List.of(1, 1, 2);
        Map<Integer, Integer> available = values(1, 10, 2, 10, 3, 9, 4, 11);
        Map<Integer, Integer> choices = HostAlwaysWinsStrategy.chooseDisplayIds(
                hostFrames, available, 19, true);

        int actualScore = scoreForHostFrames(hostFrames, choices, available);
        assertTrue(actualScore > 19, "Host's repeated map frames must still beat Viewer");
        assertTrue(actualScore <= 21, "Host's repeated map frames must not bust");
    }

    private static int scoreForHostFrames(
            List<Integer> hostFrames, Map<Integer, Integer> choices, Map<Integer, Integer> available) {
        int fixedTotal = 0;
        int aces = 0;
        for (int actualId : hostFrames) {
            Integer displayId = choices.get(actualId);
            if (displayId == null) return -1;
            int value = available.get(displayId);
            if (value == 11) aces++;
            else fixedTotal += value;
        }
        return BlackjackScore.score(fixedTotal, aces);
    }

    @Test
    void choosesDealerMinimumWhenViewerHasALowTotal() {
        Map<Integer, Integer> available = values(1, 3, 2, 8, 3, 9);
        Map<Integer, Integer> choices = HostAlwaysWinsStrategy.chooseDisplayIds(
                List.of(1, 2), available, 10, true);

        int total = 0;
        for (int id : choices.values()) total += available.get(id);
        assertTrue(total >= 17 && total <= 21);
    }

    @Test
    void doesNotAssignUnscannedHostMaps() {
        assertTrue(HostAlwaysWinsStrategy.chooseDisplayIds(
                List.of(999), values(1, 8, 2, 9), 18, true).isEmpty());
    }

    @Test
    void withNoViewerCardsStillTargetsDealerStandingRange() {
        Map<Integer, Integer> available = values(1, 8, 2, 9, 3, 10);
        Map<Integer, Integer> choices = HostAlwaysWinsStrategy.chooseDisplayIds(
                List.of(1, 2), available, 0, false);
        int total = 0;
        for (int id : choices.values()) total += available.get(id);
        assertTrue(total >= 17 && total <= 21);
    }
}
