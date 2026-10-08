"""숨은 테스트. 사용자에게 나가지 않는다."""

import unittest

from sheet import CYCLE, DIV0, Sheet


def _cells():
    return [f"{chr(c)}{r}" for c in range(ord("A"), ord("Z") + 1) for r in range(1, 100)]


class Recalculation(unittest.TestCase):
    def test_dependents_follow_updates(self):
        sheet = Sheet()
        sheet.set("A1", "2")
        sheet.set("B1", "=A1*10")
        sheet.set("C1", "=B1+A1")
        sheet.set("A1", "3")
        self.assertEqual(30, sheet.get("B1"))
        self.assertEqual(33, sheet.get("C1"))

    def test_formula_before_its_inputs(self):
        sheet = Sheet()
        sheet.set("B2", "=A2+1")
        sheet.set("A2", "41")
        self.assertEqual(42, sheet.get("B2"))

    def test_clearing_makes_zero(self):
        sheet = Sheet()
        sheet.set("A1", "9")
        sheet.set("B1", "=A1")
        sheet.set("A1", "  ")
        self.assertEqual(0, sheet.get("A1"))
        self.assertEqual(0, sheet.get("B1"))

    def test_long_chain(self):
        # 2 574 칸이 앞 칸 + 1 로 이어진다 — 재귀로 따라가면 파이썬의 재귀 한도를 넘는다.
        sheet = Sheet()
        cells = _cells()
        sheet.set(cells[0], "1")
        for before, after in zip(cells, cells[1:]):
            sheet.set(after, f"={before}+1")
        self.assertEqual(len(cells), sheet.get(cells[-1]))

    def test_shared_inputs_are_computed_once(self):
        # 각 칸이 바로 위 칸을 두 번 쓴다 — 칸마다 다시 계산하면 2^59 번이다.
        sheet = Sheet()
        sheet.set("A1", "1")
        for row in range(2, 61):
            sheet.set(f"A{row}", f"=A{row - 1}+A{row - 1}")
        self.assertEqual(2 ** 59, sheet.get("A60"))


class Arithmetic(unittest.TestCase):
    def setUp(self):
        self.sheet = Sheet()

    def value(self, formula):
        self.sheet.set("Z99", formula)
        return self.sheet.get("Z99")

    def test_precedence_and_parentheses(self):
        self.assertEqual(14, self.value("=2+3*4"))
        self.assertEqual(20, self.value("=(2+3)*4"))
        self.assertEqual(2, self.value("=8-4-2"))
        self.assertEqual(2, self.value("=16/4/2"))
        self.assertEqual(7, self.value("=1+12/2"))

    def test_unary_minus(self):
        self.assertEqual(-5, self.value("=-5"))
        self.assertEqual(5, self.value("=--5"))
        self.assertEqual(-6, self.value("=2*-3"))

    def test_division_truncates_toward_zero(self):
        self.assertEqual(-3, self.value("=-7/2"))
        self.assertEqual(-3, self.value("=7/-2"))
        self.assertEqual(3, self.value("=-7/-2"))
        self.assertEqual(3, self.value("=7/2"))

    def test_whitespace(self):
        self.assertEqual(9, self.value("=  4 *  2 + 1 "))

    def test_sum_over_rectangle(self):
        s = self.sheet
        s.set("A1", "1")
        s.set("A2", "2")
        s.set("B1", "10")
        s.set("B2", "20")
        s.set("C5", "=SUM(A1:B2)")
        self.assertEqual(33, s.get("C5"))
        s.set("C6", "=SUM(B2:A1)")
        self.assertEqual(33, s.get("C6"))
        s.set("C7", "=SUM(A1:A3)+1")
        self.assertEqual(4, s.get("C7"))


class Errors(unittest.TestCase):
    def test_division_by_zero_propagates(self):
        sheet = Sheet()
        sheet.set("A1", "=1/0")
        sheet.set("B1", "=A1+1")
        sheet.set("C1", "=SUM(A1:B1)")
        sheet.set("D1", "=5/(2-2)")
        self.assertEqual(DIV0, sheet.get("A1"))
        self.assertEqual(DIV0, sheet.get("B1"))
        self.assertEqual(DIV0, sheet.get("C1"))
        self.assertEqual(DIV0, sheet.get("D1"))

    def test_cycle_members_and_dependents(self):
        sheet = Sheet()
        sheet.set("A1", "=B1+1")
        sheet.set("B1", "=A1+1")
        sheet.set("C1", "=A1*0+7")
        sheet.set("D1", "=5")
        self.assertEqual(CYCLE, sheet.get("A1"))
        self.assertEqual(CYCLE, sheet.get("B1"))
        self.assertEqual(CYCLE, sheet.get("C1"))
        self.assertEqual(5, sheet.get("D1"))

    def test_self_reference(self):
        sheet = Sheet()
        sheet.set("A1", "=A1")
        self.assertEqual(CYCLE, sheet.get("A1"))

    def test_cycle_beats_division_by_zero(self):
        sheet = Sheet()
        sheet.set("A1", "=1/0")
        sheet.set("B1", "=C1")
        sheet.set("C1", "=B1")
        sheet.set("D1", "=A1+B1")
        self.assertEqual(CYCLE, sheet.get("D1"))

    def test_breaking_a_cycle(self):
        sheet = Sheet()
        sheet.set("A1", "=B1")
        sheet.set("B1", "=A1")
        sheet.set("B1", "4")
        self.assertEqual(4, sheet.get("A1"))

    def test_bad_input_keeps_old_value(self):
        sheet = Sheet()
        sheet.set("A1", "5")
        for bad in ["=", "=SUM(A1)", "=a1", "=A0", "=2 3", "=(1", "1.5", "=A100", "=1%2"]:
            with self.assertRaises(ValueError):
                sheet.set("A1", bad)
        self.assertEqual(5, sheet.get("A1"))
        for bad in ["a1", "A0", "A100", "", "1A"]:
            with self.assertRaises(ValueError):
                sheet.get(bad)


if __name__ == "__main__":
    unittest.main()
