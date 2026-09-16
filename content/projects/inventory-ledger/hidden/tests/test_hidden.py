"""숨은 테스트. 사용자에게 나가지 않는다 — 실패 사유도 나가지 않는다."""

import unittest

from ledger import InsufficientStock, InvalidQuantity, Ledger, Movement


class FifoCost(unittest.TestCase):
    def test_partial_lot_keeps_remainder_at_its_own_cost(self):
        ledger = Ledger()
        ledger.receive("A", 4, 10)
        ledger.receive("A", 6, 20)
        ledger.receive("A", 5, 30)
        self.assertEqual(4 * 10 + 3 * 20, ledger.ship("A", 7))
        # 두 번째 로트에 3개가 단가 20 으로 남아 있어야 다음 출고 원가가 맞다.
        self.assertEqual(3 * 20 + 5 * 30, ledger.ship("A", 8))
        self.assertEqual(0, ledger.on_hand("A"))

    def test_exact_depletion_then_new_lot(self):
        ledger = Ledger()
        ledger.receive("A", 3, 7)
        self.assertEqual(21, ledger.ship("A", 3))
        ledger.receive("A", 2, 9)
        self.assertEqual(18, ledger.ship("A", 2))
        self.assertEqual(0, ledger.valuation())

    def test_zero_unit_cost_is_allowed(self):
        ledger = Ledger()
        ledger.receive("FREE", 5, 0)
        self.assertEqual(0, ledger.ship("FREE", 2))
        self.assertEqual(3, ledger.on_hand("FREE"))


class Shortage(unittest.TestCase):
    def test_insufficient_stock_leaves_ledger_unchanged(self):
        ledger = Ledger()
        ledger.receive("A", 2, 10)
        ledger.receive("A", 2, 20)
        with self.assertRaises(InsufficientStock) as caught:
            ledger.ship("A", 5)
        self.assertEqual("A", caught.exception.sku)
        self.assertEqual(5, caught.exception.requested)
        self.assertEqual(4, caught.exception.available)
        self.assertEqual(4, ledger.on_hand("A"))
        self.assertEqual(2 * 10 + 2 * 20, ledger.valuation())
        self.assertEqual(2, len(ledger.history("A")))

    def test_unknown_sku_is_a_shortage_of_zero(self):
        ledger = Ledger()
        with self.assertRaises(InsufficientStock) as caught:
            ledger.ship("NOPE", 1)
        self.assertEqual(0, caught.exception.available)

    def test_ship_rejects_non_positive_quantity_before_stock_check(self):
        ledger = Ledger()
        with self.assertRaises(InvalidQuantity):
            ledger.ship("A", 0)
        with self.assertRaises(InvalidQuantity):
            ledger.ship("A", -3)


class Views(unittest.TestCase):
    def test_valuation_spans_skus(self):
        ledger = Ledger()
        ledger.receive("A", 2, 10)
        ledger.receive("B", 3, 5)
        ledger.ship("A", 1)
        self.assertEqual(1 * 10 + 3 * 5, ledger.valuation())

    def test_skus_sorted_and_include_depleted(self):
        ledger = Ledger()
        ledger.receive("b", 1, 1)
        ledger.receive("a", 1, 1)
        ledger.ship("b", 1)
        self.assertEqual(["a", "b"], ledger.skus())

    def test_history_is_a_copy(self):
        ledger = Ledger()
        ledger.receive("A", 2, 10)
        ledger.ship("A", 1)
        recorded = ledger.history("A")
        self.assertEqual([Movement("RECEIVE", 2, 20), Movement("SHIP", 1, 10)], recorded)
        recorded.clear()
        self.assertEqual(2, len(ledger.history("A")))

    def test_invalid_receive_leaves_no_trace(self):
        ledger = Ledger()
        with self.assertRaises(InvalidQuantity):
            ledger.receive("A", -1, 10)
        self.assertEqual([], ledger.skus())
        self.assertEqual([], ledger.history("A"))
