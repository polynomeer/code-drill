package parking;

/** 예외 셋. 이름과 메시지 규칙은 요구사항에 있다. 이 파일은 그대로 둔다. */
public final class ParkingExceptions {
    private ParkingExceptions() {}

    public static class LotFullException extends RuntimeException {
        public LotFullException(VehicleSize size) { super("no spot for " + size); }
    }

    public static class AlreadyParkedException extends RuntimeException {
        public AlreadyParkedException(String plate) { super("already parked: " + plate); }
    }

    public static class UnknownTicketException extends RuntimeException {
        public UnknownTicketException(String id) { super("unknown ticket: " + id); }
    }
}
