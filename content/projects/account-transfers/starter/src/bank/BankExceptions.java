package bank;

/** 예외 셋. 이름과 메시지 규칙은 요구사항에 있다. 이 파일은 그대로 둔다. */
public final class BankExceptions {
    private BankExceptions() {}

    public static class DuplicateAccountException extends RuntimeException {
        public DuplicateAccountException(String id) { super("account already exists: " + id); }
    }

    public static class NoSuchAccountException extends RuntimeException {
        public NoSuchAccountException(String id) { super("no such account: " + id); }
    }

    public static class InsufficientFundsException extends RuntimeException {
        public InsufficientFundsException(String id, long requested, long available) {
            super(id + ": requested " + requested + ", available " + available);
        }
    }
}
