package book;

/** 들어오는 주문. 시장가 주문의 price 는 보지 않는다. 값 객체라 바뀌지 않는다. */
public record Order(long id, Side side, OrderType type, long price, long quantity) {
}
