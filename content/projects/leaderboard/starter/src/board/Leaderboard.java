package board;

import java.util.List;

/**
 * 순위표 — 골격. 시그니처는 그대로 두고 본문을 채운다.
 */
public class Leaderboard {

    public void addScore(String player, long points) {
        throw new UnsupportedOperationException("TODO");
    }

    public int rank(String player) {
        throw new UnsupportedOperationException("TODO");
    }

    public List<Standing> top(int k) {
        throw new UnsupportedOperationException("TODO");
    }

    public void reset(String player) {
        throw new UnsupportedOperationException("TODO");
    }

    public int size() {
        throw new UnsupportedOperationException("TODO");
    }
}
