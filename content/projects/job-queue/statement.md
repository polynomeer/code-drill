# 재시도가 있는 작업 큐

`src/queue/JobQueue.kt` 의 `JobQueue` 를 완성한다. 워커가 작업을 받아 처리하고 끝났다고 보고하는 큐다.
워커는 죽을 수 있으므로 **받아 간 작업은 시한부로 빌려 준다** — 시한 안에 보고가 없으면 다른 워커에게 다시
준다. `Lease` 는 `src/queue/Lease.kt` 에 있고 고치지 않는다.

시간은 언제나 인자로 들어온다. 안에서 시계를 읽지 않는다 — 그래야 시험이 결정적이다.

## 만들 때

`JobQueue(maxAttempts, visibilityMillis)`. `maxAttempts` 는 한 작업을 최대 몇 번 **줄** 수 있는가이고
`1` 이상이다. `visibilityMillis` 는 빌려 주는 시한이고 `0` 이상이다. 어기면 `IllegalArgumentException`.

## 요구사항

### `String submit(payload, now)`

작업을 대기열에 넣고 **작업 번호**를 돌려준다. 번호는 제출 순서대로 `job-1`, `job-2`, … 다.

### `Lease? poll(now)`

대기 중인 작업 하나를 빌려 준다. 없으면 `null`.

- **먼저 제출된 것부터** 준다. 실패해서 대기로 돌아온 작업도 제 자리는 제출 순서 그대로다.
- 준 횟수(`attempts`)가 1 늘고, **새 토큰**을 단다. 토큰은 `1` 부터 하나씩 늘어나는 수이고 큐 전체에서
  다시 쓰이지 않는다.
- 시한은 `now + visibilityMillis` 다.

### `ack(jobId, token, now)` / `nack(jobId, token, now)`

`ack` 는 끝났다는 보고이고 `nack` 는 실패했다는 보고다. 둘 다 **지금 빌려 준 상태**이고 **토큰이 같아야**
받아들인다. 아니면 `IllegalStateException` 이고 큐는 바뀌지 않는다. 없는 작업 번호는
`NoSuchElementException`.

`nack` 는 실패다 — 준 횟수가 `maxAttempts` 에 이르렀으면 **죽은 작업**이 되고, 아니면 대기로 돌아온다.

### 시한이 지나면

시한이 **지났거나 꼭 찬**(`시한 <= now`) 임대는 실패로 친다 — `nack` 와 같다. 회수는 큐를 들여다보는
모든 호출(`poll`·`ack`·`nack`·`pending`)에서 그 시점의 `now` 로 먼저 일어난다.

회수된 뒤에는 **옛 토큰이 쓸모없다.** 늦게 돌아온 워커의 `ack` 는 거절된다 — 그 사이 다른 워커가 같은
작업을 받았을 수 있기 때문이다.

### `int attempts(jobId)`

지금까지 그 작업을 준 횟수.

### `List<String> deadLetters()`

죽은 작업의 번호를 **제출 순서대로**.

### `int pending(now)`

아직 끝나지도 죽지도 않은 작업의 수 — 대기 중인 것과 빌려 준 것을 합친 수다.

## 제출

`src/queue/JobQueue.kt` 를 고친다. `tests/` 아래의 파일은 채점 때 숨은 스위트로 덮인다. 테스트는
`import codedrill.*` 의 `assertEquals`·`assertTrue`·`assertFalse`·`assertNull`·`assertThrows` 를 쓰고,
이름이 `Test` 로 끝나는 클래스의 `test…` 메서드다.
