# 정렬된 배열에서 개수 세기

오름차순(같은 값이 여럿일 수 있다) 배열 `nums` 와 `targets` 가 주어진다. `targets[j]` 마다 그 값이 `nums` 에 몇 번
나오는지 담은 배열을 반환한다.

```kotlin
fun countInSorted(nums: IntArray, targets: IntArray): IntArray
```

같은 값은 붙어 있다. 그 값이 처음 나오는 자리와, 그 값보다 큰 값이 처음 나오는 자리를 이분 탐색으로 찾으면 둘의 차가
개수다. 값이 없으면 두 자리가 같아 0 이다. 하나 찾은 뒤 양옆으로 세어 나가면 같은 값이 많을 때 느리다.

## 실행 리플레이 계측 (선택)

풀이 과정을 시각적으로 되짚고 싶으면 `Drill` SDK 를 호출한다. 채점 실행에서는
no-op 으로 컴파일되므로 시간·메모리 판정에 영향을 주지 않는다.

```kotlin
Drill.pointer("low", low)     // 탐색 구간
Drill.pointer("high", high)
```

## 제약

- `1 <= nums.size, targets.size <= 200_000`
- `-10^9 <= nums[i], targets[j] <= 10^9`, `nums` 는 오름차순
