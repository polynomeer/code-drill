# 알파벳별 등장 횟수

문자열 `text` 가 주어진다. `a` 부터 `z` 까지 각 글자가 몇 번 나오는지를 담은 **길이 26
배열**을 반환한다. 대소문자는 구분하지 않고, 알파벳이 아닌 문자는 세지 않는다.

```kotlin
fun countLetters(text: String): IntArray
```

## 실행 리플레이 계측 (선택)

풀이 과정을 시각적으로 되짚고 싶으면 `Drill` SDK 를 호출한다. 채점 실행에서는
no-op 으로 컴파일되므로 시간·메모리 판정에 영향을 주지 않는다.

```kotlin
Drill.visit(index, 0)      // 글자를 봤다
Drill.write(slot, count)   // 그 글자의 개수를 올렸다
```

## 제약

- `0 <= text.length <= 200_000`
- 임의의 유니코드 문자가 올 수 있다
