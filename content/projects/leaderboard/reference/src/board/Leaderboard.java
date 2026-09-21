package board;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Leaderboard {

    private final Map<String, Long> scores = new HashMap<>();

    public void addScore(String player, long points) {
        if (player == null || player.isEmpty()) throw new IllegalArgumentException("player must not be empty");
        long next = scores.getOrDefault(player, 0L) + points;
        if (next < 0) throw new IllegalArgumentException("score would go below zero: " + next);
        // 검사가 다 지난 뒤에만 적는다 — 처음 보는 선수도 거절되면 표에 오르지 않는다.
        scores.put(player, next);
    }

    public int rank(String player) {
        Long score = scores.get(player);
        if (score == null) throw new NoSuchPlayerException(player);
        return rankOf(score);
    }

    /** 경쟁 순위: 더 높은 점수의 선수 수 + 1. 같은 점수는 같은 순위다. */
    private int rankOf(long score) {
        int above = 0;
        for (long other : scores.values()) if (other > score) above++;
        return above + 1;
    }

    public List<Standing> top(int k) {
        if (k < 0) throw new IllegalArgumentException("k must not be negative: " + k);
        List<Map.Entry<String, Long>> rows = new ArrayList<>(scores.entrySet());
        rows.sort(Comparator.<Map.Entry<String, Long>>comparingLong(e -> -e.getValue()).thenComparing(Map.Entry::getKey));
        List<Standing> out = new ArrayList<>();
        for (int i = 0; i < Math.min(k, rows.size()); i++) {
            Map.Entry<String, Long> row = rows.get(i);
            out.add(new Standing(rankOf(row.getValue()), row.getKey(), row.getValue()));
        }
        return out;
    }

    public void reset(String player) {
        if (!scores.containsKey(player)) throw new NoSuchPlayerException(player);
        scores.put(player, 0L);
    }

    public int size() {
        return scores.size();
    }
}
