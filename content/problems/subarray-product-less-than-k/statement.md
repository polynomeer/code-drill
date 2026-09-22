# 곱이 k 미만인 구간의 수

양의 정수 배열 `nums` 와 `k` 가 주어진다. 원소의 곱이 `k` **미만**인 연속 부분 배열의 수를 반환한다.

```kotlin
fun subarrayProductLessThanK(nums: IntArray, k: Int): Int
```

값이 전부 양수라 곱은 오른쪽을 늘리면 커지고 왼쪽을 줄이면 작아진다 — 창이 된다. 오른쪽마다 곱이
`k` 이상인 동안 왼쪽을 줄이고 `right − left + 1` 을 더한다. 곱은 창 안에서만 유지되어 `k · 최댓값`
을 넘지 않지만 그것이 이미 `Int` 를 넘는다 — `Long` 이다. `k ≤ 1` 이면 답은 0 이다(곱은 1 이상).

## 실행 리플레이 계측 (선택)

풀이 과정을 시각적으로 되짚고 싶으면 `Drill` SDK 를 호출한다. 채점 실행에서는
no-op 으로 컴파일되므로 시간·메모리 판정에 영향을 주지 않는다.

```kotlin
Drill.compare(left, right)    // 창을 봤다
Drill.write(0, total)         // 세었다
```

## 제약

- `1 <= nums.length <= 60_000`, `1 <= nums[i] <= 1000` (답은 `Int` 안이다: n(n+1)/2 < 2³¹)
- `0 <= k <= 10^9`
