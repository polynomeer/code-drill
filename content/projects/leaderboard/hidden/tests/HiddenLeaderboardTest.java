package tests;

import board.Leaderboard;
import board.NoSuchPlayerException;
import board.Standing;
import java.util.List;
import static codedrill.Assertions.*;

/** 숨은 테스트. 사용자에게 나가지 않는다. */
public class HiddenLeaderboardTest {

    private static Leaderboard tied() {
        Leaderboard board = new Leaderboard();
        board.addScore("dan", 10);
        board.addScore("bob", 8);
        board.addScore("ann", 8);
        board.addScore("cid", 5);
        return board;
    }

    public void testCompetitionRankingSkipsAfterTie() {
        Leaderboard board = tied();
        assertEquals(1, board.rank("dan"));
        assertEquals(2, board.rank("ann"));
        assertEquals(2, board.rank("bob"));
        assertEquals(4, board.rank("cid"));
    }

    public void testTopBreaksTiesByNameAndCarriesRank() {
        Leaderboard board = tied();
        assertEquals(
            List.of(new Standing(1, "dan", 10), new Standing(2, "ann", 8), new Standing(2, "bob", 8), new Standing(4, "cid", 5)),
            board.top(10));
        assertEquals(List.of(new Standing(1, "dan", 10), new Standing(2, "ann", 8)), board.top(2));
        assertEquals(List.of(), board.top(0));
        assertThrows(IllegalArgumentException.class, () -> board.top(-1));
    }

    public void testResetKeepsThePlayerAtZero() {
        Leaderboard board = tied();
        board.reset("dan");
        assertEquals(4, board.size());
        assertEquals(4, board.rank("dan"));
        assertEquals(1, board.rank("ann"));
        assertEquals(new Standing(4, "dan", 0), board.top(4).get(3));
        assertThrows(NoSuchPlayerException.class, () -> board.reset("zed"));
    }

    public void testNegativePointsAllowedButNotBelowZero() {
        Leaderboard board = new Leaderboard();
        board.addScore("ann", 5);
        board.addScore("ann", -3);
        assertEquals(List.of(new Standing(1, "ann", 2)), board.top(1));
        assertThrows(IllegalArgumentException.class, () -> board.addScore("ann", -3));
        assertEquals(List.of(new Standing(1, "ann", 2)), board.top(1));
        assertThrows(IllegalArgumentException.class, () -> board.addScore("new", -1));
        assertEquals(1, board.size());
    }

    public void testAllTiedShareFirst() {
        Leaderboard board = new Leaderboard();
        board.addScore("b", 3);
        board.addScore("a", 3);
        board.addScore("c", 3);
        assertEquals(1, board.rank("c"));
        assertEquals(List.of("a", "b", "c"), board.top(3).stream().map(Standing::player).toList());
        assertEquals(List.of(1, 1, 1), board.top(3).stream().map(Standing::rank).toList());
    }

    public void testZeroScorePlayersStillRank() {
        Leaderboard board = new Leaderboard();
        board.addScore("ann", 0);
        board.addScore("bob", 1);
        assertEquals(2, board.rank("ann"));
        assertEquals(2, board.size());
    }

    public void testInvalidNames() {
        Leaderboard board = new Leaderboard();
        assertThrows(IllegalArgumentException.class, () -> board.addScore("", 1));
        assertThrows(IllegalArgumentException.class, () -> board.addScore(null, 1));
        assertEquals(0, board.size());
    }
}
