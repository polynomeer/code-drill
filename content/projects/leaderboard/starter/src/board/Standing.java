package board;

/** 순위표의 한 줄. 값 객체라 바뀌지 않는다. rank 는 경쟁 순위다 — 같은 점수는 같은 rank. */
public record Standing(int rank, String player, long score) {
}
