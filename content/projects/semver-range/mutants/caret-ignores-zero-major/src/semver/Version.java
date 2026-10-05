package semver;

import java.util.ArrayList;
import java.util.List;

/**
 * 유의적 버전 하나 — MAJOR.MINOR.PATCH, 그 뒤에 선택적으로 `-프리릴리스` 와 `+빌드`.
 * 순서는 빌드를 보지 않는다. 같은 순서의 두 버전은 같은 버전이다.
 */
public final class Version implements Comparable<Version> {

    private final int major;
    private final int minor;
    private final int patch;
    private final List<String> prerelease;
    private final String build;

    private Version(int major, int minor, int patch, List<String> prerelease, String build) {
        this.major = major;
        this.minor = minor;
        this.patch = patch;
        this.prerelease = prerelease;
        this.build = build;
    }

    public static Version parse(String text) {
        if (text == null) throw new IllegalArgumentException("version is null");
        String rest = text;
        String build = "";
        int plus = rest.indexOf('+');
        if (plus >= 0) {
            build = rest.substring(plus + 1);
            identifiers(build, false);
            rest = rest.substring(0, plus);
        }
        List<String> prerelease = List.of();
        int dash = rest.indexOf('-');
        if (dash >= 0) {
            prerelease = identifiers(rest.substring(dash + 1), true);
            rest = rest.substring(0, dash);
        }
        String[] core = rest.split("\\.", -1);
        if (core.length != 3) throw new IllegalArgumentException("expected MAJOR.MINOR.PATCH: " + text);
        return new Version(number(core[0], text), number(core[1], text), number(core[2], text), prerelease, build);
    }

    private static boolean digits(String s) {
        if (s.isEmpty()) return false;
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (c < '0' || c > '9') return false;
        }
        return true;
    }

    private static int number(String s, String text) {
        if (!digits(s)) throw new IllegalArgumentException("not a number: " + text);
        if (s.length() > 1 && s.charAt(0) == '0') throw new IllegalArgumentException("leading zero: " + text);
        try {
            return Integer.parseInt(s);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("too large: " + text);
        }
    }

    private static List<String> identifiers(String part, boolean prerelease) {
        List<String> out = new ArrayList<>();
        for (String id : part.split("\\.", -1)) {
            if (id.isEmpty()) throw new IllegalArgumentException("empty identifier: " + part);
            for (int i = 0; i < id.length(); i++) {
                char c = id.charAt(i);
                boolean ok = (c >= '0' && c <= '9') || (c >= 'a' && c <= 'z') || (c >= 'A' && c <= 'Z') || c == '-';
                if (!ok) throw new IllegalArgumentException("bad identifier: " + part);
            }
            if (prerelease && digits(id) && id.length() > 1 && id.charAt(0) == '0') {
                throw new IllegalArgumentException("leading zero: " + part);
            }
            out.add(id);
        }
        return List.copyOf(out);
    }

    public int major() { return major; }

    public int minor() { return minor; }

    public int patch() { return patch; }

    public List<String> prerelease() { return prerelease; }

    public String build() { return build; }

    public boolean isPrerelease() { return !prerelease.isEmpty(); }

    /** 프리릴리스와 빌드를 뗀 세 수가 같은가. */
    public boolean sameCore(Version other) {
        return major == other.major && minor == other.minor && patch == other.patch;
    }

    @Override
    public int compareTo(Version other) {
        if (major != other.major) return Integer.compare(major, other.major);
        if (minor != other.minor) return Integer.compare(minor, other.minor);
        if (patch != other.patch) return Integer.compare(patch, other.patch);
        // 프리릴리스가 없는 쪽이 크다
        if (prerelease.isEmpty() || other.prerelease.isEmpty()) {
            return Boolean.compare(prerelease.isEmpty(), other.prerelease.isEmpty());
        }
        int shared = Math.min(prerelease.size(), other.prerelease.size());
        for (int i = 0; i < shared; i++) {
            int c = compareIdentifier(prerelease.get(i), other.prerelease.get(i));
            if (c != 0) return c;
        }
        return Integer.compare(prerelease.size(), other.prerelease.size());
    }

    private static int compareIdentifier(String a, String b) {
        boolean na = digits(a);
        boolean nb = digits(b);
        if (na && nb) {
            // 앞자리 0 이 없으므로 길이가 곧 자릿수다 — Int 를 넘는 수도 비교된다
            if (a.length() != b.length()) return Integer.compare(a.length(), b.length());
            return a.compareTo(b);
        }
        if (na) return -1;
        if (nb) return 1;
        return a.compareTo(b);
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof Version v && compareTo(v) == 0;
    }

    @Override
    public int hashCode() {
        return (major * 31 + minor) * 31 + patch + prerelease.hashCode() * 17;
    }

    @Override
    public String toString() {
        String core = major + "." + minor + "." + patch;
        if (!prerelease.isEmpty()) core += "-" + String.join(".", prerelease);
        if (!build.isEmpty()) core += "+" + build;
        return core;
    }
}
