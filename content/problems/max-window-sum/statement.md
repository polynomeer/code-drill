# 크기 k 창의 최대 합

정수 배열 `nums` 와 정수 `k` 가 주어진다. 연속한 `k` 개 원소의 합 중 **최댓값**을
반환한다.

```kotlin
fun maxWindowSum(nums: IntArray, k: Int): Int
```

창을 한 칸 옮길 때 다시 더하지 않는다. 들어온 값을 더하고 나간 값을 빼면 한 번의
덧셈과 뺄셈으로 끝난다.

## 실행 리플레이 계측 (선택)

풀이 과정을 시각적으로 되짚고 싶으면 `Drill` SDK 를 호출한다. 채점 실행에서는
no-op 으로 컴파일되므로 시간·메모리 판정에 영향을 주지 않는다.

```kotlin
Drill.pointer("right", i)   // 창의 오른쪽 끝을 옮겼다
Drill.visit(i, value)       // 창에 들어온 값
Drill.write(0, sum)         // 현재 창의 합
```

## 제약

- `1 <= k <= nums.size <= 200_000`
- `-10^4 <= nums[i] <= 10^4`
