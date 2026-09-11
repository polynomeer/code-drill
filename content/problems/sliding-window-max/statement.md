# 창마다 최댓값

정수 배열 `nums` 와 창 크기 `k` 가 주어진다. 왼쪽부터 한 칸씩 미는 크기 `k` 의 창마다
그 안의 **최댓값**을 담아 반환한다. 결과의 길이는 `nums.size - k + 1` 이다.

```kotlin
fun windowMax(nums: IntArray, k: Int): IntArray
```

창을 밀 때 "앞으로 최댓값이 될 가능성이 없는 원소"는 버려도 된다 — 자기보다 크고 더
나중에 들어온 원소가 있으면 그렇다. 남는 후보는 내림차순이고, 그것을 양끝에서 다루는
구조가 덱이다.

## 실행 리플레이 계측 (선택)

풀이 과정을 시각적으로 되짚고 싶으면 `Drill` SDK 를 호출한다. 채점 실행에서는
no-op 으로 컴파일되므로 시간·메모리 판정에 영향을 주지 않는다.

```kotlin
Drill.enqueue(value)           // 후보로 넣었다
Drill.dequeue(value)           // 창을 벗어나 버렸다
Drill.pop(value)               // 더 큰 값에 밀려 버렸다
Drill.write(i, max)            // i 번째 창의 답을 적었다
```

## 제약

- `1 <= k <= nums.size <= 200_000`
- `-10^9 <= nums[i] <= 10^9`
