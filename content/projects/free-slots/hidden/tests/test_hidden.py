"""숨은 테스트. 사용자에게 나가지 않는다."""

import random
import unittest

from meetings import free_slots


def person(offset, hours, busy=()):
    return {"offset": offset, "hours": hours, "busy": list(busy)}


def utc(offset="+00:00", hours=("00:00", "24:00"), busy=()):
    return person(offset, hours, busy)


class Busy(unittest.TestCase):
    def test_overlapping_busy_merges(self):
        people = [utc(hours=("09:00", "12:00"), busy=[("09:30", "10:30"), ("10:00", "11:00")])]
        self.assertEqual([("09:00", "09:30"), ("11:00", "12:00")], free_slots(people, 15))

    def test_touching_busy_leaves_no_gap(self):
        people = [utc(hours=("09:00", "12:00"), busy=[("09:00", "10:00"), ("10:00", "12:00")])]
        self.assertEqual([], free_slots(people, 1))

    def test_unsorted_busy(self):
        people = [utc(hours=("09:00", "13:00"), busy=[("12:00", "12:30"), ("09:30", "10:00")])]
        self.assertEqual([("09:00", "09:30"), ("10:00", "12:00"), ("12:30", "13:00")], free_slots(people, 30))

    def test_nested_busy(self):
        people = [utc(hours=("09:00", "12:00"), busy=[("09:00", "11:00"), ("09:30", "10:00")])]
        self.assertEqual([("11:00", "12:00")], free_slots(people, 30))

    def test_busy_outside_hours_is_ignored(self):
        people = [utc(hours=("09:00", "10:00"), busy=[("07:00", "08:00"), ("10:00", "11:00")])]
        self.assertEqual([("09:00", "10:00")], free_slots(people, 60))

    def test_busy_of_everyone_counts(self):
        people = [utc(hours=("09:00", "12:00"), busy=[("09:00", "10:00")]),
                  utc(hours=("09:00", "12:00"), busy=[("11:00", "12:00")])]
        self.assertEqual([("10:00", "11:00")], free_slots(people, 60))


class Duration(unittest.TestCase):
    def test_exactly_long_enough(self):
        people = [utc(hours=("09:00", "09:30"))]
        self.assertEqual([("09:00", "09:30")], free_slots(people, 30))

    def test_one_minute_short(self):
        people = [utc(hours=("09:00", "09:29"))]
        self.assertEqual([], free_slots(people, 30))

    def test_rejects_non_positive(self):
        with self.assertRaises(ValueError):
            free_slots([utc()], 0)


class Offsets(unittest.TestCase):
    def test_positive_offset_moves_earlier(self):
        self.assertEqual([("00:00", "09:00")], free_slots([person("+09:00", ("09:00", "18:00"))], 60))

    def test_negative_offset_moves_later(self):
        self.assertEqual([("14:00", "22:00")], free_slots([person("-05:00", ("09:00", "17:00"))], 60))

    def test_half_hour_offset(self):
        self.assertEqual([("03:30", "11:30")], free_slots([person("+05:30", ("09:00", "17:00"))], 60))

    def test_teams_in_two_zones(self):
        people = [person("+09:00", ("09:00", "18:00"), [("10:00", "11:00")]), person("+01:00", ("08:00", "17:00"))]
        # 서울 09–18 = UTC 00–09, 바쁜 01–02. 파리(+01) 08–17 = UTC 07–16. 겹침 07–09.
        self.assertEqual([("07:00", "09:00")], free_slots(people, 30))

    def test_hours_past_the_day_are_clipped(self):
        self.assertEqual([("00:00", "04:00")], free_slots([person("+04:00", ("00:00", "08:00"))], 60))
        self.assertEqual([("23:00", "24:00")], free_slots([person("-03:00", ("20:00", "24:00"))], 60))

    def test_no_overlap_at_all(self):
        people = [person("+09:00", ("09:00", "18:00")), person("-05:00", ("09:00", "17:00"))]
        self.assertEqual([], free_slots(people, 15))


class Times(unittest.TestCase):
    def test_end_of_day(self):
        self.assertEqual([("22:00", "24:00")], free_slots([utc(hours=("22:00", "24:00"))], 60))

    def test_rejects_bad_times(self):
        for bad in ("24:30", "12:60", "7:00", "07-00", "ab:cd", "25:00"):
            with self.assertRaises(ValueError, msg=bad):
                free_slots([utc(hours=("06:00", bad))], 30)

    def test_rejects_reversed_or_empty_intervals(self):
        with self.assertRaises(ValueError):
            free_slots([utc(hours=("10:00", "09:00"))], 30)
        with self.assertRaises(ValueError):
            free_slots([utc(hours=("09:00", "12:00"), busy=[("10:00", "10:00")])], 30)

    def test_rejects_bad_offsets(self):
        for bad in ("+9:00", "09:00", "+05:20", "+15:00"):
            with self.assertRaises(ValueError, msg=bad):
                free_slots([person(bad, ("09:00", "17:00"))], 30)


class Model(unittest.TestCase):
    def test_random_against_minutes(self):
        source = random.Random(20261014)

        def text(m):
            return f"{m // 60:02d}:{m % 60:02d}"

        for _ in range(300):
            people = []
            free = [True] * 1440
            for _ in range(source.randint(1, 3)):
                offset = source.choice([0, 60, -60, 330, -570, 540])
                start = source.randrange(0, 1430)
                end = source.randint(start + 1, 1440)
                busy = []
                for _ in range(source.randint(0, 5)):
                    a = source.randrange(0, 1439)
                    b = source.randint(a + 1, 1440)
                    busy.append((text(a), text(b)))
                sign = "+" if offset >= 0 else "-"
                people.append(person(sign + text(abs(offset)), (text(start), text(end)), busy))
                for m in range(1440):
                    if not (start - offset <= m < end - offset):
                        free[m] = False
                for a, b in busy:
                    for m in range(1440):
                        if _in(a, b, offset, m, text):
                            free[m] = False
            duration = source.randint(1, 120)
            expected = []
            m = 0
            while m < 1440:
                if not free[m]:
                    m += 1
                    continue
                k = m
                while k < 1440 and free[k]:
                    k += 1
                if k - m >= duration:
                    expected.append((text(m), text(k)))
                m = k
            self.assertEqual(expected, free_slots(people, duration))


def _in(a, b, offset, minute, text):
    def to_minutes(s):
        return int(s[:2]) * 60 + int(s[3:])
    return to_minutes(a) - offset <= minute < to_minutes(b) - offset


if __name__ == "__main__":
    unittest.main()
