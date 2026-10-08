package book;

import java.util.List;

/**
 * 호가창 — 골격. 시그니처는 그대로 두고 본문을 채운다. 필드와 도우미는 마음대로 더한다.
 */
public class OrderBook {

    public List<Trade> submit(Order order) {
        throw new UnsupportedOperationException("TODO");
    }

    public boolean cancel(long orderId) {
        throw new UnsupportedOperationException("TODO");
    }

    public List<Level> depth(Side side, int levels) {
        throw new UnsupportedOperationException("TODO");
    }
}
