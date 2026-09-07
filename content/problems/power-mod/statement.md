# 거듭제곱의 나머지

정수 `base` 와 `exponent` 가 주어진다. `base^exponent` 를 `1_000_000_007` 로 나눈
나머지를 반환한다.

```kotlin
fun powerMod(base: Int, exponent: Int): Int
```

지수가 크므로 곱셈을 지수 번 하면 끝나지 않는다. 지수를 반으로 접어 가면 `log`
번이면 된다.

곱하는 중간값이 `Int` 를 훌쩍 넘으므로 `Long` 으로 계산해야 한다.

## 실행 리플레이 계측 (선택)

풀이 과정을 시각적으로 되짚고 싶으면 `Drill` SDK 를 호출한다. 채점 실행에서는
no-op 으로 컴파일되므로 시간·메모리 판정에 영향을 주지 않는다.

```kotlin
Drill.write(0, result)  // 지금까지의 결과
Drill.visit(bit, 1)     // 지수의 이 비트가 켜져 있었다
```

## 제약

- `0 <= base <= 1_000_000`
- `0 <= exponent <= 1_000_000_000`
