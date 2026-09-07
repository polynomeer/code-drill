# 최장 증가 부분 수열의 길이

정수 배열 `nums` 에서 **엄격히 증가하는** 부분 수열 중 가장 긴 것의 길이를 반환한다.
부분 수열은 연속하지 않아도 되지만 순서는 유지해야 한다.

```kotlin
fun longestIncreasing(nums: IntArray): Int
```

`O(n^2)` DP 로도 풀리지만 큰 입력에서는 무너진다. 길이별로 "가능한 가장 작은 끝값"을
들고 이분 탐색으로 갱신하면 `O(n log n)` 이다.

## 실행 리플레이 계측 (선택)

풀이 과정을 시각적으로 되짚고 싶으면 `Drill` SDK 를 호출한다. 채점 실행에서는
no-op 으로 컴파일되므로 시간·메모리 판정에 영향을 주지 않는다.

```kotlin
Drill.visit(index, value)  // 원소를 봤다
Drill.write(pos, value)    // 길이 pos+1 의 가장 작은 끝값을 갱신했다
```

## 제약

- `1 <= nums.size <= 200_000`
- `-10^9 <= nums[i] <= 10^9`
