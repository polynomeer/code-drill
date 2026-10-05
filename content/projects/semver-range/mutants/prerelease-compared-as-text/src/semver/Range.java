package semver;

import java.util.ArrayList;
import java.util.List;

/**
 * 버전 범위 — `||` 로 나뉜 묶음 중 하나라도 맞으면 맞는다. 한 묶음은 공백으로 나뉜 비교식이 모두 맞아야 맞는다.
 */
public final class Range {

    private record Comparator(String op, Version version) {
        boolean test(Version v) {
            int c = v.compareTo(version);
            return switch (op) {
                case ">=" -> c >= 0;
                case "<=" -> c <= 0;
                case ">" -> c > 0;
                case "<" -> c < 0;
                default -> c == 0;
            };
        }
    }

    private final List<List<Comparator>> sets;

    private Range(List<List<Comparator>> sets) {
        this.sets = sets;
    }

    public static Range parse(String text) {
        if (text == null) throw new IllegalArgumentException("range is null");
        List<List<Comparator>> sets = new ArrayList<>();
        for (String part : text.split("\\|\\|", -1)) {
            String set = part.trim();
            if (set.isEmpty()) throw new IllegalArgumentException("empty set: " + text);
            List<Comparator> comparators = new ArrayList<>();
            for (String token : set.split("\\s+")) {
                if (token.equals("*")) continue;
                expand(token, comparators);
            }
            sets.add(List.copyOf(comparators));
        }
        return new Range(List.copyOf(sets));
    }

    private static void expand(String token, List<Comparator> out) {
        String op;
        if (token.startsWith(">=") || token.startsWith("<=")) op = token.substring(0, 2);
        else if (token.startsWith(">") || token.startsWith("<") || token.startsWith("=")
            || token.startsWith("^") || token.startsWith("~")) op = token.substring(0, 1);
        else op = "";
        Version v = Version.parse(token.substring(op.length()));
        switch (op) {
            case "^" -> {
                out.add(new Comparator(">=", v));
                if (v.major() > 0) out.add(new Comparator("<", Version.parse((v.major() + 1) + ".0.0")));
                else if (v.minor() > 0) out.add(new Comparator("<", Version.parse("0." + (v.minor() + 1) + ".0")));
                else out.add(new Comparator("<", Version.parse("0.0." + (v.patch() + 1))));
            }
            case "~" -> {
                out.add(new Comparator(">=", v));
                out.add(new Comparator("<", Version.parse(v.major() + "." + (v.minor() + 1) + ".0")));
            }
            case "", "=" -> out.add(new Comparator("=", v));
            default -> out.add(new Comparator(op, v));
        }
    }

    public boolean satisfies(Version version) {
        for (List<Comparator> set : sets) {
            if (matches(set, version)) return true;
        }
        return false;
    }

    private static boolean matches(List<Comparator> set, Version version) {
        for (Comparator c : set) {
            if (!c.test(version)) return false;
        }
        if (!version.isPrerelease()) return true;
        // 프리릴리스는 같은 세 수의 프리릴리스를 적은 비교식이 그 묶음에 있을 때만 들어온다
        for (Comparator c : set) {
            if (c.version().isPrerelease() && c.version().sameCore(version)) return true;
        }
        return false;
    }

    public Version maxSatisfying(List<Version> versions) {
        Version best = null;
        for (Version v : versions) {
            if (satisfies(v) && (best == null || v.compareTo(best) > 0)) best = v;
        }
        return best;
    }
}
