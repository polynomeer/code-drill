# 차량 무리의 수

한 차선 도로의 목적지가 `target` 이다. 차 `i` 는 `positions[i]` 에서 출발해 초당 `speeds[i]` 로
목적지를 향해 달린다(출발 위치는 서로 다르다). 앞차를 **추월할 수 없다** — 따라잡으면 앞차의 속도로
바로 뒤에 붙어 한 **무리**가 되어 함께 간다. 목적지에 닿을 때의 무리의 수를 반환한다. 목적지에서
정확히 따라잡아도 같은 무리다.

```kotlin
fun carFleetCount(target: Int, positions: IntArray, speeds: IntArray): Int
```

목적지에 가까운 차부터 본다. 각 차가 혼자 달렸을 때 걸리는 시간 `(target − pos) / speed` 를 재면,
뒤차의 시간이 앞차의 시간보다 **작거나 같으면** 앞차를 따라잡아 그 무리에 든다 — 그 무리의 시간은
앞차(가장 느린 것)의 시간이다. 시간이 더 크면 새 무리다. 위치로 내림차순 정렬한 뒤 한 번 훑으며
"지금 무리의 시간"만 들고 가면 되고, 그것은 스택에 시간을 쌓다 작은 것을 흡수하는 것과 같다.

## 실행 리플레이 계측 (선택)

풀이 과정을 시각적으로 되짚고 싶으면 `Drill` SDK 를 호출한다. 채점 실행에서는
no-op 으로 컴파일되므로 시간·메모리 판정에 영향을 주지 않는다.

```kotlin
Drill.compare(i, fleets)      // 차를 봤다
Drill.write(0, fleets)        // 무리를 하나 늘렸다
```

## 제약

- `1 <= positions.length <= 100_000`, `positions.length == speeds.length`
- `0 <= positions[i] < target <= 10^6`, `1 <= speeds[i] <= 100`, 위치는 서로 다르다
