# CSV 읽고 쓰기

`src/csv/Csv.kt` 의 `parse` 와 `format` 을 완성한다. 규칙은 흔히 쓰는 CSV(RFC 4180)를 따르되 아래가
전부다. 예외는 `src/csv/Model.kt` 에 있고 고치지 않는다.

## 읽기 — `parse(text): List<List<String>>`

- 줄은 `\n` 또는 `\r\n` 으로 나뉜다. 마지막 줄 끝의 줄바꿈 하나는 없는 것과 같다. 빈 문자열은 빈 목록이다.
- 줄 안의 값은 `,` 로 나뉜다. `a,,b` 는 값 셋(`a`, 빈 값, `b`)이고 `a,` 는 값 둘(`a`, 빈 값)이다.
- 값이 `"` 로 시작하면 **따옴표 값**이다: 닫는 `"` 까지가 값이고 그 안의 `,` 와 줄바꿈은 값의 일부다.
  따옴표 안의 `""` 는 `"` 하나다. 닫는 `"` 뒤에는 `,` 나 줄 끝만 올 수 있다 — 다른 글자가 오면
  `MalformedCsv`. 닫는 `"` 없이 끝나도 `MalformedCsv`.
- 따옴표 값이 아닌 값 안에 `"` 가 있으면 `MalformedCsv`.
- 줄마다 값의 수가 달라도 된다. 빈 줄은 빈 값 하나짜리 줄이다.

## 쓰기 — `format(rows): String`

- 값을 `,` 로 잇고 줄을 `\n` 으로 잇는다. 마지막 줄 뒤에도 `\n` 하나.
- 값에 `,`·`"`·`\n`·`\r` 이 있으면 **그 값만** `"` 로 감싸고 안의 `"` 는 `""` 로 적는다. 없으면 감싸지
  않는다 — 빈 값도 감싸지 않는다.
- `rows` 가 비었으면 빈 문자열.

`parse(format(rows)) == rows` 가 늘 성립해야 한다.

## 제출

`src/csv/Csv.kt` 를 고친다. `tests/` 아래의 파일은 채점 때 숨은 스위트로 덮인다. 테스트는
`import codedrill.*` 의 `assertEquals`·`assertTrue`·`assertFalse`·`assertNull`·`assertThrows<T>` 를 쓴다.
테스트는 이름이 `Test` 로 끝나는 클래스의 `test…` 메서드다.
