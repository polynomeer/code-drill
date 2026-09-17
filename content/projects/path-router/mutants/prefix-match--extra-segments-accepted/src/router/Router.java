package router;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

// kind: OFF_BY_ONE
// 패턴이 경로의 앞부분이기만 하면 맞는다고 한다 — /users/{id} 가 /users/1/posts 와 맞는다.
public class Router {

    private record Route(String pattern, List<String> segments, String handler) {}

    private final List<Route> routes = new ArrayList<>();

    /** 질의 문자열을 떼고 끝의 슬래시 하나를 뗀다. 패턴과 경로에 같은 규칙이다. */
    private static String normalize(String path) {
        if (path == null || !path.startsWith("/")) throw new IllegalArgumentException("path must start with '/': " + path);
        int query = path.indexOf('?');
        if (query >= 0) path = path.substring(0, query);
        if (path.length() > 1 && path.endsWith("/")) path = path.substring(0, path.length() - 1);
        return path;
    }

    private static List<String> split(String normalized) {
        return normalized.equals("/") ? List.of() : List.of(normalized.substring(1).split("/", -1));
    }

    private static boolean isParam(String segment) {
        return segment.startsWith("{");
    }

    private static String paramName(String segment) {
        return segment.substring(1, segment.length() - 1);
    }

    /** 패턴을 검사하며 조각으로 나눈다. */
    private static List<String> parse(String pattern) {
        List<String> segments = split(normalize(pattern));
        Set<String> names = new HashSet<>();
        for (String segment : segments) {
            if (segment.isEmpty()) throw new IllegalArgumentException("empty segment in " + pattern);
            if (isParam(segment)) {
                if (!segment.endsWith("}") || segment.length() < 3) throw new IllegalArgumentException("bad parameter segment: " + segment);
                if (!names.add(paramName(segment))) throw new IllegalArgumentException("duplicate parameter: " + segment);
            }
        }
        return segments;
    }

    /** 매개변수 이름을 지운 모양. 같은 모양은 같은 라우트다. */
    private static String shape(List<String> segments) {
        StringBuilder shape = new StringBuilder();
        for (String segment : segments) shape.append('/').append(isParam(segment) ? "{}" : segment);
        return shape.toString();
    }

    public void add(String pattern, String handler) {
        if (handler == null || handler.isEmpty()) throw new IllegalArgumentException("handler must not be empty");
        List<String> segments = parse(pattern);
        String shape = shape(segments);
        for (Route route : routes) {
            if (shape(route.segments()).equals(shape)) throw new DuplicateRouteException(route.pattern());
        }
        routes.add(new Route(normalize(pattern), segments, handler));
    }

    public Match match(String path) {
        List<String> parts = split(normalize(path));
        Route best = null;
        Map<String, String> bestParams = null;
        for (Route route : routes) {
            Map<String, String> params = bind(route, parts);
            if (params == null) continue;
            if (best == null || moreSpecific(route, best)) {
                best = route;
                bestParams = params;
            }
        }
        return best == null ? null : new Match(best.handler(), Map.copyOf(bestParams));
    }

    /** 맞으면 매개변수 맵, 아니면 null. 조각 수가 같아야 하고 매개변수에는 빈 조각이 오지 않는다. */
    private static Map<String, String> bind(Route route, List<String> parts) {
        if (route.segments().size() > parts.size()) return null;
        Map<String, String> params = new LinkedHashMap<>();
        for (int i = 0; i < route.segments().size(); i++) {
            String segment = route.segments().get(i);
            String part = parts.get(i);
            if (isParam(segment)) {
                if (part.isEmpty()) return null;
                params.put(paramName(segment), part);
            } else if (!segment.equals(part)) {
                return null;
            }
        }
        return params;
    }

    /** 왼쪽부터 처음으로 종류가 다른 조각에서 글자 조각인 쪽이 더 구체적이다. */
    private static boolean moreSpecific(Route candidate, Route current) {
        for (int i = 0; i < candidate.segments().size(); i++) {
            boolean candidateParam = isParam(candidate.segments().get(i));
            boolean currentParam = isParam(current.segments().get(i));
            if (candidateParam != currentParam) return currentParam;
        }
        return false;
    }

    public boolean remove(String pattern) {
        String normalized = normalize(pattern);
        return routes.removeIf(route -> route.pattern().equals(normalized));
    }

    public List<String> routes() {
        List<String> patterns = new ArrayList<>();
        for (Route route : routes) patterns.add(route.pattern());
        return patterns;
    }
}
