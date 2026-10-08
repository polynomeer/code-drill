package book;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.NavigableMap;
import java.util.Set;
import java.util.TreeMap;

/**
 * 호가창. 가격마다 쉬고 있는 주문의 줄(먼저 온 것이 앞)을 두고, 들어오는 주문은 상대편의 가장 좋은 가격부터 체결한다.
 */
// kind: MISSING_EDGE_CASE
// 조금이라도 체결된 지정가 주문은 남은 수량을 버린다. 지정가의 남은 것은 호가창에 쉰다.
public class OrderBook {

    private static final class Resting {
        final long id;
        final Side side;
        final long price;
        long remaining;

        Resting(long id, Side side, long price, long remaining) {
            this.id = id;
            this.side = side;
            this.price = price;
            this.remaining = remaining;
        }
    }

    // 사는 쪽은 비싼 값이, 파는 쪽은 싼 값이 먼저다
    private final NavigableMap<Long, ArrayDeque<Resting>> bids = new TreeMap<>(Collections.reverseOrder());
    private final NavigableMap<Long, ArrayDeque<Resting>> asks = new TreeMap<>();
    private final Map<Long, Resting> resting = new HashMap<>();
    private final Set<Long> seen = new HashSet<>();

    public List<Trade> submit(Order order) {
        validate(order);
        seen.add(order.id());
        NavigableMap<Long, ArrayDeque<Resting>> opposite = order.side() == Side.BUY ? asks : bids;
        List<Trade> trades = new ArrayList<>();
        long left = order.quantity();
        while (left > 0 && !opposite.isEmpty()) {
            Map.Entry<Long, ArrayDeque<Resting>> best = opposite.firstEntry();
            long price = best.getKey();
            if (order.type() == OrderType.LIMIT && !crosses(order, price)) break;
            ArrayDeque<Resting> queue = best.getValue();
            while (left > 0 && !queue.isEmpty()) {
                Resting head = queue.peekFirst();
                long filled = Math.min(left, head.remaining);
                left -= filled;
                head.remaining -= filled;
                trades.add(order.side() == Side.BUY
                    ? new Trade(order.id(), head.id, price, filled)
                    : new Trade(head.id, order.id(), price, filled));
                if (head.remaining == 0) {
                    queue.pollFirst();
                    resting.remove(head.id);
                }
            }
            if (queue.isEmpty()) opposite.remove(price);
        }
        if (left > 0 && order.type() == OrderType.LIMIT && trades.isEmpty()) {
            Resting rest = new Resting(order.id(), order.side(), order.price(), left);
            (order.side() == Side.BUY ? bids : asks).computeIfAbsent(order.price(), p -> new ArrayDeque<>()).addLast(rest);
            resting.put(order.id(), rest);
        }
        return trades;
    }

    private static boolean crosses(Order order, long restingPrice) {
        return order.side() == Side.BUY ? restingPrice <= order.price() : restingPrice >= order.price();
    }

    private void validate(Order order) {
        if (order == null || order.side() == null || order.type() == null) throw new IllegalArgumentException("incomplete order");
        if (order.quantity() <= 0) throw new IllegalArgumentException("quantity must be positive");
        if (order.type() == OrderType.LIMIT && order.price() <= 0) throw new IllegalArgumentException("limit price must be positive");
        if (seen.contains(order.id())) throw new IllegalArgumentException("duplicate order id: " + order.id());
    }

    public boolean cancel(long orderId) {
        Resting rest = resting.remove(orderId);
        if (rest == null) return false;
        NavigableMap<Long, ArrayDeque<Resting>> book = rest.side == Side.BUY ? bids : asks;
        ArrayDeque<Resting> queue = book.get(rest.price);
        for (Iterator<Resting> it = queue.iterator(); it.hasNext(); ) {
            if (it.next().id == orderId) {
                it.remove();
                break;
            }
        }
        if (queue.isEmpty()) book.remove(rest.price);
        return true;
    }

    public List<Level> depth(Side side, int levels) {
        if (side == null || levels < 0) throw new IllegalArgumentException("bad depth request");
        List<Level> out = new ArrayList<>();
        for (Map.Entry<Long, ArrayDeque<Resting>> e : (side == Side.BUY ? bids : asks).entrySet()) {
            if (out.size() == levels) break;
            long total = 0;
            for (Resting r : e.getValue()) total += r.remaining;
            out.add(new Level(e.getKey(), total));
        }
        return out;
    }
}
