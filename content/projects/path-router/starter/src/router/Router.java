package router;

import java.util.List;

/**
 * 경로 라우터 — 골격. 시그니처는 그대로 두고 본문을 채운다.
 */
public class Router {

    public void add(String pattern, String handler) {
        throw new UnsupportedOperationException("TODO");
    }

    public Match match(String path) {
        throw new UnsupportedOperationException("TODO");
    }

    public boolean remove(String pattern) {
        throw new UnsupportedOperationException("TODO");
    }

    public List<String> routes() {
        throw new UnsupportedOperationException("TODO");
    }
}
