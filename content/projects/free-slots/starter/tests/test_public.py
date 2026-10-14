"""공개 테스트. 채점은 이것과 숨은 테스트를 함께 돌린다."""

import unittest

from meetings import free_slots


def person(offset, hours, busy=()):
    return {"offset": offset, "hours": hours, "busy": list(busy)}


class PublicTests(unittest.TestCase):
    def test_one_person_one_meeting(self):
        people = [person("+00:00", ("09:00", "12:00"), [("10:00", "11:00")])]
        self.assertEqual([("09:00", "10:00"), ("11:00", "12:00")], free_slots(people, 30))

    def test_working_hours_intersect(self):
        people = [person("+00:00", ("09:00", "17:00")), person("+00:00", ("13:00", "20:00"))]
        self.assertEqual([("13:00", "17:00")], free_slots(people, 60))

    def test_rejects_bad_time(self):
        with self.assertRaises(ValueError):
            free_slots([person("+00:00", ("9:00", "17:00"))], 30)


if __name__ == "__main__":
    unittest.main()
