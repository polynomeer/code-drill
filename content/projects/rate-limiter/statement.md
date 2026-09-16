# 토큰 버킷 요청 제한기

API 서버 앞에 두는 요청 제한기 `limiter/bucket.py` 를 완성한다. 키(사용자·IP)마다 **토큰
버킷** 하나를 두고, 요청은 토큰을 써서 통과한다. 시간은 `time.time()` 이 아니라 **생성자로
받은 시계 함수**에서만 읽는다 — 테스트가 시간을 앞으로 돌리며 검사한다.

## 요구사항

### `RateLimiter(capacity, refill_per_second, clock)`

- `capacity` 는 버킷의 최대 토큰 수(1 이상), `refill_per_second` 는 초당 채워지는 토큰 수(0 보다 큰
  실수), `clock` 은 현재 시각(초, 실수)을 돌려주는 함수다. 범위 밖이면 `ValueError`.
- 처음 보는 키의 버킷은 **가득 찬 채로** 시작한다.

### `allow(key, cost=1) -> bool`

- `cost` 가 1 미만이거나 `capacity` 보다 크면 `InvalidCost` 를 던진다 — 어떤 버킷에도 손대지 않는다.
- 먼저 마지막으로 본 시각부터 지금까지 지난 시간만큼 토큰을 채운다. 채우는 양은
  `지난 초 × refill_per_second` 이고 **실수로 누적**한다. 버킷은 `capacity` 를 넘지 않는다.
- 토큰이 `cost` 이상이면 그만큼 빼고 `True`, 아니면 **아무것도 빼지 않고** `False`.
- 시계가 뒤로 가면(지난 시간이 음수) 채우지 않는다. 마지막 시각은 늘 지금으로 갱신한다.

### `remaining(key) -> int`

지금 시각으로 채운 뒤의 토큰을 **내림**한 정수. 처음 보는 키는 `capacity`. 조회도 버킷을
채운다 — 그 뒤의 `allow` 가 같은 값을 보게.

### `reset(key) -> None`

그 키의 버킷을 가득 채운다. 다른 키는 그대로다.

## 제출

`limiter/bucket.py` 를 고친다. `tests/` 아래의 파일은 채점 때 숨은 스위트로 덮인다.
