# 구간의 비트 AND

`left <= right` 인 두 정수가 주어진다. `left` 부터 `right` 까지 모든 정수의 비트 AND 를 반환한다.

```kotlin
fun bitwiseAndOfRange(left: Int, right: Int): Int
```

구간을 다 곱하면 20 억 번이다. 어떤 비트 자리가 구간 안에서 한 번이라도 0 이면 결과의 그 자리는 0
이고, 연속한 정수는 낮은 자리부터 뒤집히니 `left` 와 `right` 가 갈리는 자리 아래는 전부 0 이 된다.
답은 두 수의 **공통 이진 접두사**다 — 두 수가 같아질 때까지 오른쪽으로 밀고, 민 만큼 되돌린다.

## 실행 리플레이 계측 (선택)

풀이 과정을 시각적으로 되짚고 싶으면 `Drill` SDK 를 호출한다. 채점 실행에서는
no-op 으로 컴파일되므로 시간·메모리 판정에 영향을 주지 않는다.

```kotlin
Drill.compare(left, right)    // 두 수를 비교했다
Drill.write(0, shift)         // 한 자리 밀었다
```

## 제약

- `0 <= left <= right <= 2^31 - 1`
