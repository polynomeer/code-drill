# 배에 실을 최소 용량

짐의 무게가 `weights` 순서로 주어진다. **순서를 바꾸지 않고** 앞에서부터 차례로 실어
`days` 일 안에 전부 옮겨야 한다. 하루에 한 번 싣고, 하루에 싣는 무게의 합이 배의 용량을
넘을 수 없다. 이것이 가능한 **최소 용량**을 반환한다.

```kotlin
fun minCapacity(weights: IntArray, days: Int): Int
```

용량이 클수록 날 수가 준다 — 단조롭다. 그래서 답에 대해 이분 탐색할 수 있다: 어떤 용량으로
며칠이 드는지는 한 번 훑어 안다. 용량의 하한은 가장 무거운 짐, 상한은 전체 합이다.

## 실행 리플레이 계측 (선택)

풀이 과정을 시각적으로 되짚고 싶으면 `Drill` SDK 를 호출한다. 채점 실행에서는
no-op 으로 컴파일되므로 시간·메모리 판정에 영향을 주지 않는다.

```kotlin
Drill.pointer("lo", lo)        // 탐색 구간의 아래쪽
Drill.pointer("hi", hi)        // 탐색 구간의 위쪽
Drill.compare(mid, days)       // 용량 mid 로 며칠 드는지 봤다
```

## 제약

- `1 <= weights.size <= 100_000`, `1 <= weights[i] <= 500`
- `1 <= days <= weights.size`
