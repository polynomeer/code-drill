# 두 수열의 편집 거리

정수 수열 `source` 를 `target` 으로 바꾸는 데 필요한 최소 연산 수를 반환한다. 연산은
세 가지다.

- 원소 하나 **삽입**
- 원소 하나 **삭제**
- 원소 하나 **교체**

```kotlin
fun editDistance(source: IntArray, target: IntArray): Int
```

교체는 한 번의 연산이다. "삭제하고 삽입"으로 세면 두 번이 되어 답이 커진다.

## 실행 리플레이 계측 (선택)

풀이 과정을 시각적으로 되짚고 싶으면 `Drill` SDK 를 호출한다. 채점 실행에서는
no-op 으로 컴파일되므로 시간·메모리 판정에 영향을 주지 않는다.

```kotlin
Drill.compare(i, j)      // 두 원소를 견줬다
Drill.write(j, distance) // 그 자리까지의 최소 연산 수
```

## 제약

- `0 <= source.size, target.size <= 1_000`
- `-10^9 <= 원소 <= 10^9`
