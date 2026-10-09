package slug;

import java.text.Normalizer;
import java.util.Set;

/**
 * 제목에서 URL 슬러그를 만든다. 악센트를 떼고, ASCII 영숫자만 남기고, 나머지 덩어리는 대시 하나로 바꾼다.
 */
// kind: MISSING_EDGE_CASE
// 악센트를 떼지 않는다. é 가 영숫자가 아니라 구분자가 되어 café 가 caf 가 된다.
public final class Slugs {

    private Slugs() { }

    public static String slugify(String title, int maxLength) {
        if (maxLength < 1) throw new IllegalArgumentException("maxLength must be at least 1");
        // NFD 로 나누면 é 가 e + 결합 부호가 된다. 부호만 버리면 글자가 남는다.
        String plain = title;
        StringBuilder out = new StringBuilder();
        boolean gap = false;
        for (int i = 0; i < plain.length(); i++) {
            char c = plain.charAt(i);
            if (Character.getType(c) == Character.NON_SPACING_MARK) continue;
            // 아포스트로피는 낱말을 가르지 않는다: don't → dont
            if (c == '\'' || c == '\u2019') continue;
            if (c >= 'A' && c <= 'Z') c = (char) (c + ('a' - 'A'));
            if ((c >= 'a' && c <= 'z') || (c >= '0' && c <= '9')) {
                // 앞에 글자가 있을 때만 대시를 놓는다 — 앞뒤의 대시는 생기지 않는다
                if (gap && out.length() > 0) out.append('-');
                gap = false;
                out.append(c);
            } else {
                gap = true;
            }
        }
        String slug = out.toString();
        if (slug.length() > maxLength) {
            boolean insideWord = slug.charAt(maxLength) != '-';
            slug = slug.substring(0, maxLength);
            if (insideWord) {
                int dash = slug.lastIndexOf('-');
                if (dash > 0) slug = slug.substring(0, dash);
            }
        }
        return slug.isEmpty() ? "untitled" : slug;
    }

    public static String unique(String slug, Set<String> taken) {
        if (!taken.contains(slug)) return slug;
        for (int n = 2; ; n++) {
            String candidate = slug + "-" + n;
            if (!taken.contains(candidate)) return candidate;
        }
    }
}
