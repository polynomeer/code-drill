package tests;

import java.util.Set;
import slug.Slugs;
import static codedrill.Assertions.*;

/** 숨은 테스트. 사용자에게 나가지 않는다. */
public class HiddenSlugsTest {

    public void testDigitsStay() {
        assertEquals("top-10-tips-for-2024", Slugs.slugify("Top 10 Tips for 2024", 80));
    }

    public void testEdgesHaveNoDashes() {
        assertEquals("hello", Slugs.slugify("  ...Hello!!  ", 80));
        assertEquals("a-b", Slugs.slugify("-a-b-", 80));
    }

    public void testAccentsAreDropped() {
        assertEquals("cafe-creme", Slugs.slugify("Café Crème", 80));
        assertEquals("aeiou-n", Slugs.slugify("ÁÉÍÓÚ Ñ", 80));
    }

    public void testOtherScriptsSeparate() {
        assertEquals("seoul-2024", Slugs.slugify("서울 Seoul — 2024", 80));
        assertEquals("a-b", Slugs.slugify("a😀b", 80));
    }

    public void testApostrophesJoin() {
        assertEquals("dont-stop", Slugs.slugify("Don't stop", 80));
        assertEquals("its-here", Slugs.slugify("It\u2019s here", 80));
    }

    public void testNothingLeftIsUntitled() {
        assertEquals("untitled", Slugs.slugify("!!!", 80));
        assertEquals("untitled", Slugs.slugify("", 80));
        assertEquals("untitled", Slugs.slugify("한글만", 5));
    }

    public void testShortEnoughIsUntouched() {
        assertEquals("big-cat", Slugs.slugify("Big cat", 7));
    }

    public void testCutAtAWordEndKeepsTheWord() {
        assertEquals("big-cat", Slugs.slugify("Big cat dog", 7));
    }

    public void testCutInsideAWordDropsIt() {
        assertEquals("big", Slugs.slugify("Big cat dog", 6));
        assertEquals("big-cat", Slugs.slugify("Big cat dogs", 10));
    }

    public void testCutRightBeforeADash() {
        assertEquals("big", Slugs.slugify("Big cat", 3));
        assertEquals("big", Slugs.slugify("Big cat", 4));
    }

    public void testOneLongWordIsCutHard() {
        assertEquals("supercali", Slugs.slugify("Supercalifragilistic", 9));
        assertEquals("a", Slugs.slugify("ab cd", 1));
    }

    public void testMaxLengthMustBePositive() {
        assertThrows(IllegalArgumentException.class, () -> Slugs.slugify("x", 0));
    }

    public void testUniqueCountsUpFromTwo() {
        assertEquals("post-4", Slugs.unique("post", Set.of("post", "post-2", "post-3")));
    }

    public void testUniqueSkipsGaps() {
        assertEquals("post-2", Slugs.unique("post", Set.of("post", "post-3")));
    }

    public void testUniqueIgnoresSimilarNames() {
        assertEquals("post", Slugs.unique("post", Set.of("post-2", "posts")));
    }
}
