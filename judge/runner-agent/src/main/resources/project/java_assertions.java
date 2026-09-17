package codedrill;

import java.util.Objects;

/** 테스트가 쓰는 단언. JUnit 은 샌드박스에 없다 — 표준 라이브러리만 있다. `import static codedrill.Assertions.*;` */
public final class Assertions {
    private Assertions() {}

    public static void assertEquals(Object expected, Object actual) {
        assertEquals(expected, actual, null);
    }

    public static void assertEquals(Object expected, Object actual, String message) {
        if (!Objects.equals(expected, actual)) {
            throw new AssertionError((message == null ? "" : message + ": ") + "expected <" + expected + "> but was <" + actual + ">");
        }
    }

    public static void assertTrue(boolean condition) { assertTrue(condition, "expected true"); }

    public static void assertTrue(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }

    public static void assertFalse(boolean condition) { assertTrue(!condition, "expected false"); }

    public static void assertNull(Object actual) {
        if (actual != null) throw new AssertionError("expected null but was <" + actual + ">");
    }

    public interface Block { void run() throws Exception; }

    public static <T extends Throwable> T assertThrows(Class<T> type, Block block) {
        try {
            block.run();
        } catch (Throwable e) {
            if (type.isInstance(e)) return type.cast(e);
            throw new AssertionError("expected " + type.getSimpleName() + " but got " + e.getClass().getSimpleName() + ": " + e.getMessage());
        }
        throw new AssertionError("expected " + type.getSimpleName() + " but nothing was thrown");
    }
}
