package bank;

import java.util.List;

/** 은행 원장 — 골격. 시그니처는 그대로 두고 본문을 채운다. 요구사항은 문제 본문에 있다. */
public class Bank {

    public void open(String id, long initial) {
        throw new UnsupportedOperationException("TODO");
    }

    public void deposit(String id, long amount) {
        throw new UnsupportedOperationException("TODO");
    }

    public void withdraw(String id, long amount) {
        throw new UnsupportedOperationException("TODO");
    }

    public void transfer(String from, String to, long amount) {
        throw new UnsupportedOperationException("TODO");
    }

    public long balance(String id) {
        throw new UnsupportedOperationException("TODO");
    }

    public List<Transaction> history(String id) {
        throw new UnsupportedOperationException("TODO");
    }
}
