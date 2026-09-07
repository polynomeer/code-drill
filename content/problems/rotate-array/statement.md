# 배열 오른쪽으로 회전

정수 배열 `nums` 를 오른쪽으로 `k` 칸 회전한 결과를 반환한다. 끝을 넘어간 원소는 앞으로
돌아온다.

```kotlin
fun rotate(nums: IntArray, k: Int): IntArray
```

`k` 가 배열 길이보다 클 수 있다. `k % n` 으로 줄이지 않으면 인덱스가 범위를 벗어난다.

## 실행 리플레이 계측 (선택)

풀이 과정을 시각적으로 되짚고 싶으면 `Drill` SDK 를 호출한다. 채점 실행에서는
no-op 으로 컴파일되므로 시간·메모리 판정에 영향을 주지 않는다.

```kotlin
Drill.visit(index, value)  // 원본에서 읽었다
Drill.write(target, value) // 회전한 자리에 썼다
```

## 제약

- `1 <= nums.size <= 200_000`
- `0 <= k <= 10^9`
- `-10^9 <= nums[i] <= 10^9`
