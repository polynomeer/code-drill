# 되돌리기가 있는 편집기

한 줄짜리 텍스트 버퍼 `editor/buffer.py` 를 완성한다. 삽입과 삭제를 할 수 있고, 되돌리기(undo)와
다시 하기(redo)가 있다.

## 요구사항

### `Editor(initial="")`

`text` 속성이 지금의 내용이다. 읽기만 한다.

### `insert(position, text) -> None`

- `position` 이 0 미만이거나 지금 길이보다 크면 `IndexError`. 버퍼는 바뀌지 않는다.
- `text` 가 빈 문자열이면 **아무 일도 하지 않는다** — 이력에도 남지 않는다.
- 그 자리에 끼워 넣는다. `position == len(text)` 는 끝에 붙이는 것이다.

### `delete(position, length) -> str`

- `position` 이 범위 밖이거나 `length` 가 1 미만이거나 `position + length` 가 길이를 넘으면
  `IndexError`. 버퍼는 바뀌지 않는다 — 일부만 지우는 일은 없다.
- 지운 문자열을 돌려준다.

### `undo() -> bool`, `redo() -> bool`

- 되돌릴 것이 있으면 마지막 편집을 원래대로 돌리고 `True`, 없으면 `False`.
- 되돌린 편집은 `redo` 로 다시 적용한다 — 같은 자리에 같은 내용이다.
- **새 편집(`insert`·`delete`)이 일어나면 다시 할 것들은 사라진다.** 그 뒤의 `redo` 는 `False` 다.
- `undo` 와 `redo` 를 여러 번 번갈아 해도 내용이 정확해야 한다.

### `history() -> list[str]`

지금 되돌릴 수 있는 편집들을 **오래된 것부터** `"insert@3:'abc'"`, `"delete@0:'x'"` 모양으로.
되돌린 편집은 여기 없다. 반환한 목록을 바꿔도 편집기는 바뀌지 않는다.

## 제출

`editor/buffer.py` 를 고친다. `tests/` 아래의 파일은 채점 때 숨은 스위트로 덮인다.
