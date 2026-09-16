# 마감 전에 끝낼 수 있는 작업의 최대 수

작업이 `n` 개 있다. `durations[i]` 는 `i` 번째 작업에 걸리는 시간, `deadlines[i]` 는 그 작업을
**끝내야 하는** 시각이다. 시각 `0` 부터 작업을 하나씩 골라 쉬지 않고 이어서 한다 — 한 번에
하나, 중간에 끊지 않는다. 작업은 끝나는 시각이 마감 **이하**여야 한다.

끝낼 수 있는 작업 수의 최댓값을 반환한다. 어떤 작업을 골라 어떤 순서로 할지는 자유다.

```kotlin
fun maxTasksBeforeDeadlines(durations: IntArray, deadlines: IntArray): Int
```

마감이 이른 순서로 보면서 일단 넣고, 넣은 것들의 합이 지금 작업의 마감을 넘기면 **지금까지
넣은 것 중 가장 긴 하나**를 뺀다. 뺄 것을 빨리 찾는 구조가 최대 힙이다. 남은 작업 수가
답이다 — 무엇을 남겼는지는 안 물어본다.

## 실행 리플레이 계측 (선택)

풀이 과정을 시각적으로 되짚고 싶으면 `Drill` SDK 를 호출한다. 채점 실행에서는
no-op 으로 컴파일되므로 시간·메모리 판정에 영향을 주지 않는다.

```kotlin
Drill.push(duration)          // 작업을 힙에 넣었다
Drill.pop(duration)           // 가장 긴 작업을 뺐다
Drill.compare(elapsed, deadline)
```

## 제약

- `1 <= n <= 100_000`
- `1 <= durations[i] <= 1000`, `1 <= deadlines[i] <= 10^8`
- `durations.size == deadlines.size`
