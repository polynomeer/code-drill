"""공개 테스트. 채점은 이것과 숨은 테스트를 함께 돌린다."""

import unittest

from limiter import InvalidCost, RateLimiter


class FakeClock:
    def __init__(self, now: float = 0.0) -> None:
        self.now = now

    def __call__(self) -> float:
        return self.now


class PublicTests(unittest.TestCase):
    def test_bucket_starts_full_and_drains(self):
        clock = FakeClock()
        limiter = RateLimiter(capacity=3, refill_per_second=1, clock=clock)
        self.assertEqual([True, True, True, False], [limiter.allow("a") for _ in range(4)])
        self.assertEqual(0, limiter.remaining("a"))

    def test_refills_over_time(self):
        clock = FakeClock()
        limiter = RateLimiter(capacity=2, refill_per_second=1, clock=clock)
        limiter.allow("a")
        limiter.allow("a")
        clock.now = 1.0
        self.assertTrue(limiter.allow("a"))
        self.assertFalse(limiter.allow("a"))

    def test_invalid_cost(self):
        limiter = RateLimiter(capacity=2, refill_per_second=1, clock=FakeClock())
        with self.assertRaises(InvalidCost):
            limiter.allow("a", cost=0)
        with self.assertRaises(InvalidCost):
            limiter.allow("a", cost=3)
