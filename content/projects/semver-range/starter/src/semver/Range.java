package semver;

import java.util.List;

/**
 * 버전 범위 — 골격. 시그니처는 그대로 두고 본문을 채운다.
 */
public final class Range {

    public static Range parse(String text) {
        throw new UnsupportedOperationException("TODO");
    }

    public boolean satisfies(Version version) {
        throw new UnsupportedOperationException("TODO");
    }

    public Version maxSatisfying(List<Version> versions) {
        throw new UnsupportedOperationException("TODO");
    }
}
