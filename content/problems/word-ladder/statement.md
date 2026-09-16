# 단어 사다리

같은 길이의 소문자 단어 `begin`, `end` 와 단어 목록 `words` 가 주어진다. `begin` 에서
시작해 **한 번에 글자 하나만 바꿔** `end` 에 이르되, 중간의 단어는 전부 `words` 에 있어야
한다. 가장 짧은 변환의 **단어 수** (`begin` 과 `end` 포함) 를 반환한다. 없으면 `0`.
`end` 가 `words` 에 없으면 `0` 이다.

예: `hit → hot → dot → dog → cog` 이면 `5`.

```kotlin
fun wordLadder(begin: String, end: String, words: Array<String>): Int
```

단어가 정점이고 "글자 하나 차이"가 간선인 그래프의 최단 경로 — BFS 다. 간선을 모든 쌍으로
만들면 O(n²·L) — 5 만 단어면 12 억 번 — 이고, 단어마다 자리 하나를 26 글자로 바꿔 목록에 있는지 보면 O(n·L·26) 이다.
방문 표시가 없으면 같은 단어를 몇 번이고 다시 넣어 끝나지 않는다.

## 실행 리플레이 계측 (선택)

풀이 과정을 시각적으로 되짚고 싶으면 `Drill` SDK 를 호출한다. 채점 실행에서는
no-op 으로 컴파일되므로 시간·메모리 판정에 영향을 주지 않는다.

```kotlin
Drill.enqueue(i)              // 단어를 큐에 넣었다 (목록의 자리)
Drill.dequeue(i)              // 꺼냈다
Drill.match(i, steps)         // end 에 닿았다
```

## 제약

- `1 <= 단어 길이 <= 10`, `0 <= words.size <= 50_000`, 소문자만
- `begin != end`
