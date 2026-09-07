# 정렬된 두 수의 합

**오름차순으로 정렬된** 정수 배열 `nums` 와 정수 `target` 이 주어진다. 더해서 `target`
이 되는 두 원소의 인덱스를 오름차순으로 담아 반환한다.

- 정답은 항상 정확히 하나 존재한다.
- 같은 원소를 두 번 쓸 수 없다.

```kotlin
fun pairSum(nums: IntArray, target: Int): IntArray
```

정렬되어 있다는 사실이 핵심이다. 해시맵으로도 풀리지만, 그러면 정렬이라는 조건을 쓰지
않은 것이다.

## 실행 리플레이 계측 (선택)

풀이 과정을 시각적으로 되짚고 싶으면 `Drill` SDK 를 호출한다. 채점 실행에서는
no-op 으로 컴파일되므로 시간·메모리 판정에 영향을 주지 않는다.

```kotlin
Drill.pointer("left", left)    // 왼쪽 포인터를 옮겼다
Drill.pointer("right", right)  // 오른쪽 포인터를 옮겼다
Drill.compare(left, right)     // 두 끝의 합을 봤다
Drill.match(left, right)       // 답을 찾았다
```

## 제약

- `2 <= nums.size <= 100_000`
- `-10^9 <= nums[i] <= 10^9`, 오름차순 정렬
- 두 원소의 합은 `Int` 범위를 넘지 않는다
