package merge;

import java.util.List;

/**
 * 세 판 병합 — 골격. 시그니처와 표시 문자열은 그대로 두고 본문을 채운다.
 */
public final class ThreeWayMerge {

    public static final String OURS_MARKER = "<<<<<<< ours";
    public static final String SPLIT_MARKER = "=======";
    public static final String THEIRS_MARKER = ">>>>>>> theirs";

    private ThreeWayMerge() { }

    public static MergeResult merge(List<String> base, List<String> ours, List<String> theirs) {
        throw new UnsupportedOperationException("TODO");
    }
}
