# 재고 원장

창고 하나의 재고를 기록하는 작은 모듈 `ledger/inventory.py` 를 완성한다. 시작 저장소에
클래스와 메서드의 골격이 있고, `tests/test_public.py` 에 공개 테스트가 셋 있다. 채점은
**공개 테스트와 숨은 테스트를 함께** 돌려 전부 통과해야 정답이다.

## 요구사항

수량은 정수 개, 원가는 정수 원(단위 없음)이다. 실수는 쓰지 않는다.

### 입고 — `receive(sku, quantity, unit_cost)`

- `quantity` 가 1 이상, `unit_cost` 가 0 이상이 아니면 `InvalidQuantity` 를 던지고 원장은 바뀌지 않는다.
- 입고 한 번이 **로트 하나**다. 같은 SKU 를 여러 번 받으면 로트가 여럿이다.

### 출고 — `ship(sku, quantity) -> int`

- `quantity` 가 1 이상이 아니면 `InvalidQuantity`.
- 재고가 모자라면 `InsufficientStock` 을 던진다. 예외에는 `sku`, `requested`, `available` 속성이
  있어야 하고, **원장은 바뀌지 않아야 한다** — 일부만 나가는 일은 없다.
- 원가는 **선입선출(FIFO)** 이다. 가장 오래된 로트부터 소진하고, 로트 하나가 일부만 나가면
  나머지는 그 로트에 남는다. 반환값은 나간 물량의 원가 합이다.

### 조회

- `on_hand(sku) -> int`: 남은 수량. 모르는 SKU 는 0 이다.
- `valuation() -> int`: 모든 SKU 의 남은 로트에 대한 `수량 × 단가` 의 합.
- `skus() -> list[str]`: 한 번이라도 입고된 SKU 를 **정렬해서**. 다 나간 것도 들어간다.
- `history(sku) -> list[Movement]`: 그 SKU 의 입출고 기록을 일어난 순서로. `Movement` 는
  `kind`(`"RECEIVE"` 또는 `"SHIP"`), `quantity`, `cost`(입고면 `수량 × 단가`, 출고면 원가 합)를
  가진 불변 값이다. **반환한 목록을 호출자가 바꿔도 원장은 바뀌지 않아야 한다.**

## 제출

`ledger/inventory.py` 를 고친다. 파일을 더 만들어도 되지만 `tests/` 아래의 파일은 채점 때
숨은 스위트로 덮인다. 테스트 기반(`unittest`)을 건드리는 제출은 정답으로 인정하지 않는다.
