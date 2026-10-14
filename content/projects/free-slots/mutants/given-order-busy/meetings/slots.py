# kind: WRONG_ALGORITHM
# 바쁜 시간을 정렬하지 않고 주어진 순서대로 훑는다. 여러 사람의 바쁜 시간은 섞여 있다.
"""함께 비는 시간. 모든 시각을 UTC 의 하루 [0, 1440) 분으로 바꾼 뒤, 근무 시간의 교집합에서 바쁜 시간의 합집합을 뺀다."""

import re

_TIME = re.compile(r"([0-9]{2}):([0-9]{2})")
_OFFSET = re.compile(r"([+-])([0-9]{2}):([0-9]{2})")
DAY = 24 * 60


def _minutes(text: str) -> int:
    match = _TIME.fullmatch(text) if isinstance(text, str) else None
    if not match:
        raise ValueError(f"bad time: {text!r}")
    hours, minutes = int(match.group(1)), int(match.group(2))
    if minutes >= 60 or hours > 24 or (hours == 24 and minutes != 0):
        raise ValueError(f"bad time: {text!r}")
    return hours * 60 + minutes


def _offset(text: str) -> int:
    match = _OFFSET.fullmatch(text) if isinstance(text, str) else None
    if not match:
        raise ValueError(f"bad offset: {text!r}")
    sign, hours, minutes = match.group(1), int(match.group(2)), int(match.group(3))
    if hours > 14 or minutes not in (0, 15, 30, 45):
        raise ValueError(f"bad offset: {text!r}")
    value = hours * 60 + minutes
    return value if sign == "+" else -value


def _interval(pair, offset):
    start, end = _minutes(pair[0]), _minutes(pair[1])
    if start >= end:
        raise ValueError(f"empty or reversed interval: {pair!r}")
    # 현지 시각 − 오프셋 = UTC. 하루 밖으로 나간 부분은 버린다.
    return max(0, start - offset), min(DAY, end - offset)


def _text(minute: int) -> str:
    return f"{minute // 60:02d}:{minute % 60:02d}"


def free_slots(people: list, duration: int) -> list:
    if duration <= 0:
        raise ValueError("duration must be positive")
    if not people:
        raise ValueError("no people")
    # 모두가 일하는 시간: 근무 시간들의 교집합
    low, high = 0, DAY
    busy = []
    for person in people:
        offset = _offset(person["offset"])
        start, end = _interval(person["hours"], offset)
        low, high = max(low, start), min(high, end)
        for pair in person.get("busy", []):
            b_start, b_end = _interval(pair, offset)
            if b_start < b_end:
                busy.append((b_start, b_end))
    if low >= high:
        return []
    # 바쁜 시간을 정렬해 겹치거나 맞닿은 것을 합치며, 그 사이의 빈 시간을 모은다
    out = []
    cursor = low
    for b_start, b_end in busy:
        if b_end <= cursor:
            continue
        if b_start >= high:
            break
        if b_start - cursor >= duration:
            out.append((_text(cursor), _text(b_start)))
        cursor = max(cursor, b_end)
        if cursor >= high:
            break
    if high - cursor >= duration:
        out.append((_text(cursor), _text(high)))
    return out
