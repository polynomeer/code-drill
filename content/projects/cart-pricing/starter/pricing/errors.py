"""예외 셋. 이름은 요구사항에 있다. 이 파일은 그대로 둔다."""


class UnknownItem(KeyError):
    """상품 목록에 없는 sku."""


class UnknownCoupon(KeyError):
    """쿠폰 목록에 없는 코드."""


class CouponAlreadyApplied(ValueError):
    """쿠폰은 하나만 붙는다."""
