"""공개 테스트. 채점은 이것과 숨은 테스트를 함께 돌린다."""

import unittest

from ledger import InvalidQuantity, Ledger


class PublicTests(unittest.TestCase):
    def test_receive_then_on_hand(self):
        ledger = Ledger()
        ledger.receive("A", 10, 100)
        self.assertEqual(10, ledger.on_hand("A"))
        self.assertEqual(0, ledger.on_hand("unknown"))

    def test_ship_uses_oldest_lot_first(self):
        ledger = Ledger()
        ledger.receive("A", 10, 100)
        ledger.receive("A", 10, 200)
        self.assertEqual(10 * 100 + 5 * 200, ledger.ship("A", 15))
        self.assertEqual(5, ledger.on_hand("A"))

    def test_rejects_non_positive_quantity(self):
        ledger = Ledger()
        with self.assertRaises(InvalidQuantity):
            ledger.receive("A", 0, 100)
        with self.assertRaises(InvalidQuantity):
            ledger.receive("A", 1, -1)
