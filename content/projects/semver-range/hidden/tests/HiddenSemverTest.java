package tests;

import java.util.List;
import semver.Range;
import semver.Version;
import static codedrill.Assertions.*;

/** 숨은 테스트. 사용자에게 나가지 않는다. */
public class HiddenSemverTest {

    private static Version v(String text) {
        return Version.parse(text);
    }

    private static boolean in(String range, String version) {
        return Range.parse(range).satisfies(v(version));
    }

    public void testRejectsMalformedVersions() {
        for (String bad : List.of("", "1", "1.2.3.4", "1..3", "a.b.c", "1.2.-3", "1.02.3", "1.2.3-01",
                "1.2.3-a..b", "1.2.3+", "1.2.3-a_b", "99999999999.0.0", " 1.2.3", "v1.2.3")) {
            assertThrows(IllegalArgumentException.class, () -> v(bad));
        }
        assertThrows(IllegalArgumentException.class, () -> v(null));
    }

    public void testAcceptsEdgeVersions() {
        assertEquals(0, v("0.0.0").major());
        assertEquals(List.of("alpha-1", "0"), v("1.0.0-alpha-1.0").prerelease());
        assertEquals("001.x-y", v("1.0.0+001.x-y").build());
        assertFalse(v("1.0.0+build").isPrerelease());
    }

    public void testPrereleaseNumbersCompareNumerically() {
        assertTrue(v("1.0.0-alpha.10").compareTo(v("1.0.0-alpha.9")) > 0);
        assertTrue(v("1.0.0-2").compareTo(v("1.0.0-10")) < 0);
    }

    public void testPrereleasePrecedenceChain() {
        List<String> chain = List.of("1.0.0-alpha", "1.0.0-alpha.1", "1.0.0-alpha.beta", "1.0.0-beta",
            "1.0.0-beta.2", "1.0.0-beta.11", "1.0.0-rc.1", "1.0.0");
        for (int i = 0; i + 1 < chain.size(); i++) {
            assertTrue(v(chain.get(i)).compareTo(v(chain.get(i + 1))) < 0);
            assertTrue(v(chain.get(i + 1)).compareTo(v(chain.get(i))) > 0);
        }
    }

    public void testNumericIdentifierBelowAlphanumeric() {
        assertTrue(v("1.0.0-1").compareTo(v("1.0.0-a")) < 0);
        assertTrue(v("1.0.0-Z").compareTo(v("1.0.0-a")) < 0);
    }

    public void testBuildIgnoredInOrder() {
        assertEquals(0, v("1.0.0+b").compareTo(v("1.0.0+a")));
        assertEquals(v("1.0.0+zzz"), v("1.0.0"));
        assertEquals(v("1.0.0+a").hashCode(), v("1.0.0+b").hashCode());
        assertTrue(in("=1.0.0", "1.0.0+anything"));
    }

    public void testMaxKeepsFirstOfEqualVersions() {
        Range any = Range.parse("*");
        Version picked = any.maxSatisfying(List.of(v("1.0.0+first"), v("1.0.0+second"), v("0.9.0")));
        assertEquals("first", picked.build());
    }

    public void testCaretOnZeroMajor() {
        assertTrue(in("^0.2.3", "0.2.9"));
        assertFalse(in("^0.2.3", "0.3.0"));
        assertTrue(in("^0.0.3", "0.0.3"));
        assertFalse(in("^0.0.3", "0.0.4"));
        assertTrue(in("^1.0.0", "1.99.99"));
    }

    public void testComparisonOperators() {
        assertTrue(in(">1.0.0", "1.0.1"));
        assertFalse(in(">1.0.0", "1.0.0"));
        assertTrue(in("<=1.0.0", "1.0.0"));
        assertFalse(in("<1.0.0", "1.0.0"));
        assertTrue(in("1.2.3", "1.2.3"));
        assertFalse(in("=1.2.3", "1.2.4"));
    }

    public void testPrereleaseNeedsSameCoreComparator() {
        assertFalse(in("^1.2.3", "1.3.0-beta"));
        assertFalse(in(">=1.0.0", "2.0.0-rc.1"));
        assertFalse(in("*", "1.0.0-alpha"));
        assertTrue(in("^1.2.3-beta.2", "1.2.3-beta.4"));
        assertFalse(in("^1.2.3-beta.2", "1.2.4-beta.1"));
        assertTrue(in("^1.2.3-beta.2", "1.2.4"));
        // 다른 묶음의 비교식은 들이지 않는다
        assertFalse(in(">=1.2.3-alpha <1.2.3 || >=2.0.0", "2.0.0-rc.1"));
        assertTrue(in(">=1.2.3-alpha <1.2.3 || >=2.0.0", "1.2.3-beta"));
    }

    public void testWhitespaceAndStar() {
        assertTrue(in("  >=1.0.0    <2.0.0  ", "1.5.0"));
        assertTrue(in("*", "0.0.0"));
        assertTrue(in("1.0.0||2.0.0", "2.0.0"));
        assertThrows(IllegalArgumentException.class, () -> Range.parse("1.0.0 ||"));
        assertThrows(IllegalArgumentException.class, () -> Range.parse(">= 1.0.0"));
        assertThrows(IllegalArgumentException.class, () -> Range.parse("^1.2"));
        assertThrows(IllegalArgumentException.class, () -> Range.parse(""));
    }

    public void testMaxSatisfyingSkipsPrereleaseAndUnsorted() {
        Range range = Range.parse("^1.0.0");
        Version best = range.maxSatisfying(List.of(v("1.2.0"), v("1.10.0"), v("2.0.0-alpha"), v("1.9.9"), v("1.11.0-rc.1")));
        assertEquals(v("1.10.0"), best);
        assertNull(range.maxSatisfying(List.of()));
    }
}
