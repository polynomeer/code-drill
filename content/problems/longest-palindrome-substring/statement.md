# 가장 긴 회문 부분 문자열

문자열 `text` 가 주어진다. **연속한** 부분 문자열 중 회문인 것의 최대 길이를 반환한다.
빈 문자열이면 `0` 이다.

```kotlin
fun longestPalindrome(text: String): Int
```

회문은 중심에서 양쪽으로 자란다. 중심이 될 자리는 글자 n 개와 글자 사이 n-1 개, 2n-1 개다.
짝수 길이 회문의 중심을 빠뜨리는 것이 가장 흔한 실수다.

## 실행 리플레이 계측 (선택)

풀이 과정을 시각적으로 되짚고 싶으면 `Drill` SDK 를 호출한다. 채점 실행에서는
no-op 으로 컴파일되므로 시간·메모리 판정에 영향을 주지 않는다.

```kotlin
Drill.pointer("lo", lo)        // 왼쪽으로 넓혔다
Drill.pointer("hi", hi)        // 오른쪽으로 넓혔다
Drill.compare(lo, hi)          // 양끝 글자를 견줬다
Drill.match(lo, hi)            // 최댓값이 갱신됐다
```

## 제약

- `0 <= text.length <= 2_000`
- 영문 소문자
