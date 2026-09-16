# 만료가 있는 LRU 캐시

`src/cache/TtlCache.kt` 의 `TtlCache<K, V>` 를 완성한다. 용량이 정해져 있고, 항목마다 만료
시각이 있으며, 가득 차면 **가장 오래 쓰지 않은** 항목을 내보낸다. 시간은 `System.currentTimeMillis()`
가 아니라 **생성자로 받은 `clock: () -> Long`** 에서만 읽는다 — 테스트가 시계를 돌리며 검사한다.

## 요구사항

### `TtlCache(capacity, ttlMillis, clock)`

- `capacity` 는 1 이상, `ttlMillis` 는 1 이상. 아니면 `IllegalArgumentException`.

### `put(key, value)`

- 항목의 만료 시각은 `지금 + ttlMillis` 다. 이미 있는 키를 다시 넣으면 값·만료 시각·최근 사용
  순서가 모두 새로워진다.
- 새 키를 넣을 자리가 없으면 **먼저 만료된 항목들을 지우고**, 그래도 가득 차 있으면 가장
  오래 쓰지 않은 항목 하나를 내보낸다. 만료된 항목은 용량을 차지하지 않는다.

### `get(key): V?`

- 없거나 **만료됐으면** `null`. 만료된 항목은 이때 지운다. 시각 `t` 에 항목은 `t >= 만료 시각`
  이면 만료다 — 경계에서 만료다.
- 있으면 값을 돌려주고 그 항목이 **가장 최근에 쓴 것**이 된다. 조회가 순서를 바꾼다.

### `remove(key): Boolean`

있었으면(만료 여부와 관계없이) 지우고 `true`, 없었으면 `false`.

### `size: Int`

지금 시각에 **만료되지 않은** 항목 수. 만료된 것은 세기 전에 지운다.

### `keys(): List<K>`

만료되지 않은 키를 **가장 오래 쓰지 않은 것부터** 가장 최근 것까지.

## 제출

`src/cache/TtlCache.kt` 를 고친다. `tests/` 아래의 파일은 채점 때 숨은 스위트로 덮인다. 테스트는
`import codedrill.*` 의 `assertEquals`·`assertTrue`·`assertNull`·`assertThrows<T>` 를 쓴다 — 표준
라이브러리 외의 의존성은 없다. 테스트는 이름이 `Test` 로 끝나는 클래스의 `test…` 메서드다.
