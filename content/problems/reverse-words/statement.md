# 단어 순서와 글자 뒤집기

문자열 배열 `words` 가 주어진다. **배열의 순서를 뒤집고, 각 단어의 글자도 뒤집어**
반환한다.

```kotlin
fun reverseWords(words: Array<String>): Array<String>
```

두 가지를 모두 뒤집어야 한다. 하나만 뒤집은 답은 짧은 예제에서 우연히 맞을 수 있다.

## 실행 리플레이 계측 (선택)

풀이 과정을 시각적으로 되짚고 싶으면 `Drill` SDK 를 호출한다. 채점 실행에서는
no-op 으로 컴파일되므로 시간·메모리 판정에 영향을 주지 않는다.

```kotlin
Drill.visit(index, 0)      // 단어를 봤다
Drill.swap(left, right)    // 자리를 바꿨다
```

## 제약

- `1 <= words.size <= 10_000`
- `0 <= words[i].length <= 1_000`
