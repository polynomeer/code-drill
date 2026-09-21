"""숨은 테스트. 사용자에게 나가지 않는다."""

import unittest

from pricing import Cart, CouponAlreadyApplied, UnknownCoupon, UnknownItem

CATALOG = {"apple": 500, "pear": 999, "melon": 12000, "free": 0, "kiwi": 501}
COUPONS = {"TEN": ("PERCENT", 10), "ALL": ("PERCENT", 100), "OFF2000": ("FIXED", 2000), "OFF1": ("FIXED", 1)}


class BulkDiscount(unittest.TestCase):
    def test_ten_or_more_gets_five_percent_floored(self):
        cart = Cart(CATALOG, COUPONS)
        cart.add("pear", 10)
        self.assertEqual([("pear", 10, 9491)], cart.lines())

    def test_nine_is_not_bulk(self):
        cart = Cart(CATALOG, COUPONS)
        cart.add("pear", 9)
        self.assertEqual([("pear", 9, 8991)], cart.lines())

    def test_bulk_applies_per_line_not_per_cart(self):
        cart = Cart(CATALOG, COUPONS)
        cart.add("apple", 5)
        cart.add("pear", 5)
        self.assertEqual(2500 + 4995, cart.total())

    def test_add_accumulates_into_bulk(self):
        cart = Cart(CATALOG, COUPONS)
        cart.add("apple", 6)
        cart.add("apple", 4)
        self.assertEqual([("apple", 10, 4750)], cart.lines())


class Coupons(unittest.TestCase):
    def test_percent_applies_to_subtotal_after_bulk(self):
        cart = Cart(CATALOG, COUPONS)
        cart.add("pear", 10)  # 9491
        cart.add("apple", 1)  # 500 → 소계 9991
        cart.apply_coupon("TEN")
        self.assertEqual(9991 - 999, cart.total())

    def test_percent_is_floored_once_not_per_line(self):
        cart = Cart(CATALOG, COUPONS)
        cart.add("pear", 1)   # 999 의 10% 는 99.9
        cart.add("kiwi", 1)   # 501 의 10% 는 50.1 — 줄마다 내림하면 149, 소계 1500 에 한 번이면 150
        cart.apply_coupon("TEN")
        self.assertEqual(1350, cart.total())

    def test_fixed_never_below_zero(self):
        cart = Cart(CATALOG, COUPONS)
        cart.add("apple", 2)
        cart.apply_coupon("OFF2000")
        self.assertEqual(0, cart.total())

    def test_hundred_percent_is_zero(self):
        cart = Cart(CATALOG, COUPONS)
        cart.add("melon")
        cart.apply_coupon("ALL")
        self.assertEqual(0, cart.total())

    def test_only_one_coupon(self):
        cart = Cart(CATALOG, COUPONS)
        cart.add("melon")
        cart.apply_coupon("OFF1")
        with self.assertRaises(CouponAlreadyApplied):
            cart.apply_coupon("TEN")
        self.assertEqual(11999, cart.total())

    def test_unknown_coupon(self):
        cart = Cart(CATALOG, COUPONS)
        with self.assertRaises(UnknownCoupon):
            cart.apply_coupon("NOPE")

    def test_invalid_coupon_values(self):
        with self.assertRaises(ValueError):
            Cart(CATALOG, {"BAD": ("PERCENT", 0)})
        with self.assertRaises(ValueError):
            Cart(CATALOG, {"BAD": ("PERCENT", 101)})
        with self.assertRaises(ValueError):
            Cart(CATALOG, {"BAD": ("FIXED", 0)})


class Lines(unittest.TestCase):
    def test_remove_below_zero_is_rejected_and_unchanged(self):
        cart = Cart(CATALOG, COUPONS)
        cart.add("apple", 2)
        with self.assertRaises(ValueError):
            cart.remove("apple", 3)
        self.assertEqual([("apple", 2, 1000)], cart.lines())

    def test_remove_to_zero_drops_line(self):
        cart = Cart(CATALOG, COUPONS)
        cart.add("apple", 2)
        cart.remove("apple", 2)
        self.assertEqual([], cart.lines())
        self.assertEqual(0, cart.total())

    def test_remove_unknown_and_bad_qty(self):
        cart = Cart(CATALOG, COUPONS)
        with self.assertRaises(UnknownItem):
            cart.remove("durian")
        with self.assertRaises(ValueError):
            cart.add("apple", 0)

    def test_lines_sorted_by_sku(self):
        cart = Cart(CATALOG, COUPONS)
        cart.add("pear")
        cart.add("apple")
        cart.add("melon")
        self.assertEqual(["apple", "melon", "pear"], [sku for sku, _, _ in cart.lines()])

    def test_free_item_and_clear(self):
        cart = Cart(CATALOG, COUPONS)
        cart.add("free", 20)
        cart.apply_coupon("TEN")
        self.assertEqual(0, cart.total())
        cart.clear()
        cart.add("melon")
        self.assertEqual(12000, cart.total())  # 쿠폰도 지워졌다
        cart.apply_coupon("TEN")             # 다시 붙는다
        self.assertEqual(10800, cart.total())
