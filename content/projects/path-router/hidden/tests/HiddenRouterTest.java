package tests;

import java.util.List;
import java.util.Map;
import router.DuplicateRouteException;
import router.Match;
import router.Router;
import static codedrill.Assertions.*;

/** 숨은 테스트. 사용자에게 나가지 않는다. */
public class HiddenRouterTest {

    public void testLiteralBeatsParamRegardlessOfOrder() {
        Router router = new Router();
        router.add("/users/{id}", "user");
        router.add("/users/me", "me");
        assertEquals("me", router.match("/users/me").handler());
        assertEquals("user", router.match("/users/7").handler());

        Router reversed = new Router();
        reversed.add("/users/me", "me");
        reversed.add("/users/{id}", "user");
        assertEquals("me", reversed.match("/users/me").handler());
    }

    public void testFirstDifferingSegmentDecides() {
        Router router = new Router();
        router.add("/{a}/x", "param-then-literal");
        router.add("/x/{b}", "literal-then-param");
        assertEquals("literal-then-param", router.match("/x/x").handler());
    }

    public void testTrailingSlashIsIgnored() {
        Router router = new Router();
        router.add("/users/", "users");
        assertEquals(List.of("/users"), router.routes());
        assertEquals("users", router.match("/users").handler());
        assertEquals("users", router.match("/users/").handler());
        assertTrue(router.remove("/users/"));
    }

    public void testQueryStringIsIgnored() {
        Router router = new Router();
        router.add("/search/{term}", "search");
        Match match = router.match("/search/kotlin?page=2&sort=asc");
        assertEquals("search", match.handler());
        assertEquals(Map.of("term", "kotlin"), match.params());
    }

    public void testSegmentCountMustMatch() {
        Router router = new Router();
        router.add("/users/{id}", "user");
        assertNull(router.match("/users/1/posts"));
        assertNull(router.match("/users//"));
        assertNull(router.match("/"));
    }

    public void testRootAndNoParams() {
        Router router = new Router();
        router.add("/", "root");
        router.add("/about", "about");
        assertEquals(Map.of(), router.match("/").params());
        assertEquals("root", router.match("/?x=1").handler());
        assertEquals("about", router.match("/about/").handler());
    }

    public void testDuplicateShapeIsRejected() {
        Router router = new Router();
        router.add("/users/{id}", "user");
        assertThrows(DuplicateRouteException.class, () -> router.add("/users/{name}", "other"));
        assertThrows(DuplicateRouteException.class, () -> router.add("/users/{id}/", "other"));
        router.add("/users/{id}/posts", "posts");
        assertEquals(List.of("/users/{id}", "/users/{id}/posts"), router.routes());
    }

    public void testInvalidPatternsAndHandlers() {
        Router router = new Router();
        assertThrows(IllegalArgumentException.class, () -> router.add("users", "h"));
        assertThrows(IllegalArgumentException.class, () -> router.add("/a//b", "h"));
        assertThrows(IllegalArgumentException.class, () -> router.add("/a/{", "h"));
        assertThrows(IllegalArgumentException.class, () -> router.add("/a/{}", "h"));
        assertThrows(IllegalArgumentException.class, () -> router.add("/{x}/{x}", "h"));
        assertThrows(IllegalArgumentException.class, () -> router.add("/a", ""));
        assertThrows(IllegalArgumentException.class, () -> router.add("/a", null));
        assertThrows(IllegalArgumentException.class, () -> router.match("a/b"));
        assertEquals(List.of(), router.routes());
    }

    public void testParamsAreCapturedPerRoute() {
        Router router = new Router();
        router.add("/{kind}/{id}", "generic");
        router.add("/users/{id}", "user");
        assertEquals(Map.of("id", "5"), router.match("/users/5").params());
        assertEquals(Map.of("kind", "teams", "id", "5"), router.match("/teams/5").params());
    }
}
