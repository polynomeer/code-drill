"""숨은 테스트. 사용자에게 나가지 않는다."""

import unittest

from limiter import InvalidCost, RateLimiter


class FakeClock:
    def __init__(self, now: float = 0.0) -> None:
        self.now = now

    def __call__(self) -> float:
        return self.now


class Refill(unittest.TestCase):
    def test_fractional_refill_accumulates(self):
        clock = FakeClock()
        limiter = RateLimiter(capacity=5, refill_per_second=0.5, clock=clock)
        for _ in range(5):
            limiter.allow("a")
        clock.now = 1.0  # 0.5 토큰 — 아직 하나가 안 된다
        self.assertFalse(limiter.allow("a"))
        clock.now = 2.0  # 누적 1.0
        self.assertTrue(limiter.allow("a"))

    def test_refill_is_capped_at_capacity(self):
        clock = FakeClock()
        limiter = RateLimiter(capacity=2, refill_per_second=10, clock=clock)
        # 비운 뒤 오래 쉰다. 처음 보는 키는 어차피 가득 차 있어 상한을 시험하지 못한다.
        limiter.allow("a", cost=2)
        clock.now = 100.0
        self.assertEqual(2, limiter.remaining("a"))
        self.assertEqual([True, True, False], [limiter.allow("a") for _ in range(3)])

    def test_sub_second_elapsed_counts(self):
        clock = FakeClock()
        limiter = RateLimiter(capacity=5, refill_per_second=2, clock=clock)
        limiter.allow("a", cost=5)
        clock.now = 0.5  # 1 토큰. 지난 시간을 정수로 내리면 0 이다.
        self.assertTrue(limiter.allow("a"))
        self.assertFalse(limiter.allow("a"))

    def test_denied_request_does_not_consume(self):
        clock = FakeClock()
        limiter = RateLimiter(capacity=3, refill_per_second=1, clock=clock)
        limiter.allow("a")
        self.assertFalse(limiter.allow("a", cost=3))
        self.assertEqual(2, limiter.remaining("a"))
        self.assertTrue(limiter.allow("a", cost=2))

    def test_clock_going_backwards_does_not_refill(self):
        clock = FakeClock(now=10.0)
        limiter = RateLimiter(capacity=1, refill_per_second=1, clock=clock)
        limiter.allow("a")
        clock.now = 5.0
        self.assertFalse(limiter.allow("a"))
        clock.now = 6.0  # 마지막 시각(5.0)부터 1초
        self.assertTrue(limiter.allow("a"))


class Keys(unittest.TestCase):
    def test_keys_have_independent_buckets(self):
        limiter = RateLimiter(capacity=1, refill_per_second=1, clock=FakeClock())
        self.assertTrue(limiter.allow("a"))
        self.assertTrue(limiter.allow("b"))
        self.assertFalse(limiter.allow("a"))

    def test_reset_fills_only_that_key(self):
        limiter = RateLimiter(capacity=2, refill_per_second=1, clock=FakeClock())
        limiter.allow("a", cost=2)
        limiter.allow("b", cost=2)
        limiter.reset("a")
        self.assertEqual(2, limiter.remaining("a"))
        self.assertEqual(0, limiter.remaining("b"))

    def test_remaining_floors_and_refills(self):
        clock = FakeClock()
        limiter = RateLimiter(capacity=4, refill_per_second=0.75, clock=clock)
        limiter.allow("a", cost=4)
        clock.now = 2.0  # 1.5 토큰
        self.assertEqual(1, limiter.remaining("a"))
        self.assertTrue(limiter.allow("a"))
        self.assertFalse(limiter.allow("a"))


class Validation(unittest.TestCase):
    def test_invalid_cost_touches_nothing(self):
        limiter = RateLimiter(capacity=2, refill_per_second=1, clock=FakeClock())
        with self.assertRaises(InvalidCost):
            limiter.allow("a", cost=5)
        self.assertEqual(2, limiter.remaining("a"))

    def test_constructor_rejects_bad_parameters(self):
        with self.assertRaises(ValueError):
            RateLimiter(capacity=0, refill_per_second=1, clock=FakeClock())
        with self.assertRaises(ValueError):
            RateLimiter(capacity=1, refill_per_second=0, clock=FakeClock())
