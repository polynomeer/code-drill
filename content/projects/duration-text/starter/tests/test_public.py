"""공개 테스트. 채점은 이것과 숨은 테스트를 함께 돌린다."""

import unittest

from durations import format_duration, parse_duration


class PublicTests(unittest.TestCase):
    def test_parse_hours_and_minutes(self):
        self.assertEqual(5400, parse_duration("1h30m"))

    def test_parse_every_unit(self):
        self.assertEqual(93784, parse_duration("1d2h3m4s"))

    def test_format_largest_first(self):
        self.assertEqual("1h1m1s", format_duration(3661))

    def test_rejects_unknown_unit(self):
        with self.assertRaises(ValueError):
            parse_duration("3w")


if __name__ == "__main__":
    unittest.main()
