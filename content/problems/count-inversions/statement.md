# 역순 쌍의 개수

정수 배열 `nums` 에서 `i < j` 이면서 `nums[i] > nums[j]` 인 쌍의 개수를 반환한다.
"얼마나 정렬되어 있지 않은가"를 재는 값이다.

```kotlin
fun countInversions(nums: IntArray): Int
```

같은 값은 역순이 아니다. 두 겹으로 세면 O(n^2) 이고, 병합 정렬로 세면 O(n log n) 이다 —
병합할 때 오른쪽 값이 먼저 나오면, 왼쪽에 남은 원소 수만큼 역순 쌍이 한꺼번에 생긴다.

## 실행 리플레이 계측 (선택)

풀이 과정을 시각적으로 되짚고 싶으면 `Drill` SDK 를 호출한다. 채점 실행에서는
no-op 으로 컴파일되므로 시간·메모리 판정에 영향을 주지 않는다.

```kotlin
Drill.compare(left, right)  // 두 쪽의 앞을 견줬다
Drill.write(index, value)   // 병합 결과에 썼다
Drill.call("sort")          // 재귀로 내려갔다
```

## 제약

- `1 <= nums.size <= 120_000`
- `-10^9 <= nums[i] <= 10^9`
- 정답은 `Int` 범위를 넘지 않는다
