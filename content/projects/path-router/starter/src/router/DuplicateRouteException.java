package router;

/** 같은 모양의 패턴이 이미 있다. */
public class DuplicateRouteException extends RuntimeException {
    public DuplicateRouteException(String pattern) {
        super("route already exists: " + pattern);
    }
}
