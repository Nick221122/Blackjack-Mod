package com.blackjackcalculator;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class HostAlwaysWinsStrategyTest {
    private static final Map<Integer, Integer> VALUES = Map.of(
            1, 11, 2, 3, 3, 4, 4, 5, 5, 6, 6, 7, 7, 8, 8, 9, 9, 10
    );

    @Test
    void choosesAValidWinningTotalRandomlyWhenViewerHasSeventeen() {
        Map<Integer, Integer> overrides = HostAlwaysWinsStrategy.chooseDisplayIds(
                List.of(8, 9), Set.of(), VALUES, 17, true, 42L);

        assertEquals(2, overrides.size());
        int total = scoreDisplayIds(overrides, List.of(8, 9), VALUES);
        assertTrue(total >= 18 && total <= 21, "host total should beat 17 without busting");
    }

    @Test
    void neverChangesMapIdsSharedWithViewer() {
        Map<Integer, Integer> overrides = HostAlwaysWinsStrategy.chooseDisplayIds(
                List.of(1, 2), Set.of(1), VALUES, 17, true, 7L);

        assertEquals(1, overrides.get(1));
        assertTrue(overrides.containsKey(2));
    }

    @Test
    void stillChoosesAFakeNonBustFaceWhenHostCannotReachSeventeen() {
        Map<Integer, Integer> overrides = HostAlwaysWinsStrategy.chooseDisplayIds(
                List.of(2), Set.of(), VALUES, 17, true, 11L);

        assertEquals(1, overrides.size());
        assertNotEquals(2, overrides.get(2), "Host-exclusive card should use a fake face when alternatives exist");
        assertTrue(VALUES.get(overrides.get(2)) <= 11);
    }

    @Test
    void leavesInputsWithoutScannedRepresentationsUnchanged() {
        assertTrue(HostAlwaysWinsStrategy.chooseDisplayIds(
                List.of(99), Set.of(), VALUES, 17, true, 7L).isEmpty());
    }

    private static int scoreDisplayIds(Map<Integer, Integer> overrides, List<Integer> actualIds,
                                       Map<Integer, Integer> values) {
        int fixed = 0;
        int aces = 0;
        for (int actualId : actualIds) {
            int displayId = overrides.getOrDefault(actualId, actualId);
            int value = values.get(displayId);
            if (value == 11) aces++;
            else fixed += value;
        }
        return BlackjackScore.score(fixed, aces);
    }
}
