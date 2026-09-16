# 조상보다 작지 않은 정점

트리가 부모 배열로 주어진다. `parent[i]` 는 `i` 의 부모이고 루트는 `-1` 이다. 부모의 번호는
항상 자식보다 작다. `values[i]` 는 정점의 값이다.

루트에서 어떤 정점까지 내려오는 길에 그 정점보다 **큰 값이 없으면** 그 정점은 좋은 정점이다
— 조상들 중 자기보다 큰 것이 없다. 루트는 언제나 좋다. 좋은 정점의 수를 반환한다.

```kotlin
fun goodNodes(parent: IntArray, values: IntArray): Int
```

정점마다 "루트에서 여기까지의 최댓값"을 알면 된다. 부모가 자식보다 앞에 오므로 번호 순서로
한 번 지나며 `best[i] = max(best[parent], values[i])` 를 채우면 재귀도 스택도 필요 없다. 좋은
정점의 조건은 `values[i] >= best[parent]` — 같으면 좋다.

## 실행 리플레이 계측 (선택)

풀이 과정을 시각적으로 되짚고 싶으면 `Drill` SDK 를 호출한다. 채점 실행에서는
no-op 으로 컴파일되므로 시간·메모리 판정에 영향을 주지 않는다.

```kotlin
Drill.visit(i, values[i])     // 정점을 봤다
Drill.compare(values[i], above)
```

## 제약

- `1 <= n <= 100_000`
- `parent[i] < i` (루트는 `-1`), 값은 `-10^4 <= values[i] <= 10^4`
