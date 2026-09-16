"""편집 버퍼 — 골격. 시그니처는 그대로 두고 본문을 채운다."""


class Editor:
    def __init__(self, initial: str = "") -> None:
        raise NotImplementedError

    @property
    def text(self) -> str:
        raise NotImplementedError

    def insert(self, position: int, text: str) -> None:
        raise NotImplementedError

    def delete(self, position: int, length: int) -> str:
        raise NotImplementedError

    def undo(self) -> bool:
        raise NotImplementedError

    def redo(self) -> bool:
        raise NotImplementedError

    def history(self) -> list[str]:
        raise NotImplementedError
