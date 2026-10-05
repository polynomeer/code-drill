package tests;

import java.util.List;
import semver.Range;
import semver.Version;
import static codedrill.Assertions.*;

/** 공개 테스트. 채점은 이것과 숨은 테스트를 함께 돌린다. */
public class PublicSemverTest {

    private static Version v(String text) {
        return Version.parse(text);
    }

    public void testParseParts() {
        Version version = v("1.20.3-beta.2+build.7");
        assertEquals(1, version.major());
        assertEquals(20, version.minor());
        assertEquals(3, version.patch());
        assertEquals(List.of("beta", "2"), version.prerelease());
        assertEquals("build.7", version.build());
        assertTrue(version.isPrerelease());
    }

    public void testRejectsMalformed() {
        assertThrows(IllegalArgumentException.class, () -> v("1.2"));
        assertThrows(IllegalArgumentException.class, () -> v("01.2.3"));
        assertThrows(IllegalArgumentException.class, () -> v("1.2.3-"));
    }

    public void testNumbersCompareNumerically() {
        assertTrue(v("1.10.0").compareTo(v("1.9.0")) > 0);
        assertTrue(v("2.0.0").compareTo(v("10.0.0")) < 0);
    }

    public void testReleaseIsAbovePrerelease() {
        assertTrue(v("1.0.0").compareTo(v("1.0.0-rc.1")) > 0);
    }

    public void testCaretAndTilde() {
        Range caret = Range.parse("^1.2.3");
        assertTrue(caret.satisfies(v("1.9.0")));
        assertFalse(caret.satisfies(v("2.0.0")));
        assertFalse(caret.satisfies(v("1.2.2")));
        Range tilde = Range.parse("~1.2.3");
        assertTrue(tilde.satisfies(v("1.2.9")));
        assertFalse(tilde.satisfies(v("1.3.0")));
    }

    public void testUnionAndMax() {
        Range range = Range.parse(">=1.0.0 <1.5.0 || 3.0.0");
        assertTrue(range.satisfies(v("1.4.9")));
        assertTrue(range.satisfies(v("3.0.0")));
        assertFalse(range.satisfies(v("2.0.0")));
        assertEquals(v("1.4.0"), range.maxSatisfying(List.of(v("1.0.0"), v("1.4.0"), v("2.0.0"))));
        assertNull(range.maxSatisfying(List.of(v("0.9.0"))));
    }
}
