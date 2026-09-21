# 순위표

`src/board/Leaderboard.java` 의 `Leaderboard` 를 완성한다. 선수의 점수를 더하고 순위를 묻는다.
`Standing` 과 예외는 `src/board/` 의 다른 파일에 있고 고치지 않는다.

## 요구사항

### `addScore(player, points)`

점수를 **누적**한다 — 처음 보는 선수는 0 에서 시작한다. `points` 는 음수여도 되지만 누적 점수가
0 아래로 내려가면 `IllegalArgumentException` 이고 아무것도 바뀌지 않는다. `player` 가 `null` 이거나
비었으면 `IllegalArgumentException`.

### `int rank(player)`

**경쟁 순위**: 점수가 더 높은 선수의 수 + 1. 같은 점수는 같은 순위이고 그다음 순위는 그만큼 건너뛴다 —
점수가 `10, 8, 8, 5` 면 순위는 `1, 2, 2, 4`. 없는 선수면 `NoSuchPlayerException`.

### `List<Standing> top(k)`

상위 `k` 명을 순서대로. 점수 내림차순, 같은 점수는 **이름 오름차순**. `k` 가 선수 수보다 크면 전부,
`k < 0` 이면 `IllegalArgumentException`. `Standing` 은 `(rank, player, score)` 이고 `rank` 는 위의
경쟁 순위다 — 같은 점수는 같은 `rank` 를 단다.

### `void reset(player)`

그 선수의 점수를 **0 으로** 만든다 — 순위표에서 빼는 것이 아니다. 없는 선수면 `NoSuchPlayerException`.

### `int size()`

선수 수.

## 제출

`src/board/Leaderboard.java` 를 고친다. `tests/` 아래의 파일은 채점 때 숨은 스위트로 덮인다. 테스트는
`import static codedrill.Assertions.*;` 의 `assertEquals`·`assertTrue`·`assertFalse`·`assertNull`·
`assertThrows` 를 쓴다. 테스트는 이름이 `Test` 로 끝나는 클래스의 `public void test…()` 메서드다.
