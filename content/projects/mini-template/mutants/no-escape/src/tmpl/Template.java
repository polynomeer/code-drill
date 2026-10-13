package tmpl;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.Map;

/**
 * 작은 머스태시. 템플릿을 한 번 읽어 나무(글자·변수·섹션)로 만들고, 문맥 스택을 들고 나무를 그린다.
 */
// kind: MISSING_EDGE_CASE
// {{name}} 도 이스케이프하지 않는다. 두 겹 중괄호는 HTML 을 이스케이프한다.
public final class Template {

    private Template() { }

    private sealed interface Node permits Text, Variable, Section { }
    private record Text(String text) implements Node { }
    private record Variable(String name, boolean escape) implements Node { }
    private record Section(String name, boolean inverted, List<Node> body) implements Node { }

    public static String render(String template, Map<String, ?> data) {
        List<Node> nodes = parse(template);
        Deque<Object> contexts = new ArrayDeque<>();
        contexts.push(data);
        StringBuilder out = new StringBuilder();
        draw(nodes, contexts, out);
        return out.toString();
    }

    // --- 읽기 ---------------------------------------------------------------

    private static List<Node> parse(String template) {
        // 열린 섹션마다 (이름, 뒤집힘, 몸통) — 닫는 태그는 가장 안쪽의 열린 섹션과 이름이 같아야 한다
        Deque<Section> open = new ArrayDeque<>();
        Deque<List<Node>> bodies = new ArrayDeque<>();
        List<Node> root = new ArrayList<>();
        bodies.push(root);
        int i = 0;
        while (i < template.length()) {
            int start = template.indexOf("{{", i);
            if (start < 0) {
                bodies.peek().add(new Text(template.substring(i)));
                break;
            }
            if (start > i) bodies.peek().add(new Text(template.substring(i, start)));
            boolean triple = template.startsWith("{{{", start);
            String closer = triple ? "}}}" : "}}";
            int end = template.indexOf(closer, start + (triple ? 3 : 2));
            if (end < 0) throw new IllegalArgumentException("unclosed tag at " + start);
            String inside = template.substring(start + (triple ? 3 : 2), end).trim();
            i = end + closer.length();
            if (triple) {
                bodies.peek().add(new Variable(name(inside), false));
            } else if (inside.startsWith("#") || inside.startsWith("^")) {
                Section section = new Section(name(inside.substring(1).trim()), inside.startsWith("^"), new ArrayList<>());
                bodies.peek().add(section);
                open.push(section);
                bodies.push(section.body());
            } else if (inside.startsWith("/")) {
                String closing = name(inside.substring(1).trim());
                if (open.isEmpty() || !open.peek().name().equals(closing)) {
                    throw new IllegalArgumentException("unexpected {{/" + closing + "}}");
                }
                open.pop();
                bodies.pop();
            } else {
                bodies.peek().add(new Variable(name(inside), true));
            }
        }
        if (!open.isEmpty()) throw new IllegalArgumentException("unclosed section " + open.peek().name());
        return root;
    }

    private static String name(String raw) {
        if (raw.isEmpty()) throw new IllegalArgumentException("empty tag");
        return raw;
    }

    // --- 그리기 -------------------------------------------------------------

    private static void draw(List<Node> nodes, Deque<Object> contexts, StringBuilder out) {
        for (Node node : nodes) {
            if (node instanceof Text text) {
                out.append(text.text());
            } else if (node instanceof Variable variable) {
                Object value = lookup(variable.name(), contexts);
                if (value == null) continue;
                String shown = String.valueOf(value);
                out.append(shown);
            } else if (node instanceof Section section) {
                Object value = lookup(section.name(), contexts);
                if (section.inverted()) {
                    if (falsy(value)) draw(section.body(), contexts, out);
                } else if (value instanceof List<?> list) {
                    for (Object item : list) {
                        contexts.push(item);
                        draw(section.body(), contexts, out);
                        contexts.pop();
                    }
                } else if (!falsy(value)) {
                    contexts.push(value);
                    draw(section.body(), contexts, out);
                    contexts.pop();
                }
            }
        }
    }

    private static boolean falsy(Object value) {
        return value == null || Boolean.FALSE.equals(value) || (value instanceof List<?> list && list.isEmpty());
    }

    /** 첫 조각은 안쪽 문맥부터 바깥으로 찾고, 나머지 조각은 찾은 값 안에서만 따라간다. */
    private static Object lookup(String name, Deque<Object> contexts) {
        if (name.equals(".")) return contexts.peek();
        String[] parts = name.split("\\.", -1);
        Object found = null;
        boolean hit = false;
        for (Object context : contexts) {
            if (context instanceof Map<?, ?> map && map.containsKey(parts[0])) {
                found = map.get(parts[0]);
                hit = true;
                break;
            }
        }
        if (!hit) return null;
        for (int k = 1; k < parts.length; k++) {
            if (!(found instanceof Map<?, ?> map)) return null;
            found = map.get(parts[k]);
        }
        return found;
    }

    private static String escape(String text) {
        StringBuilder out = new StringBuilder(text.length());
        for (int k = 0; k < text.length(); k++) {
            char c = text.charAt(k);
            switch (c) {
                case '&' -> out.append("&amp;");
                case '<' -> out.append("&lt;");
                case '>' -> out.append("&gt;");
                case '"' -> out.append("&quot;");
                case '\'' -> out.append("&#39;");
                default -> out.append(c);
            }
        }
        return out.toString();
    }
}
