# kind: WRONG_ALGORITHM
# 거절해도 비용을 뺀다. 거절 자체는 맞아서 remaining 을 보지 않으면 안 드러난다.

from collections.abc import Callable


class InvalidCost(ValueError):
    """요청 비용이 1 미만이거나 버킷 용량보다 크다."""


class RateLimiter:
    def __init__(self, capacity: int, refill_per_second: float, clock: Callable[[], float]) -> None:
        if capacity < 1 or refill_per_second <= 0:
            raise ValueError(f"capacity={capacity}, refill_per_second={refill_per_second}")
        self._capacity = capacity
        self._rate = refill_per_second
        self._clock = clock
        # key -> [tokens, last_seen]
        self._buckets: dict[str, list[float]] = {}

    def _bucket(self, key: str) -> list[float]:
        now = self._clock()
        bucket = self._buckets.get(key)
        if bucket is None:
            bucket = [float(self._capacity), now]
            self._buckets[key] = bucket
            return bucket
        elapsed = now - bucket[1]
        if elapsed > 0:
            bucket[0] = min(float(self._capacity), bucket[0] + elapsed * self._rate)
        bucket[1] = now
        return bucket

    def allow(self, key: str, cost: int = 1) -> bool:
        if cost < 1 or cost > self._capacity:
            raise InvalidCost(f"cost={cost}")
        bucket = self._bucket(key)
        allowed = bucket[0] >= cost
        bucket[0] = max(0.0, bucket[0] - cost)
        return allowed

    def remaining(self, key: str) -> int:
        return int(self._bucket(key)[0])

    def reset(self, key: str) -> None:
        self._buckets[key] = [float(self._capacity), self._clock()]
