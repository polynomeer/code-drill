# 가장 긴 공통 접두사

문자열 배열 `words` 가 주어진다. 모든 문자열이 공통으로 갖는 가장 긴 **접두사**를
반환한다. 없으면 빈 문자열이다.

```kotlin
fun commonPrefix(words: Array<String>): String
```

빈 문자열이 하나라도 있으면 공통 접두사도 빈 문자열이다.

## 실행 리플레이 계측 (선택)

풀이 과정을 시각적으로 되짚고 싶으면 `Drill` SDK 를 호출한다. 채점 실행에서는
no-op 으로 컴파일되므로 시간·메모리 판정에 영향을 주지 않는다.

```kotlin
Drill.visit(index, 0)   // 몇 번째 글자를 보고 있는지
Drill.compare(a, b)     // 두 단어의 같은 자리를 견줬다
```

## 제약

- `1 <= words.size <= 10_000`
- `0 <= words[i].length <= 1_000`
