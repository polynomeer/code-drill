# 정렬 배열에서 삽입 위치

오름차순으로 정렬된 배열 `nums` 와 정수 `target` 이 주어진다. `target` 이 들어갈
자리의 인덱스를 반환한다. 이미 있으면 **가장 왼쪽** 자리다.

```kotlin
fun insertPosition(nums: IntArray, target: Int): Int
```

같은 값이 여러 개일 때 어느 자리를 답으로 볼지가 이 문제의 전부다. "찾으면 바로
반환"하는 이분 탐색은 가운데 어딘가를 돌려주므로 답이 흔들린다.

## 실행 리플레이 계측 (선택)

풀이 과정을 시각적으로 되짚고 싶으면 `Drill` SDK 를 호출한다. 채점 실행에서는
no-op 으로 컴파일되므로 시간·메모리 판정에 영향을 주지 않는다.

```kotlin
Drill.pointer("low", low)    // 후보 구간의 왼쪽
Drill.pointer("high", high)  // 후보 구간의 오른쪽
Drill.visit(mid, value)      // 가운데를 봤다
```

## 제약

- `1 <= nums.size <= 200_000`
- `-10^9 <= nums[i], target <= 10^9`, `nums` 는 오름차순
