# 섬의 개수

`grid` 는 0(바다)과 1(땅)로 이루어진 격자를 행 우선으로 편 1차원 배열이고, `width` 는
한 행의 길이다. 상하좌우로 이어진 땅 덩어리(섬)의 개수를 반환한다.

```kotlin
fun countIslands(grid: IntArray, width: Int): Int
```

## 실행 리플레이 계측 (선택)

이 문제는 다섯 렌더러를 모두 쓸 수 있다. 어떤 것을 부르든 자유이며, 채점 실행에서는
전부 no-op 으로 컴파일된다.

```kotlin
Drill.visit(index, value)      // 칸을 살펴봤다 (배열)
Drill.pointer("scan", index)   // 훑고 있는 위치 (포인터)
Drill.enqueue(index)           // BFS 큐에 넣었다 (큐)
Drill.dequeue(index)           // BFS 큐에서 꺼냈다 (큐)
Drill.push(index)              // DFS 스택에 넣었다 (스택)
Drill.pop(index)               // DFS 스택에서 꺼냈다 (스택)
Drill.node("r1c2")             // 정점 방문 (그래프)
Drill.edge("r1c2", "r1c3")     // 이웃 연결 (그래프)
Drill.call("flood(3)")         // 재귀 진입 (재귀)
Drill.ret("flood(3)", 5)       // 재귀 반환 (재귀)
Drill.match(index, index)      // 섬 하나를 확정했다
```

## 제약

- `1 <= width <= 200`
- `grid.size` 는 `width` 의 배수이고 40,000 이하다
- 각 원소는 0 또는 1

## 예제

두 번째 예제는 `width` 가 2 이므로 2행 2열이다. 인덱스 0 과 2 는 세로로 이어져 한 섬이다.

| grid | width | 반환 |
|---|---|---|
| `[1,1,0,0,1]` | `5` | `2` |
| `[1,0,1,0]` | `2` | `1` |
