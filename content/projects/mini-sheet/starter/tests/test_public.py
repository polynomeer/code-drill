"""공개 테스트. 채점은 이것과 숨은 테스트를 함께 돌린다."""

import unittest

from sheet import Sheet


class PublicTests(unittest.TestCase):
    def test_number_and_formula(self):
        sheet = Sheet()
        sheet.set("A1", "2")
        sheet.set("B1", "=A1*3+1")
        self.assertEqual(2, sheet.get("A1"))
        self.assertEqual(7, sheet.get("B1"))

    def test_empty_cell_is_zero(self):
        sheet = Sheet()
        self.assertEqual(0, sheet.get("C3"))
        sheet.set("A1", "=C3+5")
        self.assertEqual(5, sheet.get("A1"))

    def test_rejects_bad_input(self):
        sheet = Sheet()
        with self.assertRaises(ValueError):
            sheet.set("AA1", "1")
        with self.assertRaises(ValueError):
            sheet.set("A1", "hello")
        with self.assertRaises(ValueError):
            sheet.set("A1", "=1+")
