"""토큰 버킷 — 골격. 시그니처는 그대로 두고 본문을 채운다."""

from collections.abc import Callable


class InvalidCost(ValueError):
    """요청 비용이 1 미만이거나 버킷 용량보다 크다."""


class RateLimiter:
    def __init__(self, capacity: int, refill_per_second: float, clock: Callable[[], float]) -> None:
        raise NotImplementedError

    def allow(self, key: str, cost: int = 1) -> bool:
        raise NotImplementedError

    def remaining(self, key: str) -> int:
        raise NotImplementedError

    def reset(self, key: str) -> None:
        raise NotImplementedError
