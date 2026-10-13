# kind: MISSING_EDGE_CASE
# 음수를 거부하지 않는다. 음수 기간은 ValueError 다.
"""기간 표기. `1d2h3m4s` 꼴을 초로 읽고, 초를 같은 꼴로 쓴다."""

import re

_UNITS = (("d", 86_400), ("h", 3_600), ("m", 60), ("s", 1))
_ORDER = {name: i for i, (name, _) in enumerate(_UNITS)}
# 공백으로 나눈 조각 하나 — 숫자와 단위가 붙은 부분이 하나 이상
_PIECE = re.compile(r"(?:[0-9]+[a-z])+")
_PART = re.compile(r"([0-9]+)([a-z])")


def parse_duration(text: str) -> int:
    pieces = text.split()
    if not pieces:
        raise ValueError("empty duration")
    for piece in pieces:
        # 숫자와 단위 사이의 공백, 단위 없는 수, 부호, 소수점은 여기서 걸린다
        if not _PIECE.fullmatch(piece):
            raise ValueError(f"bad duration: {text!r}")
    total = 0
    next_unit = 0  # 다음에 올 수 있는 가장 큰 단위 — 단위는 큰 것부터, 한 번씩만
    for number, unit in _PART.findall("".join(pieces)):
        order = _ORDER.get(unit)
        if order is None or order < next_unit:
            raise ValueError(f"bad duration: {text!r}")
        total += int(number) * _UNITS[order][1]
        next_unit = order + 1
    return total


def format_duration(seconds: int) -> str:
    if seconds == 0:
        return "0s"
    out = []
    for name, size in _UNITS:
        count, seconds = divmod(seconds, size)
        if count:
            out.append(f"{count}{name}")
    return "".join(out)
