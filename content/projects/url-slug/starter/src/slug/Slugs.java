package slug;

import java.util.Set;

/**
 * 슬러그 — 골격. 시그니처는 그대로 두고 본문을 채운다. 도우미는 마음대로 더한다.
 */
public final class Slugs {

    private Slugs() { }

    public static String slugify(String title, int maxLength) {
        throw new UnsupportedOperationException("TODO");
    }

    public static String unique(String slug, Set<String> taken) {
        throw new UnsupportedOperationException("TODO");
    }
}
