# 세 판 병합

`src/merge/ThreeWayMerge.java` 의 `merge` 를 완성한다. 공통 조상 `base` 와 거기서 갈라진 두 판
`ours`·`theirs` 를 **줄 단위로** 병합해 `MergeResult` 를 돌려준다. `MergeResult` 는
`src/merge/MergeResult.java` 에 있고 고치지 않는다.

버전 관리 도구가 `git merge` 에서 하는 일과 같은 것이다. 세 판을 나란히 놓고, 양쪽이 **같은 자리를
서로 다르게** 고쳤을 때만 사람을 부른다.

## 요구사항

### `MergeResult merge(base, ours, theirs)`

세 목록은 각각 한 판의 줄들이다. 결과의 `lines()` 는 병합된 줄들이고, `conflicted()` 는 충돌 블록이
**하나라도** 있었는가다.

세 판을 공통 조상 기준으로 맞추면, 서로 어긋나는 구간이 생긴다. 구간마다 이 순서로 정한다.

1. **양쪽이 같다** — `ours` 와 `theirs` 의 구간이 똑같으면 그것을 **한 번만** 쓴다. 둘이 같은 수정을
   했거나, 둘 다 손대지 않은 경우다.
2. **한쪽만 바뀌었다** — 한쪽 구간이 `base` 와 같으면 바뀐 쪽을 쓴다. 지운 것도 바뀐 것이다.
3. **둘 다 다르게 바뀌었다** — 충돌이다. 아래 모양으로 적고 `conflicted()` 는 `true` 가 된다.

한쪽이 지우고 한쪽이 고쳤으면 3번이다 — "지움"과 "고침"은 서로 다른 변경이다.

### 충돌 표시

```
<<<<<<< ours
(ours 의 구간, 없을 수 있다)
=======
(theirs 의 구간, 없을 수 있다)
>>>>>>> theirs
```

세 표시 줄은 `ThreeWayMerge` 의 `OURS_MARKER`·`SPLIT_MARKER`·`THEIRS_MARKER` 상수이고 고치지 않는다.
**`ours` 가 먼저다.** 구간이 빈 쪽은 표시 줄만 남는다.

### 그 밖의 규칙

- 세 인자 중 하나라도 `null` 이면 `IllegalArgumentException`.
- 빈 목록은 정상이다. 셋 다 비면 결과도 비고 충돌은 없다.
- 결과의 `lines()` 는 바뀌지 않는 목록이어야 한다 — 호출한 쪽이 고칠 수 없다.
- 줄은 있는 그대로 비교한다. 공백과 대소문자도 다른 줄이다.

## 크기

각 판은 1000 줄까지다.

## 제출

`src/merge/ThreeWayMerge.java` 를 고친다. `tests/` 아래의 파일은 채점 때 숨은 스위트로 덮인다. 테스트는
`import static codedrill.Assertions.*;` 의 `assertEquals`·`assertTrue`·`assertFalse`·`assertNull`·
`assertThrows` 를 쓴다. 테스트는 이름이 `Test` 로 끝나는 클래스의 `public void test…()` 메서드다.
