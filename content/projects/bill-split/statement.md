# 계산서 나누기

여럿이 함께 먹은 계산서를 나누는 부분을 만든다. `src/split/BillSplitter.kt` 의 `BillSplitter` 를 완성한다.
`Share`(한 사람의 몫)는 `src/split/Share.kt` 에 있고 고치지 않는다.

금액은 모두 **원 단위 정수**(`Long`)다. 어떤 방법으로 나누든 **몫의 합은 원래 금액과 정확히 같다.**

## `splitEvenly(total, people): List<Share>`

`total` 을 `people` 에게 똑같이 나눈다. 나누어떨어지지 않아 남는 원은 **목록의 앞사람부터** 1원씩 더 낸다.
결과는 `people` 의 순서 그대로다.

- `splitEvenly(100, ["민지", "서준", "하은"])` → `민지 34, 서준 33, 하은 33`

## `splitByWeight(total, weights): List<Share>`

`weights` 는 `(이름, 가중치)` 의 목록이다. 각자의 정확한 몫은 `total × 가중치 ÷ 가중치 합` 이다.

1. 모두에게 정확한 몫의 **내림**을 먼저 준다.
2. 그러고 남은 원은 **버린 소수 부분이 큰 사람부터** 1원씩 준다. 소수 부분이 같으면 목록의 앞사람이 먼저다.

가중치가 0 인 사람은 0원이다. 결과는 `weights` 의 순서 그대로다.

- `splitByWeight(7, [a 2, b 3, c 5])` → 정확한 몫 1.4, 2.1, 3.5 → `a 1, b 2, c 4`

금액은 1조 원까지, 가중치는 100만까지 온다. 둘의 곱은 `Long` 에 들어가지만 `Double` 로 계산하면 정밀도가 모자라
1원이 틀린다.

## `addTip(total, percent): Long`

봉사료를 더한 금액. 봉사료는 `total × percent ÷ 100` 을 원 단위로 **반올림**한 것이다(0.5원은 올린다).

- `addTip(1005, 10)` → 봉사료 100.5 → 101 → `1106`

## 잘못된 입력

다음은 모두 `IllegalArgumentException` 이다.

- `total` 이 0 보다 작거나 1조(`1_000_000_000_000`)보다 크다.
- 사람이 없다, 이름이 비었거나 공백뿐이다, **같은 이름이 두 번** 나온다.
- 가중치가 0 보다 작거나 100만보다 크다, 가중치가 모두 0 이다.
- `percent` 가 0..100 밖이다.

## 제출

`src/split/BillSplitter.kt` 를 고친다. `tests/` 아래의 파일은 채점 때 숨은 스위트로 덮인다. 테스트는
`import codedrill.*` 의 `assertEquals`·`assertTrue`·`assertFalse`·`assertNull`·`assertThrows<T>` 를 쓴다 —
표준 라이브러리 외의 의존성은 없다. 테스트는 이름이 `Test` 로 끝나는 클래스의 `test…` 메서드다.
