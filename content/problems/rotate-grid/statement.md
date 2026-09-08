# 격자 돌리기

격자 `grid` 를 시계 방향으로 90도 돌린 격자를 반환한다.

`rows × cols` 격자를 돌리면 **`cols × rows` 격자**가 된다.

```kotlin
fun rotate(grid: Array<IntArray>): Array<IntArray>
```

입력이 정사각형이라는 보장은 없다. 결과의 행 수는 입력의 열 수와 같다.

## 실행 리플레이 계측 (선택)

풀이 과정을 시각적으로 되짚고 싶으면 `Drill` SDK 를 호출한다. 채점 실행에서는
no-op 으로 컴파일되므로 시간·메모리 판정에 영향을 주지 않는다.

```kotlin
Drill.write(index, value)  // 결과 격자의 index 번째 칸을 채웠다 (행 우선)
```

## 제약

- `0 <= rows, cols <= 60`
- 모든 행의 길이는 같다
- `-1_000 <= grid[r][c] <= 1_000`
