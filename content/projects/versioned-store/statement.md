# 버전이 있는 키-값 저장소

`src/store/VersionedStore.kt` 의 `VersionedStore` 를 완성한다. 쓰기(넣기·지우기·되감기)마다
저장소 전체의 **버전**이 1 씩 오르고, 어느 키든 **지난 버전의 값**을 다시 읽을 수 있다. 값은
`String` 이다. 예외는 `src/store/Model.kt` 에 있고 고치지 않는다.

## 요구사항

### `version: Int`

지금 버전. 빈 저장소는 `0` 이고, 쓰기가 성공할 때마다 1 오른다. 실패한 쓰기는 버전을 올리지 않는다.

### `put(key, value): Int`

값을 적고 새 버전을 돌려준다. **같은 값을 다시 적어도** 새 버전이다 — 쓰기는 쓰기다.

### `delete(key): Int`

키를 지우고 새 버전을 돌려준다. 없는 키(지금 버전에 값이 없는 키)면 `NoSuchKeyException` 이고 버전은
그대로다. 지운 뒤 `get` 은 `null` 이지만 **지우기 전 버전으로 `getAt` 하면 그 값이 그대로 나온다** —
지우기는 이력을 없애는 것이 아니라 이력에 "없음"을 적는 것이다.

### `get(key): String?`

지금 버전의 값. 없으면 `null`.

### `getAt(key, version): String?`

`version` 시점의 값 — 그 키에 대해 `version` **이하**의 가장 최근 쓰기가 정한 값이다. 그 시점에
값이 없었으면 `null`. `version` 이 `0` 미만이거나 지금 버전보다 크면 `IllegalArgumentException`.

### `rollback(version): Int`

저장소 전체를 `version` 시점의 상태로 되감고 **새 버전**을 돌려준다 — 되감기는 이력을 자르는 것이
아니라 그 시점의 상태를 다시 적는 쓰기 한 번이다. 되감은 뒤에도 그 사이의 버전들은 `getAt` 으로
읽힌다. 범위는 `getAt` 과 같고, 범위 밖이면 `IllegalArgumentException` 이고 버전은 그대로다.

### `keys(): List<String>`

지금 버전에 값이 있는 키를 오름차순으로.

## 제출

`src/store/VersionedStore.kt` 를 고친다. `tests/` 아래의 파일은 채점 때 숨은 스위트로 덮인다.
테스트는 `import codedrill.*` 의 `assertEquals`·`assertTrue`·`assertFalse`·`assertNull`·`assertThrows<T>`
를 쓴다 — 표준 라이브러리 외의 의존성은 없다. 테스트는 이름이 `Test` 로 끝나는 클래스의 `test…`
메서드다. **테스트를 더 쓰면** 그 테스트가 대표 오답을 잡는지도 잰다.
