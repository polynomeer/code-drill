"""숨은 테스트. 사용자에게 나가지 않는다."""

import unittest

from cron import InvalidExpression, parse


class Steps(unittest.TestCase):
    def test_range_step_counts_from_range_start(self):
        self.assertEqual([5, 11, 17], sorted(parse("5-20/6 0").minutes))

    def test_star_step_counts_from_zero(self):
        self.assertEqual([0, 7, 14, 21], sorted(parse("0 */7").hours))

    def test_single_value_with_step_is_invalid(self):
        with self.assertRaises(InvalidExpression):
            parse("5/10 *")


class Lists(unittest.TestCase):
    def test_duplicates_collapse(self):
        self.assertEqual([1, 60 + 1], parse("1,1 0,1,0").runs_in_day())

    def test_mixed_items(self):
        self.assertEqual({0, 30, 45, 46, 47}, parse("0,30,45-47 *").minutes)

    def test_empty_item_is_invalid(self):
        for bad in ("1,,2 *", ", *", "* 1,"):
            with self.assertRaises(InvalidExpression):
                parse(bad)


class Validation(unittest.TestCase):
    def test_reversed_range_is_invalid(self):
        with self.assertRaises(InvalidExpression):
            parse("20-5 *")

    def test_out_of_range_and_bad_step(self):
        for bad in ("* 24", "-1 *", "*/0 *", "1-5/0 *", "a *", "* *  ", "5"):
            with self.assertRaises(InvalidExpression):
                parse(bad)

    def test_matches_rejects_out_of_day(self):
        schedule = parse("* *")
        with self.assertRaises(ValueError):
            schedule.matches(1440)
        with self.assertRaises(ValueError):
            schedule.matches(-1)


class NextRun(unittest.TestCase):
    def test_strictly_after(self):
        schedule = parse("0 *")
        self.assertEqual(10 * 60, schedule.next_run(9 * 60))

    def test_wraps_to_next_day(self):
        schedule = parse("30 9")
        self.assertEqual(1440 + 9 * 60 + 30, schedule.next_run(9 * 60 + 30))
        self.assertEqual(1440 + 9 * 60 + 30, schedule.next_run(23 * 60 + 59))

    def test_after_range(self):
        schedule = parse("* *")
        with self.assertRaises(ValueError):
            schedule.next_run(1440)
        self.assertEqual(1, schedule.next_run(0))

    def test_next_within_hour_then_next_hour(self):
        schedule = parse("10,50 8-9")
        self.assertEqual(8 * 60 + 50, schedule.next_run(8 * 60 + 10))
        self.assertEqual(9 * 60 + 10, schedule.next_run(8 * 60 + 50))
        self.assertEqual(1440 + 8 * 60 + 10, schedule.next_run(9 * 60 + 50))
