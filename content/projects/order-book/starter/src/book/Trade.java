package book;

/** 체결 한 건. 가격은 쉬고 있던 주문의 가격이다. */
public record Trade(long buyOrderId, long sellOrderId, long price, long quantity) {
}
