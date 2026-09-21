"""공개 테스트. 채점은 이것과 숨은 테스트를 함께 돌린다."""

import unittest

from pricing import Cart, UnknownItem

CATALOG = {"apple": 500, "pear": 999, "melon": 12000}
COUPONS = {"TEN": ("PERCENT", 10), "OFF2000": ("FIXED", 2000)}


class PublicTests(unittest.TestCase):
    def test_add_and_total(self):
        cart = Cart(CATALOG, COUPONS)
        cart.add("apple", 3)
        cart.add("melon")
        self.assertEqual([("apple", 3, 1500), ("melon", 1, 12000)], cart.lines())
        self.assertEqual(13500, cart.total())

    def test_percent_coupon(self):
        cart = Cart(CATALOG, COUPONS)
        cart.add("melon")
        cart.apply_coupon("TEN")
        self.assertEqual(10800, cart.total())

    def test_unknown_item(self):
        cart = Cart(CATALOG, COUPONS)
        with self.assertRaises(UnknownItem):
            cart.add("durian")
