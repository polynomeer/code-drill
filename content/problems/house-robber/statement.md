# 이웃하지 않게 고르기

집마다 든 금액이 배열 `values` 로 주어진다. **이웃한 두 집을 함께 고를 수 없을 때**
얻을 수 있는 최대 금액을 반환한다.

```kotlin
fun rob(values: IntArray): Int
```

"한 집 건너 전부 고르기"는 답이 아니다. `[2, 1, 1, 2]` 에서 답은 양 끝의 `4` 다.

## 실행 리플레이 계측 (선택)

풀이 과정을 시각적으로 되짚고 싶으면 `Drill` SDK 를 호출한다. 채점 실행에서는
no-op 으로 컴파일되므로 시간·메모리 판정에 영향을 주지 않는다.

```kotlin
Drill.visit(index, value)  // 집을 봤다
Drill.write(index, best)   // 이 집까지의 최선
```

## 제약

- `1 <= values.size <= 200_000`
- `0 <= values[i] <= 10_000`
