# kind: WRONG_BRANCH
# 범위 밖 삽입도 다시 할 것을 지운 뒤에 예외를 던진다. 예외 자체는 맞다.


class Editor:
    def __init__(self, initial: str = "") -> None:
        self._text = initial
        # (kind, position, text). 되돌릴 것과 다시 할 것을 스택 둘로 든다.
        self._undo: list[tuple[str, int, str]] = []
        self._redo: list[tuple[str, int, str]] = []

    @property
    def text(self) -> str:
        return self._text

    def insert(self, position: int, text: str) -> None:
        self._redo.clear()
        if position < 0 or position > len(self._text):
            raise IndexError(f"position {position} out of range")
        if not text:
            return
        self._apply(("insert", position, text))

    def delete(self, position: int, length: int) -> str:
        if position < 0 or length < 1 or position + length > len(self._text):
            raise IndexError(f"delete({position}, {length}) out of range")
        removed = self._text[position:position + length]
        self._apply(("delete", position, removed))
        self._redo.clear()
        return removed

    def undo(self) -> bool:
        if not self._undo:
            return False
        edit = self._undo.pop()
        self._revert(edit)
        self._redo.append(edit)
        return True

    def redo(self) -> bool:
        if not self._redo:
            return False
        edit = self._redo.pop()
        self._apply(edit)
        return True

    def history(self) -> list[str]:
        return [f"{kind}@{position}:{text!r}" for kind, position, text in self._undo]

    def _apply(self, edit: tuple[str, int, str]) -> None:
        kind, position, text = edit
        if kind == "insert":
            self._text = self._text[:position] + text + self._text[position:]
        else:
            self._text = self._text[:position] + self._text[position + len(text):]
        self._undo.append(edit)

    def _revert(self, edit: tuple[str, int, str]) -> None:
        kind, position, text = edit
        if kind == "insert":
            self._text = self._text[:position] + self._text[position + len(text):]
        else:
            self._text = self._text[:position] + text + self._text[position:]
