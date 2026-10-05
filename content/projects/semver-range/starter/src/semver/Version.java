package semver;

import java.util.List;

/**
 * 유의적 버전 — 골격. 시그니처는 그대로 두고 본문을 채운다. 필드와 생성자는 마음대로 둔다.
 */
public final class Version implements Comparable<Version> {

    public static Version parse(String text) {
        throw new UnsupportedOperationException("TODO");
    }

    public int major() { throw new UnsupportedOperationException("TODO"); }

    public int minor() { throw new UnsupportedOperationException("TODO"); }

    public int patch() { throw new UnsupportedOperationException("TODO"); }

    public List<String> prerelease() { throw new UnsupportedOperationException("TODO"); }

    public String build() { throw new UnsupportedOperationException("TODO"); }

    public boolean isPrerelease() { throw new UnsupportedOperationException("TODO"); }

    @Override
    public int compareTo(Version other) {
        throw new UnsupportedOperationException("TODO");
    }
}
