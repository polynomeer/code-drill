"""작은 스프레드시트 — 골격. 시그니처는 그대로 두고 본문을 채운다."""

CYCLE = "#CYCLE!"
DIV0 = "#DIV/0!"


class Sheet:
    def set(self, cell: str, text: str) -> None:
        raise NotImplementedError

    def get(self, cell: str) -> int | str:
        raise NotImplementedError
