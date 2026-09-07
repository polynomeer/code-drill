# 홀로 남은 수

정수 배열 `nums` 에서 **한 값만 한 번 나오고 나머지는 모두 정확히 두 번** 나온다.
한 번만 나오는 값을 반환한다.

```kotlin
fun singleNumber(nums: IntArray): Int
```

추가 자료구조 없이 상수 메모리로 풀 수 있다. 같은 값을 두 번 XOR 하면 사라진다는
성질을 쓰면 순서와 무관하게 한 번 훑기로 끝난다.

## 실행 리플레이 계측 (선택)

풀이 과정을 시각적으로 되짚고 싶으면 `Drill` SDK 를 호출한다. 채점 실행에서는
no-op 으로 컴파일되므로 시간·메모리 판정에 영향을 주지 않는다.

```kotlin
Drill.visit(index, value)  // 원소를 봤다
Drill.write(0, acc)        // 누적 XOR
```

## 제약

- `nums.size` 는 홀수이며 `1 <= nums.size <= 200_001`
- `-10^9 <= nums[i] <= 10^9`
