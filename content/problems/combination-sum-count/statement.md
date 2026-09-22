# 합이 목표인 조합의 수

서로 다른 양의 정수 `candidates` 와 `target` 이 주어진다. `candidates` 의 수를 **몇 번이든 다시 써서**
합이 `target` 이 되는 조합의 수를 반환한다. 순서만 다른 것은 같은 조합이다 — `2+3` 과 `3+2` 는 하나.

```kotlin
fun combinationSumCount(candidates: IntArray, target: Int): Int
```

순서를 세지 않으려면 **후보를 정한 순서로만** 고른다: `i` 번째 후보부터 보되, "이 후보를 하나 더 쓴다"
(`i` 그대로, 남은 합에서 뺀다)와 "이 후보는 그만 쓴다"(`i + 1`) 둘로 갈린다. 남은 합이 0 이면 하나,
음수거나 후보가 끝나면 0 이다. 상태 `(i, 남은 합)` 을 기억하면 `n × target` 이다 — 기억 없는 백트래킹은
`target = 500` 에서 지수다.

## 실행 리플레이 계측 (선택)

풀이 과정을 시각적으로 되짚고 싶으면 `Drill` SDK 를 호출한다. 채점 실행에서는
no-op 으로 컴파일되므로 시간·메모리 판정에 영향을 주지 않는다.

```kotlin
Drill.compare(i, remaining)   // 상태를 봤다
Drill.write(0, count)         // 조합을 하나 셌다
```

## 제약

- `1 <= candidates.length <= 20`, `1 <= candidates[i] <= 200`, 서로 다르다
- `1 <= target <= 500`, 답은 `Int` 안이다
