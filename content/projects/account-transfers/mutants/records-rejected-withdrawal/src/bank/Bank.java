package bank;

import bank.BankExceptions.DuplicateAccountException;
import bank.BankExceptions.InsufficientFundsException;
import bank.BankExceptions.NoSuchAccountException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

// kind: WRONG_BRANCH
// 부족해서 거절한 출금도 기록에 남긴다. 잔액은 맞아서 기록을 세기 전에는 안 드러난다.
public class Bank {

    private static final class Account {
        long balance;
        final List<Transaction> history = new ArrayList<>();
    }

    private final Map<String, Account> accounts = new HashMap<>();

    private Account get(String id) {
        Account account = accounts.get(id);
        if (account == null) throw new NoSuchAccountException(id);
        return account;
    }

    private static void requirePositive(long amount) {
        if (amount < 1) throw new IllegalArgumentException("amount must be positive: " + amount);
    }

    public void open(String id, long initial) {
        if (initial < 0) throw new IllegalArgumentException("initial must not be negative: " + initial);
        if (accounts.containsKey(id)) throw new DuplicateAccountException(id);
        Account account = new Account();
        account.balance = initial;
        if (initial > 0) account.history.add(new Transaction(Transaction.Kind.DEPOSIT, initial, null));
        accounts.put(id, account);
    }

    public void deposit(String id, long amount) {
        requirePositive(amount);
        Account account = get(id);
        account.balance += amount;
        account.history.add(new Transaction(Transaction.Kind.DEPOSIT, amount, null));
    }

    public void withdraw(String id, long amount) {
        requirePositive(amount);
        Account account = get(id);
        account.history.add(new Transaction(Transaction.Kind.WITHDRAWAL, amount, null));
        if (account.balance < amount) throw new InsufficientFundsException(id, amount, account.balance);
        account.balance -= amount;
    }

    public void transfer(String from, String to, long amount) {
        requirePositive(amount);
        if (from.equals(to)) throw new IllegalArgumentException("cannot transfer to the same account");
        Account source = get(from);
        Account target = get(to);
        if (source.balance < amount) throw new InsufficientFundsException(from, amount, source.balance);
        source.balance -= amount;
        target.balance += amount;
        source.history.add(new Transaction(Transaction.Kind.TRANSFER_OUT, amount, to));
        target.history.add(new Transaction(Transaction.Kind.TRANSFER_IN, amount, from));
    }

    public long balance(String id) {
        return get(id).balance;
    }

    public List<Transaction> history(String id) {
        return List.copyOf(get(id).history);
    }
}
