package tests;

import java.util.List;
import java.util.Map;
import router.Match;
import router.Router;
import static codedrill.Assertions.*;

/** 공개 테스트. 채점은 이것과 숨은 테스트를 함께 돌린다. */
public class PublicRouterTest {

    public void testMatchCapturesParams() {
        Router router = new Router();
        router.add("/users/{id}/posts/{postId}", "post");
        Match match = router.match("/users/42/posts/7");
        assertEquals("post", match.handler());
        assertEquals(Map.of("id", "42", "postId", "7"), match.params());
    }

    public void testNoMatchIsNull() {
        Router router = new Router();
        router.add("/users/{id}", "user");
        assertNull(router.match("/posts/1"));
        assertNull(router.match("/users"));
    }

    public void testRemoveAndRoutes() {
        Router router = new Router();
        router.add("/a", "a");
        router.add("/b", "b");
        assertEquals(List.of("/a", "/b"), router.routes());
        assertTrue(router.remove("/a"));
        assertFalse(router.remove("/a"));
        assertNull(router.match("/a"));
    }
}
