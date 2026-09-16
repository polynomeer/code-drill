# 누적 합

정수 배열 `nums` 에 대해 `out[i] = nums[0] + ... + nums[i]` 인 배열을 반환한다.

예: `[1, 2, 3, 4]` → `[1, 3, 6, 10]`.

```kotlin
fun runningSum(nums: IntArray): IntArray
```

앞의 누적 합에 지금 값을 더하면 된다 — `out[i] = out[i-1] + nums[i]`. 매번 처음부터 다시
더하면 O(n²) 이고, 이 문제의 크기에서는 그것도 잡힌다.

## 실행 리플레이 계측 (선택)

풀이 과정을 시각적으로 되짚고 싶으면 `Drill` SDK 를 호출한다. 채점 실행에서는
no-op 으로 컴파일되므로 시간·메모리 판정에 영향을 주지 않는다.

```kotlin
Drill.visit(i, v)             // 원소를 봤다
Drill.write(i, total)         // 누적 합을 적었다
```

## 제약

- `1 <= nums.size <= 200_000`
- `-10^4 <= nums[i] <= 10^4` — 합은 `Int` 범위 안
