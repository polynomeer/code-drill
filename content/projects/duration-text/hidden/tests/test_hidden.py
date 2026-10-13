"""숨은 테스트. 사용자에게 나가지 않는다."""

import unittest

from durations import format_duration, parse_duration


class Parsing(unittest.TestCase):
    def test_single_units(self):
        self.assertEqual(86_400, parse_duration("1d"))
        self.assertEqual(45, parse_duration("45s"))

    def test_zero(self):
        self.assertEqual(0, parse_duration("0s"))
        self.assertEqual(0, parse_duration("0h0m"))

    def test_spaces_between_parts(self):
        self.assertEqual(5400, parse_duration("1h 30m"))
        self.assertEqual(86_404, parse_duration("  1d   4s "))

    def test_leading_zeros_and_big_numbers(self):
        self.assertEqual(300, parse_duration("05m"))
        self.assertEqual(90, parse_duration("90s"))
        self.assertEqual(7_200, parse_duration("120m"))

    def test_skipped_units_are_fine(self):
        self.assertEqual(86_405, parse_duration("1d5s"))


class Rejecting(unittest.TestCase):
    def assert_rejects(self, *texts):
        for text in texts:
            with self.assertRaises(ValueError, msg=repr(text)):
                parse_duration(text)

    def test_empty(self):
        self.assert_rejects("", "   ")

    def test_out_of_order(self):
        self.assert_rejects("30m1h", "1s1d", "1m 1h")

    def test_repeated_unit(self):
        self.assert_rejects("1h1h", "1h 2h", "5m3m")

    def test_bare_number(self):
        self.assert_rejects("90", "1h30")

    def test_space_inside_a_part(self):
        self.assert_rejects("1 h", "1h 30 m")

    def test_signs_and_fractions(self):
        self.assert_rejects("-5s", "+5s", "1.5h")

    def test_case_and_garbage(self):
        self.assert_rejects("1H", "h", "1h,30m", "1h x")


class Formatting(unittest.TestCase):
    def test_zero(self):
        self.assertEqual("0s", format_duration(0))

    def test_skips_zero_parts(self):
        self.assertEqual("1d", format_duration(86_400))
        self.assertEqual("1h1s", format_duration(3_601))
        self.assertEqual("1d5s", format_duration(86_405))

    def test_carries_into_larger_units(self):
        self.assertEqual("1m30s", format_duration(90))
        self.assertEqual("2h", format_duration(7_200))

    def test_many_days(self):
        self.assertEqual("400d1s", format_duration(400 * 86_400 + 1))

    def test_rejects_negative(self):
        with self.assertRaises(ValueError):
            format_duration(-1)

    def test_round_trip(self):
        for seconds in (0, 1, 59, 60, 61, 3_599, 3_600, 86_399, 86_400, 93_784, 1_000_000):
            self.assertEqual(seconds, parse_duration(format_duration(seconds)), msg=seconds)


if __name__ == "__main__":
    unittest.main()
