# 가장 적은 간선으로 가기

정점이 `0` 부터 `n-1` 까지 있는 **무방향** 그래프가 주어진다. `edges` 는 간선을 평탄하게
이은 배열로, `[a1, b1, a2, b2, ...]` 형태다.

정점 `0` 에서 `target` 까지 가는 데 필요한 **최소 간선 수**를 반환한다. 갈 수 없으면
`-1` 이다.

```kotlin
fun shortestHops(n: Int, edges: IntArray, target: Int): Int
```

가중치가 없으므로 너비 우선 탐색이 곧 최단 거리다. 깊이 우선으로 먼저 닿은 경로를
답으로 삼으면 더 짧은 길을 놓친다.

## 실행 리플레이 계측 (선택)

풀이 과정을 시각적으로 되짚고 싶으면 `Drill` SDK 를 호출한다. 채점 실행에서는
no-op 으로 컴파일되므로 시간·메모리 판정에 영향을 주지 않는다.

```kotlin
Drill.node("v3")           // 정점을 방문했다
Drill.edge("v3", "v7")     // 간선을 따라갔다
Drill.enqueue(next)        // 큐에 넣었다
Drill.dequeue(node)        // 큐에서 꺼냈다
```

## 제약

- `1 <= n <= 100_000`
- `edges.size` 는 짝수이며 `0 <= edges.size <= 400_000`
- `0 <= target < n`
