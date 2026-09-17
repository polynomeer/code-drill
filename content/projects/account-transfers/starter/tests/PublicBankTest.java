package tests;

import bank.Bank;
import bank.BankExceptions.InsufficientFundsException;
import bank.Transaction;
import static codedrill.Assertions.*;

/** 공개 테스트. 채점은 이것과 숨은 테스트를 함께 돌린다. */
public class PublicBankTest {

    public void testOpenDepositWithdraw() {
        Bank bank = new Bank();
        bank.open("a", 100);
        bank.deposit("a", 50);
        bank.withdraw("a", 30);
        assertEquals(120L, bank.balance("a"));
    }

    public void testTransferMovesMoney() {
        Bank bank = new Bank();
        bank.open("a", 100);
        bank.open("b", 0);
        bank.transfer("a", "b", 40);
        assertEquals(60L, bank.balance("a"));
        assertEquals(40L, bank.balance("b"));
        assertEquals(Transaction.Kind.TRANSFER_OUT, bank.history("a").get(1).kind());
    }

    public void testInsufficientFunds() {
        Bank bank = new Bank();
        bank.open("a", 10);
        assertThrows(InsufficientFundsException.class, () -> bank.withdraw("a", 11));
        assertEquals(10L, bank.balance("a"));
    }
}
