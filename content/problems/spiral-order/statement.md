# 나선으로 읽기

격자 `grid` 를 바깥부터 시계 방향 나선으로 훑은 순서대로 반환한다.

왼쪽 위에서 시작해 오른쪽으로 간다.

```kotlin
fun spiralOrder(grid: Array<IntArray>): IntArray
```

빈 격자는 빈 배열을 반환한다.

## 실행 리플레이 계측 (선택)

풀이 과정을 시각적으로 되짚고 싶으면 `Drill` SDK 를 호출한다. 채점 실행에서는
no-op 으로 컴파일되므로 시간·메모리 판정에 영향을 주지 않는다.

```kotlin
Drill.write(head, value)  // 결과 배열의 head 번째를 채웠다
```

## 제약

- `0 <= rows, cols <= 60`
- 모든 행의 길이는 같다
- `-1_000 <= grid[r][c] <= 1_000`
