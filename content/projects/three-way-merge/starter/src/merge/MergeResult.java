package merge;

import java.util.List;

/** 병합 결과. 값 객체라 바뀌지 않는다. conflicted 는 충돌 블록이 하나라도 있었나다. */
public record MergeResult(boolean conflicted, List<String> lines) {
}
