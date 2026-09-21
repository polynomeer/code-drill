"""공개 테스트. 채점은 이것과 숨은 테스트를 함께 돌린다."""

import unittest

from cron import InvalidExpression, parse


class PublicTests(unittest.TestCase):
    def test_every_fifteen_minutes(self):
        schedule = parse("*/15 *")
        self.assertTrue(schedule.matches(0))
        self.assertTrue(schedule.matches(9 * 60 + 45))
        self.assertFalse(schedule.matches(9 * 60 + 10))
        self.assertEqual(9 * 60 + 15, schedule.next_run(9 * 60 + 10))

    def test_runs_in_day(self):
        self.assertEqual([9 * 60 + 30, 17 * 60 + 30], parse("30 9,17").runs_in_day())

    def test_invalid(self):
        with self.assertRaises(InvalidExpression):
            parse("60 *")
        with self.assertRaises(InvalidExpression):
            parse("* * *")
