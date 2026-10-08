# 줄 바꿈

글을 정해진 폭으로 줄 바꿈한다. `src/wrap/TextWrapper.kt` 의 `TextWrapper.wrap(text, width)` 를 완성한다.

## 낱말

공백 문자(빈칸·탭·줄바꿈·`\r`)가 낱말을 나눈다. 공백이 여러 개 이어져도 한 번 나눈 것과 같다.

## 문단

공백 문자만 있는 줄(빈 줄)이 문단을 나눈다. 빈 줄이 여러 개 이어져도 경계 하나다. 글의 앞뒤에 있는 빈 줄은 없는 것과 같다.
줄바꿈 하나만으로는 문단이 나뉘지 않는다 — 그 앞뒤의 낱말은 같은 문단이다.

## 줄 채우기

문단마다 낱말을 앞에서부터 한 줄에 들어가는 만큼 담는다. 한 줄의 낱말 사이는 빈칸 하나다. 줄의 앞뒤에 빈칸은 없다.

- 줄의 길이는 `width` **이하**다 — 꼭 같아도 된다.
- 낱말 하나가 `width` 보다 길면, 줄을 새로 시작해 `width` 글자씩 잘라 한 줄씩 차지한다. 잘라내고 남은 마지막 조각은 보통
  낱말처럼 다음 낱말과 같은 줄에 담길 수 있다.

## 결과

줄의 목록. 문단 사이에는 빈 문자열(`""`) 하나가 들어간다. 낱말이 하나도 없으면 빈 목록이다. `width` 가 1 보다 작으면
`IllegalArgumentException`.

```
wrap("the quick brown fox", 10) == ["the quick", "brown fox"]
wrap("abcdef gh", 5)           == ["abcde", "f gh"]      // 긴 낱말을 자르고 남은 조각에 다음 낱말이 붙는다
wrap("a\n\n\nb", 3)            == ["a", "", "b"]
```

## 제출

`src/wrap/TextWrapper.kt` 를 고친다. `tests/` 아래의 파일은 채점 때 숨은 스위트로 덮인다. 테스트는 `import codedrill.*`
의 `assertEquals`·`assertTrue`·`assertFalse`·`assertNull`·`assertThrows<T>` 를 쓴다 — 표준 라이브러리 외의 의존성은
없다. 테스트는 이름이 `Test` 로 끝나는 클래스의 `test…` 메서드다.
