# 최소 동전 개수

동전 금액 배열 `coins` 와 목표 금액 `amount` 가 주어진다. 목표를 만드는 데 필요한
**최소 동전 개수**를 반환한다. 만들 수 없으면 `-1` 이다. 각 동전은 몇 번이든 쓸 수 있다.

```kotlin
fun coinChange(coins: IntArray, amount: Int): Int
```

큰 동전부터 욕심껏 집으면 틀린다. `[1, 3, 4]` 로 6 을 만들 때 `4 + 1 + 1` 이 아니라
`3 + 3` 이 답이다.

## 실행 리플레이 계측 (선택)

풀이 과정을 시각적으로 되짚고 싶으면 `Drill` SDK 를 호출한다. 채점 실행에서는
no-op 으로 컴파일되므로 시간·메모리 판정에 영향을 주지 않는다.

```kotlin
Drill.write(value, count)  // value 를 만드는 최소 개수
Drill.compare(value, coin) // 이 동전을 써 봤다
```

## 제약

- `1 <= coins.size <= 20`
- `1 <= coins[i] <= 10_000`
- `0 <= amount <= 100_000`
