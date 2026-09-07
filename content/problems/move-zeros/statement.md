# 0을 뒤로 밀기

정수 배열 `nums` 에서 모든 `0` 을 배열의 끝으로 옮긴 결과를 반환한다.

- **0 이 아닌 원소의 상대 순서는 그대로여야 한다.**
- 배열의 길이는 바뀌지 않는다.

```kotlin
fun moveZeros(nums: IntArray): IntArray
```

"0 을 지우고 뒤에 붙인다"가 아니라 "쓰기 위치를 따로 들고 훑는다"로 풀면 추가 배열
없이 한 번에 끝난다.

## 실행 리플레이 계측 (선택)

풀이 과정을 시각적으로 되짚고 싶으면 `Drill` SDK 를 호출한다. 채점 실행에서는
no-op 으로 컴파일되므로 시간·메모리 판정에 영향을 주지 않는다.

```kotlin
Drill.visit(index, value)   // 원소를 읽었다
Drill.write(index, value)   // 쓰기 위치에 값을 넣었다
Drill.pointer("write", i)   // 쓰기 위치를 옮겼다
```

## 제약

- `1 <= nums.size <= 100_000`
- `-10^9 <= nums[i] <= 10^9`
