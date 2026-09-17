# 회의실 예약

`src/booking/RoomBooking.kt` 의 `RoomBooking` 을 완성한다. 회의실 하나의 하루 예약을 관리한다.
시각은 분 단위의 정수이고 예약은 **반열린 구간** `[start, end)` 다 — `[10, 20)` 과 `[20, 30)` 은
겹치지 않는다. 모델(`Booking`·`Slot`·예외)은 `src/booking/Model.kt` 에 있고 고치지 않는다.

## 요구사항

### `RoomBooking(openAt, closeAt)`

- 여는 시각과 닫는 시각. `openAt < closeAt` 이 아니면 `IllegalArgumentException`.

### `book(id, start, end): Booking`

- `start < end` 이고 `openAt <= start`, `end <= closeAt` 이어야 한다. 아니면 `IllegalArgumentException`.
- 같은 `id` 가 이미 있으면 `DuplicateBookingException`.
- 기존 예약과 겹치면 `ConflictException` — 예외의 `withId` 는 부딪힌 예약의 id 다. 겹침은
  반열린 구간의 겹침이다: 끝과 시작이 같은 두 예약은 겹치지 않는다.
- 검사 순서는 인자 → 중복 id → 겹침이다. 성공하면 만든 `Booking` 을 돌려준다.

### `move(id, start, end): Booking`

- 예약을 새 시각으로 옮긴다. 없는 id 면 `NoSuchBookingException`.
- 인자 검사와 겹침 검사는 `book` 과 같되, **자기 자신과는 부딪히지 않는다** — `[10, 20)` 을
  `[15, 25)` 로 옮기는 것은 다른 예약이 없으면 된다.
- 어떤 이유로든 실패하면 **원래 예약이 그대로 남는다**.

### `cancel(id): Boolean`

있었으면 지우고 `true`, 없었으면 `false`.

### `bookings(): List<Booking>`

모든 예약을 **시작 시각 순**으로. 넣은 순서와 무관하다.

### `free(): List<Slot>`

영업 시간 안에서 예약이 없는 구간들을 시작 순으로. 예약이 없으면 `[openAt, closeAt)` 하나,
빈 구간(길이 0)은 넣지 않는다 — 예약이 여는 시각에 시작하면 그 앞의 구간은 없다.

## 제출

`src/booking/RoomBooking.kt` 를 고친다. `tests/` 아래의 파일은 채점 때 숨은 스위트로 덮인다.
테스트는 `import codedrill.*` 의 `assertEquals`·`assertTrue`·`assertFalse`·`assertNull`·`assertThrows<T>`
를 쓴다 — 표준 라이브러리 외의 의존성은 없다. 테스트는 이름이 `Test` 로 끝나는 클래스의 `test…`
메서드다.
