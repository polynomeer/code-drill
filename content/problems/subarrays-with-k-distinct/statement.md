# 서로 다른 값이 정확히 k 개인 구간의 수

정수 배열 `nums` 와 `k` 가 주어진다. 서로 다른 값이 **정확히** `k` 개인 연속 부분 배열의 수를 반환한다.

```kotlin
fun subarraysWithKDistinct(nums: IntArray, k: Int): Int
```

"정확히 k" 는 창으로 바로 세기 어렵다 — 오른쪽을 늘리면 구간이 조건을 벗어났다가 다시 들어오기 때문이다.
"**k 개 이하**" 는 창으로 센다: 오른쪽마다 종류가 `k` 를 넘지 않을 때까지 왼쪽을 줄이면 그 창의 모든
끝이 답이라 `right − left + 1` 을 더한다. 정확히 `k` 는 `이하 k − 이하 k−1` 이다. 답은 `Int` 를 넘지
않지만 n(n+1)/2 는 조심해서 본다 — n ≤ 2·10⁴ 면 2·10⁸ 이다.

## 실행 리플레이 계측 (선택)

풀이 과정을 시각적으로 되짚고 싶으면 `Drill` SDK 를 호출한다. 채점 실행에서는
no-op 으로 컴파일되므로 시간·메모리 판정에 영향을 주지 않는다.

```kotlin
Drill.compare(left, right)    // 창을 봤다
Drill.write(0, total)         // 세었다
```

## 제약

- `1 <= nums.length <= 60_000` (답은 `Int` 안이다)
- `1 <= nums[i] <= nums.length`, `1 <= k <= nums.length`
