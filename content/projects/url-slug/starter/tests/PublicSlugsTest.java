package tests;

import java.util.Set;
import slug.Slugs;
import static codedrill.Assertions.*;

/** 공개 테스트. 채점은 이것과 숨은 테스트를 함께 돌린다. */
public class PublicSlugsTest {

    public void testWordsBecomeLowercaseWithDashes() {
        assertEquals("hello-world", Slugs.slugify("Hello World", 80));
    }

    public void testPunctuationRunIsOneDash() {
        assertEquals("ready-set-go", Slugs.slugify("Ready, set... GO", 80));
    }

    public void testUniqueKeepsAFreeSlug() {
        assertEquals("news", Slugs.unique("news", Set.of("blog")));
    }

    public void testUniqueAddsANumber() {
        assertEquals("news-2", Slugs.unique("news", Set.of("news")));
    }
}
