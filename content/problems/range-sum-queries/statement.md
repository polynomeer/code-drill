# 구간 합 질의

정수 배열 `nums` 와 질의 `[l1, r1, l2, r2, ...]` 가 주어진다. 각 질의에 대해 `nums[l..r]`
(양 끝 포함)의 합을 담아 반환한다.

```kotlin
fun rangeSums(nums: IntArray, queries: IntArray): IntArray
```

질의마다 더하면 O(n) 씩이다. 누적합을 한 번 만들어 두면 질의는 뺄셈 한 번이다.
`prefix[r + 1] - prefix[l]` — 인덱스가 하나 어긋나기 쉬운 자리다.

## 실행 리플레이 계측 (선택)

풀이 과정을 시각적으로 되짚고 싶으면 `Drill` SDK 를 호출한다. 채점 실행에서는
no-op 으로 컴파일되므로 시간·메모리 판정에 영향을 주지 않는다.

```kotlin
Drill.write(i, prefix)         // 누적합을 채웠다
Drill.compare(l, r)            // 질의 구간을 봤다
```

## 제약

- `1 <= nums.size <= 200_000`, `-10^4 <= nums[i] <= 10^4`
- `2 <= queries.size <= 400_000`, 짝수, `0 <= l <= r < nums.size`
