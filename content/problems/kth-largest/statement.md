# k 번째로 큰 수

정수 배열 `nums` 와 정수 `k` 가 주어진다. **정렬했을 때** `k` 번째로 큰 값을 반환한다.

중복은 따로 세지 않는다. `[3, 3, 1]` 에서 2번째로 큰 값은 `3` 이다.

```kotlin
fun kthLargest(nums: IntArray, k: Int): Int
```

## 실행 리플레이 계측 (선택)

풀이 과정을 시각적으로 되짚고 싶으면 `Drill` SDK 를 호출한다. 채점 실행에서는
no-op 으로 컴파일되므로 시간·메모리 판정에 영향을 주지 않는다.

```kotlin
Drill.visit(index, value)  // 원소를 봤다
Drill.compare(left, right) // 크기를 견줬다
Drill.write(0, value)      // 현재 k 번째 후보
```

## 제약

- `1 <= k <= nums.size <= 200_000`
- `-10^9 <= nums[i] <= 10^9`
