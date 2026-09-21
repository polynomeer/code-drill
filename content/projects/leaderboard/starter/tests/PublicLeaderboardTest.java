package tests;

import board.Leaderboard;
import board.NoSuchPlayerException;
import board.Standing;
import java.util.List;
import static codedrill.Assertions.*;

/** 공개 테스트. 채점은 이것과 숨은 테스트를 함께 돌린다. */
public class PublicLeaderboardTest {

    public void testScoresAccumulateAndRank() {
        Leaderboard board = new Leaderboard();
        board.addScore("ann", 5);
        board.addScore("bob", 3);
        board.addScore("ann", 2);
        assertEquals(1, board.rank("ann"));
        assertEquals(2, board.rank("bob"));
        assertEquals(2, board.size());
    }

    public void testTopIsOrderedByScore() {
        Leaderboard board = new Leaderboard();
        board.addScore("ann", 5);
        board.addScore("bob", 9);
        board.addScore("cid", 1);
        assertEquals(List.of(new Standing(1, "bob", 9), new Standing(2, "ann", 5)), board.top(2));
    }

    public void testUnknownPlayer() {
        Leaderboard board = new Leaderboard();
        assertThrows(NoSuchPlayerException.class, () -> board.rank("nobody"));
    }
}
