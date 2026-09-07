# 가장 많은 물을 담는 그릇

높이 배열 `heights` 가 주어진다. 두 선분을 골라 만든 그릇에 담을 수 있는 물의 최대
넓이를 반환한다. 넓이는 `(오른쪽 인덱스 - 왼쪽 인덱스) x min(두 높이)` 다.

```kotlin
fun maxWater(heights: IntArray): Int
```

양 끝에서 좁혀 온다. **낮은 쪽을 옮기는 것**이 핵심이다 — 낮은 쪽을 그대로 두면 폭만
줄어드니, 그 선분이 만들 수 있는 더 넓은 그릇은 이미 다 봤다.

## 실행 리플레이 계측 (선택)

풀이 과정을 시각적으로 되짚고 싶으면 `Drill` SDK 를 호출한다. 채점 실행에서는
no-op 으로 컴파일되므로 시간·메모리 판정에 영향을 주지 않는다.

```kotlin
Drill.pointer("left", left)    // 왼쪽 벽
Drill.pointer("right", right)  // 오른쪽 벽
Drill.write(0, area)           // 지금까지의 최대 넓이
```

## 제약

- `2 <= heights.size <= 100_000`
- `0 <= heights[i] <= 10_000`
