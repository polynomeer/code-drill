# 0/1 배낭

물건의 무게 배열 `weights` 와 가치 배열 `values`, 배낭 용량 `capacity` 가 주어진다.
각 물건은 **넣거나 넣지 않거나** 둘 중 하나일 때, 담을 수 있는 최대 가치를 반환한다.

```kotlin
fun knapsack(weights: IntArray, values: IntArray, capacity: Int): Int
```

같은 물건을 여러 번 담을 수 없다. 1차원 배열로 풀 때 용량을 **큰 쪽부터** 훑어야 하며,
작은 쪽부터 훑으면 같은 물건을 두 번 담게 된다.

가치 대비 무게가 좋은 것부터 욕심껏 담는 방법은 틀린다.

## 실행 리플레이 계측 (선택)

풀이 과정을 시각적으로 되짚고 싶으면 `Drill` SDK 를 호출한다. 채점 실행에서는
no-op 으로 컴파일되므로 시간·메모리 판정에 영향을 주지 않는다.

```kotlin
Drill.visit(item, value)     // 물건을 봤다
Drill.write(capacity, best)  // 그 용량에서의 최선
```

## 제약

- `1 <= weights.size == values.size <= 200`
- `1 <= weights[i] <= 1_000`
- `0 <= values[i] <= 10_000`
- `0 <= capacity <= 20_000`
