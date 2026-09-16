# kind: MISSING_EDGE_CASE
# 시계가 뒤로 가면 마지막 시각을 갱신하지 않는다. 시계가 돌아오는 순간 그동안의 시간이 한꺼번에 채워진다.

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
        if bucket[0] >= cost:
            bucket[0] -= cost
            return True
        return False

    def remaining(self, key: str) -> int:
        return int(self._bucket(key)[0])

    def reset(self, key: str) -> None:
        self._buckets[key] = [float(self._capacity), self._clock()]
