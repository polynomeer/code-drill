"""장바구니 — 골격. 시그니처는 그대로 두고 본문을 채운다."""


class Cart:
    def __init__(self, catalog: dict[str, int], coupons: dict[str, tuple[str, int]]) -> None:
        raise NotImplementedError

    def add(self, sku: str, qty: int = 1) -> None:
        raise NotImplementedError

    def remove(self, sku: str, qty: int = 1) -> None:
        raise NotImplementedError

    def apply_coupon(self, code: str) -> None:
        raise NotImplementedError

    def lines(self) -> list[tuple[str, int, int]]:
        raise NotImplementedError

    def total(self) -> int:
        raise NotImplementedError

    def clear(self) -> None:
        raise NotImplementedError
