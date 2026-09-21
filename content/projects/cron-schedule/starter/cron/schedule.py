"""분·시 일정식 — 골격. 시그니처는 그대로 두고 본문을 채운다."""


class Schedule:
    def __init__(self, minutes: set[int], hours: set[int]) -> None:
        self.minutes = minutes
        self.hours = hours

    def matches(self, minute_of_day: int) -> bool:
        raise NotImplementedError

    def runs_in_day(self) -> list[int]:
        raise NotImplementedError

    def next_run(self, after: int) -> int:
        raise NotImplementedError


def parse(expr: str) -> Schedule:
    raise NotImplementedError
