# 두 수의 합

정수 배열 `nums` 와 정수 `target` 이 주어진다. 더해서 `target` 이 되는 두 원소의
**인덱스**를 오름차순으로 담은 배열을 반환한다.

- 정답은 항상 정확히 하나 존재한다.
- 같은 원소를 두 번 쓸 수 없다.
- 반환하는 두 인덱스는 오름차순이어야 한다.

```kotlin
fun twoSum(nums: IntArray, target: Int): IntArray
```

## 실행 리플레이 계측 (선택)

풀이 과정을 시각적으로 되짚고 싶으면 `Drill` SDK 를 호출한다. 채점 실행에서는
no-op 으로 컴파일되므로 시간·메모리 판정에 영향을 주지 않는다.

```kotlin
Drill.visit(index, value)     // 원소를 살펴봤다
Drill.compare(index, other)   // 짝을 찾아봤다
Drill.match(left, right)      // 답을 찾았다
```

## 제약

- `2 <= nums.size <= 100_000`
- `-10^9 <= nums[i] <= 10^9`
- `-10^9 <= target <= 10^9`

## 예제

| nums | target | 반환 |
|---|---|---|
| `[2, 7, 11, 15]` | `9` | `[0, 1]` |
| `[3, 2, 4]` | `6` | `[1, 2]` |
