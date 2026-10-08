package tests;

import java.util.List;
import book.Level;
import book.Order;
import book.OrderBook;
import book.OrderType;
import book.Side;
import book.Trade;
import static codedrill.Assertions.*;

/** 숨은 테스트. 사용자에게 나가지 않는다. */
public class HiddenOrderBookTest {

    private static Order limit(long id, Side side, long price, long qty) {
        return new Order(id, side, OrderType.LIMIT, price, qty);
    }

    private static Order market(long id, Side side, long qty) {
        return new Order(id, side, OrderType.MARKET, 0, qty);
    }

    public void testTradesAtRestingPrice() {
        OrderBook book = new OrderBook();
        book.submit(limit(1, Side.SELL, 100, 5));
        assertEquals(List.of(new Trade(2, 1, 100, 5)), book.submit(limit(2, Side.BUY, 110, 5)));
        book.submit(limit(3, Side.BUY, 90, 5));
        assertEquals(List.of(new Trade(3, 4, 90, 5)), book.submit(limit(4, Side.SELL, 80, 5)));
    }

    public void testEqualPriceCrosses() {
        OrderBook book = new OrderBook();
        book.submit(limit(1, Side.BUY, 100, 2));
        assertEquals(List.of(new Trade(1, 2, 100, 2)), book.submit(limit(2, Side.SELL, 100, 2)));
    }

    public void testNoCrossRests() {
        OrderBook book = new OrderBook();
        book.submit(limit(1, Side.SELL, 101, 2));
        assertEquals(List.of(), book.submit(limit(2, Side.BUY, 100, 2)));
        assertEquals(List.of(new Level(100, 2)), book.depth(Side.BUY, 5));
        assertEquals(List.of(new Level(101, 2)), book.depth(Side.SELL, 5));
    }

    public void testPriceThenTimePriority() {
        OrderBook book = new OrderBook();
        book.submit(limit(1, Side.SELL, 101, 3));
        book.submit(limit(2, Side.SELL, 100, 3));
        book.submit(limit(3, Side.SELL, 100, 3));
        List<Trade> trades = book.submit(limit(4, Side.BUY, 101, 7));
        assertEquals(List.of(new Trade(4, 2, 100, 3), new Trade(4, 3, 100, 3), new Trade(4, 1, 101, 1)), trades);
        assertEquals(List.of(new Level(101, 2)), book.depth(Side.SELL, 5));
    }

    public void testPartialFillThenRest() {
        OrderBook book = new OrderBook();
        book.submit(limit(1, Side.SELL, 100, 3));
        assertEquals(List.of(new Trade(2, 1, 100, 3)), book.submit(limit(2, Side.BUY, 100, 10)));
        assertEquals(List.of(new Level(100, 7)), book.depth(Side.BUY, 5));
        assertEquals(List.of(), book.depth(Side.SELL, 5));
        // 남은 7 은 그 뒤의 파는 주문과 만난다
        assertEquals(List.of(new Trade(2, 3, 100, 4)), book.submit(limit(3, Side.SELL, 99, 4)));
        assertEquals(List.of(new Level(100, 3)), book.depth(Side.BUY, 5));
    }

    public void testRestingOrderPartiallyConsumed() {
        OrderBook book = new OrderBook();
        book.submit(limit(1, Side.BUY, 100, 10));
        book.submit(limit(2, Side.SELL, 100, 4));
        assertEquals(List.of(new Level(100, 6)), book.depth(Side.BUY, 5));
        assertTrue(book.cancel(1));
        assertEquals(List.of(), book.depth(Side.BUY, 5));
    }

    public void testMarketOrderSweepsAndDropsRemainder() {
        OrderBook book = new OrderBook();
        book.submit(limit(1, Side.SELL, 100, 2));
        book.submit(limit(2, Side.SELL, 120, 2));
        List<Trade> trades = book.submit(market(3, Side.BUY, 10));
        assertEquals(List.of(new Trade(3, 1, 100, 2), new Trade(3, 2, 120, 2)), trades);
        assertEquals(List.of(), book.depth(Side.SELL, 5));
        assertEquals(List.of(), book.depth(Side.BUY, 5));
    }

