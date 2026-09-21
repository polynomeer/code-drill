# 주차장

`src/parking/ParkingLot.java` 의 `ParkingLot` 을 완성한다. 자리는 `SMALL`·`MEDIUM`·`LARGE` 세 크기이고
차도 세 크기다. 시각은 **분 단위의 정수**(`long`)다. `Ticket`·`VehicleSize`·예외는 `src/parking/` 의
다른 파일에 있고 고치지 않는다.

## 요구사항

### `ParkingLot(small, medium, large)`

각 크기의 자리 수. 음수면 `IllegalArgumentException`.

### `Ticket park(plate, size, enteredAt)`

- 차는 **자기 크기 이상**의 자리에 댈 수 있다. 빈 자리 중 **가장 작은 크기**에 댄다 — `SMALL` 차는
  `SMALL` 이 비어 있으면 거기, 아니면 `MEDIUM`, 아니면 `LARGE`.
- 댈 자리가 없으면 `LotFullException`. 같은 번호판이 이미 주차 중이면 `AlreadyParkedException`
  (자리 유무보다 먼저 본다).
- 돌려주는 `Ticket` 은 `id`(주차장 안에서 유일한 문자열), `plate`, 댄 자리의 `spot` 크기, `enteredAt` 이다.

### `long leave(ticketId, leftAt)`

- 모르는 `id` 거나 이미 나간 표면 `UnknownTicketException`. `leftAt < enteredAt` 이면
  `IllegalArgumentException` 이고 차는 그대로 있다.
- 요금을 돌려주고 자리를 비운다. 요금은 **댄 자리의 크기**로 정한다: 시간당 `SMALL 1000`,
  `MEDIUM 2000`, `LARGE 3000`. 주차 시간은 `leftAt - enteredAt` 분이고, **30분 이하면 0 원**, 넘으면
  **시작한 시간 단위**로 센다 — 31분은 1시간, 60분은 1시간, 61분은 2시간.

### `int free(size)`

그 크기의 빈 자리 수.

### `List<String> parked()`

주차 중인 번호판을 오름차순으로.

## 제출

`src/parking/ParkingLot.java` 를 고친다. `tests/` 아래의 파일은 채점 때 숨은 스위트로 덮인다. 테스트는
`import static codedrill.Assertions.*;` 의 `assertEquals`·`assertTrue`·`assertFalse`·`assertNull`·
`assertThrows` 를 쓴다. 테스트는 이름이 `Test` 로 끝나는 클래스의 `public void test…()` 메서드다.
