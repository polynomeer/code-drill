package tests;

import bank.Bank;
import bank.BankExceptions.DuplicateAccountException;
import bank.BankExceptions.InsufficientFundsException;
import bank.BankExceptions.NoSuchAccountException;
import bank.Transaction;
import java.util.List;
import static codedrill.Assertions.*;

/** 숨은 테스트. 사용자에게 나가지 않는다. */
public class HiddenBankTest {

    public void testTransferIsAtomicOnShortage() {
        Bank bank = new Bank();
        bank.open("a", 30);
        bank.open("b", 5);
        assertThrows(InsufficientFundsException.class, () -> bank.transfer("a", "b", 31));
        assertEquals(30L, bank.balance("a"));
        assertEquals(5L, bank.balance("b"));
        assertEquals(1, bank.history("a").size());
        assertEquals(1, bank.history("b").size());
    }

    public void testTransferRecordsBothSidesWithCounterparty() {
        Bank bank = new Bank();
        bank.open("a", 100);
        bank.open("b", 0);
        bank.transfer("a", "b", 25);
        Transaction out = bank.history("a").get(1);
        Transaction in = bank.history("b").get(0);
        assertEquals(new Transaction(Transaction.Kind.TRANSFER_OUT, 25, "b"), out);
        assertEquals(new Transaction(Transaction.Kind.TRANSFER_IN, 25, "a"), in);
    }

    public void testTransferToSelfIsRejected() {
        Bank bank = new Bank();
        bank.open("a", 100);
        assertThrows(IllegalArgumentException.class, () -> bank.transfer("a", "a", 10));
        assertEquals(100L, bank.balance("a"));
    }

    public void testTransferToMissingAccountChangesNothing() {
        Bank bank = new Bank();
        bank.open("a", 100);
        assertThrows(NoSuchAccountException.class, () -> bank.transfer("a", "zzz", 10));
        assertEquals(100L, bank.balance("a"));
        assertEquals(1, bank.history("a").size());
    }

    public void testDuplicateAndNegativeOpen() {
        Bank bank = new Bank();
        bank.open("a", 0);
        assertThrows(DuplicateAccountException.class, () -> bank.open("a", 5));
        assertThrows(IllegalArgumentException.class, () -> bank.open("b", -1));
        assertThrows(NoSuchAccountException.class, () -> bank.balance("b"));
    }

    public void testZeroInitialLeavesNoRecord() {
        Bank bank = new Bank();
        bank.open("a", 0);
        assertEquals(0, bank.history("a").size());
        bank.open("b", 7);
        assertEquals(List.of(new Transaction(Transaction.Kind.DEPOSIT, 7, null)), bank.history("b"));
    }

    public void testNonPositiveAmountsRejected() {
        Bank bank = new Bank();
        bank.open("a", 10);
        assertThrows(IllegalArgumentException.class, () -> bank.deposit("a", 0));
        assertThrows(IllegalArgumentException.class, () -> bank.withdraw("a", -5));
        assertEquals(10L, bank.balance("a"));
    }

    public void testHistoryIsACopy() {
        Bank bank = new Bank();
        bank.open("a", 10);
        List<Transaction> history = bank.history("a");
        // 바꿀 수 없는 목록이어도, 바꿀 수 있는 복사본이어도 좋다 — 원장이 그대로면 된다.
        try { history.clear(); } catch (RuntimeException ignored) { }
        assertEquals(1, bank.history("a").size());
    }

    public void testRejectedWithdrawalLeavesNoRecord() {
        Bank bank = new Bank();
        bank.open("a", 10);
        assertThrows(InsufficientFundsException.class, () -> bank.withdraw("a", 11));
        assertEquals(1, bank.history("a").size());
        assertEquals(Transaction.Kind.DEPOSIT, bank.history("a").get(0).kind());
    }

    public void testExactBalanceWithdrawal() {
        Bank bank = new Bank();
        bank.open("a", 10);
        bank.withdraw("a", 10);
        assertEquals(0L, bank.balance("a"));
        assertThrows(InsufficientFundsException.class, () -> bank.withdraw("a", 1));
    }
}