    public void testMarketOrderOnEmptyBook() {
        OrderBook book = new OrderBook();
        assertEquals(List.of(), book.submit(market(1, Side.SELL, 5)));
        assertEquals(List.of(), book.depth(Side.SELL, 5));
        assertFalse(book.cancel(1));
    }

    public void testDepthDropsEmptyLevelsAndLimits() {
        OrderBook book = new OrderBook();
        book.submit(limit(1, Side.SELL, 100, 1));
        book.submit(limit(2, Side.SELL, 101, 1));
        book.submit(limit(3, Side.SELL, 102, 1));
        book.submit(limit(4, Side.SELL, 102, 5));
        book.submit(limit(5, Side.BUY, 100, 1));
        assertEquals(List.of(new Level(101, 1), new Level(102, 6)), book.depth(Side.SELL, 5));
        assertEquals(List.of(new Level(101, 1)), book.depth(Side.SELL, 1));
        assertEquals(List.of(), book.depth(Side.SELL, 0));
    }

    public void testCancelKeepsQueueOrder() {
        OrderBook book = new OrderBook();
        book.submit(limit(1, Side.BUY, 100, 1));
        book.submit(limit(2, Side.BUY, 100, 1));
        book.submit(limit(3, Side.BUY, 100, 1));
        assertTrue(book.cancel(2));
        List<Trade> trades = book.submit(limit(4, Side.SELL, 100, 2));
        assertEquals(List.of(new Trade(1, 4, 100, 1), new Trade(3, 4, 100, 1)), trades);
    }

    public void testCancelAfterFillIsFalse() {
        OrderBook book = new OrderBook();
        book.submit(limit(1, Side.SELL, 100, 1));
        book.submit(limit(2, Side.BUY, 100, 1));
        assertFalse(book.cancel(1));
        assertFalse(book.cancel(2));
        assertFalse(book.cancel(99));
    }

    public void testIdsAreNeverReused() {
        OrderBook book = new OrderBook();
        book.submit(limit(1, Side.SELL, 100, 1));
        book.submit(limit(2, Side.BUY, 100, 1));
        assertThrows(IllegalArgumentException.class, () -> book.submit(limit(1, Side.SELL, 100, 1)));
        book.submit(limit(3, Side.SELL, 100, 1));
        book.cancel(3);
        assertThrows(IllegalArgumentException.class, () -> book.submit(limit(3, Side.SELL, 100, 1)));
        book.submit(market(4, Side.BUY, 1));
        assertThrows(IllegalArgumentException.class, () -> book.submit(market(4, Side.BUY, 1)));
        assertEquals(List.of(), book.depth(Side.SELL, 5));
    }

    public void testRejectedOrderChangesNothing() {
        OrderBook book = new OrderBook();
        book.submit(limit(1, Side.SELL, 100, 1));
        assertThrows(IllegalArgumentException.class, () -> book.submit(limit(1, Side.BUY, 100, 1)));
        assertEquals(List.of(new Level(100, 1)), book.depth(Side.SELL, 5));
        assertThrows(IllegalArgumentException.class, () -> book.submit(market(2, Side.BUY, -1)));
        assertEquals(List.of(new Trade(2, 1, 100, 1)), book.submit(limit(2, Side.BUY, 100, 1)));
    }

    public void testManyOrders() {
        OrderBook book = new OrderBook();
        for (int i = 0; i < 20_000; i++) book.submit(limit(i, Side.SELL, 1000 + i % 50, 1));
        List<Trade> trades = book.submit(limit(1_000_000, Side.BUY, 1049, 20_000));
        assertEquals(20_000, trades.size());
        assertEquals(1000L, trades.get(0).price());
        assertEquals(0L, trades.get(0).sellOrderId());
        assertEquals(1049L, trades.get(trades.size() - 1).price());
        assertEquals(List.of(), book.depth(Side.SELL, 5));
    }
}
