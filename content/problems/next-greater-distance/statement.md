# 다음 더 큰 값까지의 거리

정수 배열 `values` 가 주어진다. 각 위치마다 **자기보다 큰 값이 처음 나오는 곳까지의
거리**를 담은 배열을 반환한다. 그런 값이 없으면 `0` 이다.

```kotlin
fun nextGreater(values: IntArray): IntArray
```

값이 같은 경우는 "더 크다"에 해당하지 않는다. 두 겹으로 돌면 O(n^2) 이고, 아직 답을
못 찾은 위치를 스택에 쌓아 두면 한 번 훑기로 끝난다.

## 실행 리플레이 계측 (선택)

풀이 과정을 시각적으로 되짚고 싶으면 `Drill` SDK 를 호출한다. 채점 실행에서는
no-op 으로 컴파일되므로 시간·메모리 판정에 영향을 주지 않는다.

```kotlin
Drill.visit(index, value)  // 새 값을 봤다
Drill.push(index)          // 답을 못 찾은 위치를 미뤄 뒀다
Drill.pop(index)           // 그 위치의 답을 찾았다
Drill.write(index, dist)   // 거리를 적었다
```

## 제약

- `1 <= values.size <= 200_000`
- `-10^9 <= values[i] <= 10^9`
