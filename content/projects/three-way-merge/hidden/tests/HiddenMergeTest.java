package tests;

import java.util.ArrayList;
import java.util.List;
import merge.MergeResult;
import merge.ThreeWayMerge;
import static codedrill.Assertions.*;

/** 숨은 스위트. 사용자에게 나가지 않는다 (§8.3). */
public class HiddenMergeTest {

    public void testIdenticalChangeAppearsOnce() {
        MergeResult result = ThreeWayMerge.merge(List.of("a", "b", "c"), List.of("a", "B", "c"), List.of("a", "B", "c"));
        assertFalse(result.conflicted());
        assertEquals(List.of("a", "B", "c"), result.lines());
    }

    public void testDeleteVersusEditConflicts() {
        MergeResult result = ThreeWayMerge.merge(List.of("a", "b", "c"), List.of("a", "c"), List.of("a", "B", "c"));
        assertTrue(result.conflicted());
        assertEquals(
            List.of("a", "<<<<<<< ours", "=======", "B", ">>>>>>> theirs", "c"),
            result.lines());
    }

    public void testBothDeletedTheSameLine() {
        MergeResult result = ThreeWayMerge.merge(List.of("a", "b", "c"), List.of("a", "c"), List.of("a", "c"));
        assertFalse(result.conflicted());
        assertEquals(List.of("a", "c"), result.lines());
    }

    public void testDisjointChangesBothApply() {
        MergeResult result = ThreeWayMerge.merge(
            List.of("a", "b", "c", "d"), List.of("A", "b", "c", "d"), List.of("a", "b", "c", "D"));
        assertFalse(result.conflicted());
        assertEquals(List.of("A", "b", "c", "D"), result.lines());
    }

    public void testAppendAtTheEnd() {
        MergeResult result = ThreeWayMerge.merge(List.of("a"), List.of("a", "b"), List.of("a"));
        assertFalse(result.conflicted());
        assertEquals(List.of("a", "b"), result.lines());
    }

    public void testBothAppendDifferentTails() {
        MergeResult result = ThreeWayMerge.merge(List.of("a"), List.of("a", "x"), List.of("a", "y"));
        assertTrue(result.conflicted());
        assertEquals(List.of("a", "<<<<<<< ours", "x", "=======", "y", ">>>>>>> theirs"), result.lines());
    }

    public void testConflictFlagSurvivesLaterCleanChunk() {
        MergeResult result = ThreeWayMerge.merge(
            List.of("a", "b", "c", "d"), List.of("a", "B", "c", "D"), List.of("a", "C", "c", "d"));
        assertTrue(result.conflicted());
        assertEquals(
            List.of("a", "<<<<<<< ours", "B", "=======", "C", ">>>>>>> theirs", "c", "D"),
            result.lines());
    }

    public void testEmptyBaseWithIdenticalAdditions() {
        MergeResult result = ThreeWayMerge.merge(List.of(), List.of("x"), List.of("x"));
        assertFalse(result.conflicted());
        assertEquals(List.of("x"), result.lines());
    }

    public void testAllEmpty() {
        MergeResult result = ThreeWayMerge.merge(List.of(), List.of(), List.of());
        assertFalse(result.conflicted());
        assertEquals(List.of(), result.lines());
    }

    public void testWhitespaceIsPartOfTheLine() {
        MergeResult result = ThreeWayMerge.merge(List.of("a"), List.of("a "), List.of("a"));
        assertFalse(result.conflicted());
        assertEquals(List.of("a "), result.lines());
    }

    public void testResultLinesAreImmutable() {
        MergeResult result = ThreeWayMerge.merge(List.of("a"), List.of("a"), List.of("a"));
        assertThrows(UnsupportedOperationException.class, () -> result.lines().add("b"));
    }

    public void testCallerListsAreNotModified() {
        List<String> ours = new ArrayList<>(List.of("a", "B"));
        ThreeWayMerge.merge(List.of("a", "b"), ours, List.of("a", "b"));
        assertEquals(List.of("a", "B"), ours);
    }

    public void testLongRunsStayAligned() {
        List<String> base = new ArrayList<>();
        for (int i = 0; i < 200; i++) base.add("line " + i);
        List<String> ours = new ArrayList<>(base);
        ours.set(10, "ours only");
        List<String> theirs = new ArrayList<>(base);
        theirs.set(190, "theirs only");
        MergeResult result = ThreeWayMerge.merge(base, ours, theirs);
        assertFalse(result.conflicted());
        assertEquals(200, result.lines().size());
        assertEquals("ours only", result.lines().get(10));
        assertEquals("theirs only", result.lines().get(190));
    }
}
