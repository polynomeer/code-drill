# kind: MISSING_EDGE_CASE
# 그날 안에 없으면 다음 날로 넘기지 않고 첫 실행 시각을 그대로 돌려준다.

from cron import InvalidExpression

DAY = 1440


class Schedule:
    def __init__(self, minutes: set[int], hours: set[int]) -> None:
        self.minutes = minutes
        self.hours = hours

    def matches(self, minute_of_day: int) -> bool:
        if not 0 <= minute_of_day < DAY:
            raise ValueError(f"minute of day out of range: {minute_of_day}")
        return minute_of_day // 60 in self.hours and minute_of_day % 60 in self.minutes

    def runs_in_day(self) -> list[int]:
        return sorted(hour * 60 + minute for hour in self.hours for minute in self.minutes)

    def next_run(self, after: int) -> int:
        if not 0 <= after < DAY:
            raise ValueError(f"minute of day out of range: {after}")
        runs = self.runs_in_day()
        for run in runs:
            if run > after:
                return run
        return runs[0]


def _field(text: str, limit: int) -> set[int]:
    values: set[int] = set()
    for item in text.split(","):
        if item == "":
            raise InvalidExpression(f"empty item in {text!r}")
        base, _, step_text = item.partition("/")
        step = 1
        if step_text or item.endswith("/"):
            step = _number(step_text, limit)
            if step < 1:
                raise InvalidExpression(f"step must be positive: {item}")
        if base == "*":
            lo, hi = 0, limit - 1
        elif "-" in base:
            lo_text, _, hi_text = base.partition("-")
            lo, hi = _number(lo_text, limit), _number(hi_text, limit)
            if lo > hi:
                raise InvalidExpression(f"reversed range: {item}")
        else:
            if step_text or item.endswith("/"):
                raise InvalidExpression(f"a single value cannot have a step: {item}")
            lo = hi = _number(base, limit)
        if not (0 <= lo < limit and 0 <= hi < limit):
            raise InvalidExpression(f"out of range: {item}")
        values.update(range(lo, hi + 1, step))
    return values


def _number(text: str, limit: int) -> int:
    if not text.isdigit():
        raise InvalidExpression(f"not a number: {text!r}")
    return int(text)


def parse(expr: str) -> Schedule:
    parts = expr.split(" ")
    if len(parts) != 2:
        raise InvalidExpression(f"expected two fields: {expr!r}")
    return Schedule(_field(parts[0], 60), _field(parts[1], 24))
