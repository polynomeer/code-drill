# 경로 라우터

`src/router/Router.java` 의 `Router` 를 완성한다. 경로 패턴을 등록해 두고 요청 경로가 오면 어느
핸들러인지와 매개변수를 돌려준다. `Match` 와 예외는 `src/router/` 의 다른 파일에 있고 고치지
않는다.

## 패턴

패턴은 `/` 로 시작하고 `/` 로 나뉜 조각들이다. 조각은 **글자 조각**(`users`) 또는
**매개변수 조각**(`{id}` — 중괄호 안이 이름)이다. `/` 하나만 있는 패턴은 조각이 없는 루트다.

- `/` 로 시작하지 않거나, 빈 조각이 있거나(`/a//b`), 중괄호가 짝이 맞지 않거나 이름이 비었거나
  (`{`, `{}`), 같은 이름의 매개변수가 두 번 나오면 `IllegalArgumentException`.
- 패턴 끝의 `/` 하나는 없는 것과 같다 — `/users/` 와 `/users` 는 같은 패턴이다.

## 요구사항

### `add(pattern, handler)`

- `handler` 가 `null` 이거나 비었으면 `IllegalArgumentException`.
- 이미 **같은 모양**의 패턴이 있으면 `DuplicateRouteException` — 매개변수 이름만 다른
  `/users/{id}` 와 `/users/{name}` 은 같은 모양이다.

### `match(path): Match`

- `path` 가 `/` 로 시작하지 않으면 `IllegalArgumentException`.
- `?` 부터의 질의 문자열은 무시하고, 끝의 `/` 하나도 무시한다.
- 조각 수가 같고 글자 조각은 정확히 같으며 매개변수 조각에는 비어 있지 않은 조각이 오면 맞는다.
  `/users/{id}` 는 `/users/1/posts` 와 맞지 않고(조각 수), `/users//` 와도 맞지 않는다(빈 조각).
- 둘 이상 맞으면 **왼쪽부터 처음으로 종류가 다른 조각에서 글자 조각인 쪽**이 이긴다. 등록
  순서는 상관없다. `/users/me` 와 `/users/{id}` 가 모두 있으면 `/users/me` 는 앞의 것이다.
- 맞는 것이 없으면 `null`. 맞으면 `Match(handler, params)` — `params` 는 매개변수 이름 → 조각.
  매개변수가 없으면 빈 맵이다.

### `remove(pattern): boolean`

그 패턴(끝의 `/` 무시)이 있었으면 지우고 `true`, 없었으면 `false`.

### `routes(): List<String>`

등록된 패턴을 **등록한 순서**로, 끝의 `/` 를 뗀 모양으로.

## 제출

`src/router/Router.java` 를 고친다. `tests/` 아래의 파일은 채점 때 숨은 스위트로 덮인다.
테스트는 `import static codedrill.Assertions.*;` 의 `assertEquals`·`assertTrue`·`assertFalse`·
`assertNull`·`assertThrows` 를 쓴다 — 표준 라이브러리 외의 의존성은 없다. 테스트는 이름이
`Test` 로 끝나는 클래스의 `public void test…()` 메서드다.
