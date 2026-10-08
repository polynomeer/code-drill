package tests;

import java.util.List;
import book.Level;
import book.Order;
import book.OrderBook;
import book.OrderType;
import book.Side;
import book.Trade;
import static codedrill.Assertions.*;

/** 공개 테스트. 채점은 이것과 숨은 테스트를 함께 돌린다. */
public class PublicOrderBookTest {

    private static Order limit(long id, Side side, long price, long qty) {
        return new Order(id, side, OrderType.LIMIT, price, qty);
    }

    public void testFullMatchAtSamePrice() {
        OrderBook book = new OrderBook();
        assertEquals(List.of(), book.submit(limit(1, Side.SELL, 100, 5)));
        assertEquals(List.of(new Trade(2, 1, 100, 5)), book.submit(limit(2, Side.BUY, 100, 5)));
        assertEquals(List.of(), book.depth(Side.SELL, 10));
    }

    public void testRestingOrdersShowInDepth() {
        OrderBook book = new OrderBook();
        book.submit(limit(1, Side.BUY, 99, 3));
        book.submit(limit(2, Side.BUY, 101, 2));
        book.submit(limit(3, Side.SELL, 105, 4));
        assertEquals(List.of(new Level(101, 2), new Level(99, 3)), book.depth(Side.BUY, 10));
        assertEquals(List.of(new Level(105, 4)), book.depth(Side.SELL, 10));
    }

    public void testCancel() {
        OrderBook book = new OrderBook();
        book.submit(limit(1, Side.SELL, 100, 5));
        assertTrue(book.cancel(1));
        assertFalse(book.cancel(1));
        assertEquals(List.of(), book.depth(Side.SELL, 10));
    }

    public void testRejectsBadOrders() {
        OrderBook book = new OrderBook();
        assertThrows(IllegalArgumentException.class, () -> book.submit(limit(1, Side.BUY, 100, 0)));
        assertThrows(IllegalArgumentException.class, () -> book.submit(limit(2, Side.BUY, 0, 5)));
    }
}
