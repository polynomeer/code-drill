"""재고 원장 — 골격.

요구사항은 문제 본문에 있다. 여기의 시그니처는 그대로 두고 본문을 채운다.
"""

from dataclasses import dataclass


class InvalidQuantity(ValueError):
    """수량이나 단가가 허용 범위 밖이다."""


class InsufficientStock(Exception):
    """출고하려는 수량이 남은 수량보다 많다."""

    def __init__(self, sku: str, requested: int, available: int) -> None:
        super().__init__(f"{sku}: {requested} requested, {available} available")
        self.sku = sku
        self.requested = requested
        self.available = available


@dataclass(frozen=True)
class Movement:
    kind: str  # "RECEIVE" 또는 "SHIP"
    quantity: int
    cost: int


class Ledger:
    def __init__(self) -> None:
        raise NotImplementedError

    def receive(self, sku: str, quantity: int, unit_cost: int) -> None:
        raise NotImplementedError

    def ship(self, sku: str, quantity: int) -> int:
        raise NotImplementedError

    def on_hand(self, sku: str) -> int:
        raise NotImplementedError

    def valuation(self) -> int:
        raise NotImplementedError

    def skus(self) -> list[str]:
        raise NotImplementedError

    def history(self, sku: str) -> list[Movement]:
        raise NotImplementedError
