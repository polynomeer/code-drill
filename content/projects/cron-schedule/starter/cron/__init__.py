"""분·시 일정식. 요구사항은 문제 본문에 있다."""


class InvalidExpression(ValueError):
    """일정식의 모양이나 값이 틀렸다."""


from .schedule import Schedule, parse  # noqa: E402 — 예외가 먼저 정의돼야 schedule 이 들여온다

__all__ = ["InvalidExpression", "Schedule", "parse"]
