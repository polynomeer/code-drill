package tests;

import java.util.List;
import merge.MergeResult;
import merge.ThreeWayMerge;
import static codedrill.Assertions.*;

/** 공개 테스트. 채점은 이것과 숨은 테스트를 함께 돌린다. */
public class PublicMergeTest {

    public void testNobodyChanged() {
        MergeResult result = ThreeWayMerge.merge(List.of("a", "b"), List.of("a", "b"), List.of("a", "b"));
        assertFalse(result.conflicted());
        assertEquals(List.of("a", "b"), result.lines());
    }

    public void testOnlyOursChanged() {
        MergeResult result = ThreeWayMerge.merge(List.of("a", "b", "c"), List.of("a", "B", "c"), List.of("a", "b", "c"));
        assertFalse(result.conflicted());
        assertEquals(List.of("a", "B", "c"), result.lines());
    }

    public void testBothChangedDifferently() {
        MergeResult result = ThreeWayMerge.merge(List.of("a", "b", "c"), List.of("a", "B", "c"), List.of("a", "C", "c"));
        assertTrue(result.conflicted());
        assertEquals(
            List.of("a", "<<<<<<< ours", "B", "=======", "C", ">>>>>>> theirs", "c"),
            result.lines());
    }

    public void testNullIsRejected() {
        assertThrows(IllegalArgumentException.class, () -> ThreeWayMerge.merge(List.of("a"), null, List.of("a")));
    }
}
