# 계좌와 이체

`src/bank/Bank.java` 의 `Bank` 를 완성한다. 계좌를 열고 입금·출금·이체하며, 계좌마다 거래 기록을
남긴다. 금액은 정수(원)다.

## 요구사항

### `open(String id, long initial)`

- 같은 `id` 가 이미 있으면 `DuplicateAccountException`. `initial` 이 음수면 `IllegalArgumentException`.
- 초기 잔액이 0 보다 크면 그 금액의 `DEPOSIT` 기록이 하나 남는다. 0 이면 기록이 없다.

### `deposit(String id, long amount)` / `withdraw(String id, long amount)`

- 없는 계좌면 `NoSuchAccountException`. `amount` 가 1 미만이면 `IllegalArgumentException`.
- 출금은 잔액이 모자라면 `InsufficientFundsException` — 잔액과 기록은 바뀌지 않는다.
- 성공하면 `DEPOSIT`/`WITHDRAWAL` 기록이 남는다.

### `transfer(String from, String to, long amount)`

- 두 계좌가 모두 있어야 하고, `from` 과 `to` 가 같으면 `IllegalArgumentException`.
- 잔액이 모자라면 `InsufficientFundsException` 이고 **어느 계좌도 바뀌지 않는다** — 일부만 나가는
  일은 없다.
- 성공하면 `from` 에 `TRANSFER_OUT`, `to` 에 `TRANSFER_IN` 기록이 남는다. 둘의 `amount` 는 같고
  `counterparty` 는 상대 계좌다.

### `balance(String id)` / `history(String id)`

- `history` 는 `Transaction(kind, amount, counterparty)` 의 목록을 일어난 순서로. 이체가 아니면
  `counterparty` 는 `null`. **반환한 목록을 바꿔도 은행은 바뀌지 않는다.**
- 둘 다 없는 계좌면 `NoSuchAccountException`.

## 제출

`src/bank/Bank.java` 를 고친다. 예외 클래스와 `Transaction` 은 `src/bank/` 에 이미 있고 그대로
둔다. `tests/` 아래의 파일은 채점 때 숨은 스위트로 덮인다. 테스트는 `import static codedrill.Assertions.*;`
의 `assertEquals`·`assertTrue`·`assertNull`·`assertThrows` 를 쓴다. 테스트는 이름이 `Test` 로 끝나는
클래스의 `public void test…()` 메서드다.
