package bank;

/** 거래 기록 하나. 값 객체라 바뀌지 않는다. */
public record Transaction(Kind kind, long amount, String counterparty) {
    public enum Kind { DEPOSIT, WITHDRAWAL, TRANSFER_IN, TRANSFER_OUT }
}
