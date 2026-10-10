# 부등식을 만족하는 쌍

길이가 같은 정수 배열 `nums1`, `nums2` 와 정수 `diff` 가 주어진다. `i < j` 이고
`nums1[i] - nums1[j] <= nums2[i] - nums2[j] + diff` 인 쌍 `(i, j)` 의 수를 `1_000_000_007` 로 나눈 나머지로 반환한다.

```kotlin
fun inequalityPairs(nums1: IntArray, nums2: IntArray, diff: Int): Int
```

`d[i] = nums1[i] - nums2[i]` 로 두면 조건은 `d[i] <= d[j] + diff` 다 — 한 배열 안에서 "앞의 값이 뒤의 값 + diff 이하"인 쌍을
세는 문제가 된다. 뒤집힌 쌍 세기와 같은 모양이라 병합 정렬로 센다: 왼쪽 반과 오른쪽 반을 각각 정렬해 두면, 오른쪽의 각
`d[j]` 에 대해 `d[j] + diff` 이하인 왼쪽 원소의 수는 포인터 하나가 앞으로만 움직이며 센다. 그다음 두 반을 합친다.

쌍의 수는 1 250 억까지 간다. 세는 동안은 `Long` 으로 들고 마지막에 나머지를 취한다.

## 실행 리플레이 계측 (선택)

풀이 과정을 시각적으로 되짚고 싶으면 `Drill` SDK 를 호출한다. 채점 실행에서는
no-op 으로 컴파일되므로 시간·메모리 판정에 영향을 주지 않는다.

```kotlin
Drill.call("lo..hi")          // 이 구간을 정렬하며 센다
Drill.ret("lo..hi", count)    // 두 반을 가로지르는 쌍의 수
```

## 제약

- `1 <= nums1.size == nums2.size <= 500_000`
- `-10^4 <= nums1[i], nums2[i], diff <= 10^4`
