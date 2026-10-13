# 최솟값과 최댓값이 정해진 구간

정수 배열 `nums` 와 `minK <= maxK` 가 주어진다. **최솟값이 정확히 `minK` 이고 최댓값이 정확히 `maxK`** 인 연속 구간의 수를
`1_000_000_007` 로 나눈 나머지로 반환한다.

```kotlin
fun fixedBoundSubarrays(nums: IntArray, minK: Int, maxK: Int): Int
```

구간의 오른쪽 끝 `i` 를 옮기며, 그 끝을 갖는 좋은 구간의 수를 더한다. 세 자리를 기억한다: 범위 `[minK, maxK]` 를 벗어난 마지막
자리 `bad`, `minK` 가 마지막으로 나온 자리, `maxK` 가 마지막으로 나온 자리. 왼쪽 끝은 `bad` 보다 오른쪽이고, `minK` 와 `maxK`
가 모두 들어오려면 두 마지막 자리 가운데 앞선 것 이하여야 한다. 그러니 `max(0, min(마지막 minK, 마지막 maxK) − bad)` 개다 —
`bad` 뒤에 둘 중 하나가 아직 없으면 0 이다.

`minK == maxK` 면 그 값만으로 된 구간이다. 수는 200 억까지 가므로 `Long` 으로 센다.

## 실행 리플레이 계측 (선택)

풀이 과정을 시각적으로 되짚고 싶으면 `Drill` SDK 를 호출한다. 채점 실행에서는
no-op 으로 컴파일되므로 시간·메모리 판정에 영향을 주지 않는다.

```kotlin
Drill.pointer("bad", bad)   // 범위를 벗어난 마지막 자리
```

## 제약

- `2 <= nums.size <= 200_000`
- `1 <= nums[i], minK, maxK <= 10^6`, `minK <= maxK`
