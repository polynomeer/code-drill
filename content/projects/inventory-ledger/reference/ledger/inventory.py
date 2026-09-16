"""재고 원장 — 참조 구현."""

from collections import deque
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
        # sku -> 로트 큐. 로트는 [남은 수량, 단가] 이고 오래된 것이 앞이다.
        self._lots: dict[str, deque[list[int]]] = {}
        self._history: dict[str, list[Movement]] = {}

    def receive(self, sku: str, quantity: int, unit_cost: int) -> None:
        if quantity < 1 or unit_cost < 0:
            raise InvalidQuantity(f"quantity={quantity}, unit_cost={unit_cost}")
        self._lots.setdefault(sku, deque()).append([quantity, unit_cost])
        self._history.setdefault(sku, []).append(Movement("RECEIVE", quantity, quantity * unit_cost))

    def ship(self, sku: str, quantity: int) -> int:
        if quantity < 1:
            raise InvalidQuantity(f"quantity={quantity}")
        available = self.on_hand(sku)
        if quantity > available:
            raise InsufficientStock(sku, quantity, available)

        lots = self._lots[sku]
        remaining = quantity
        cost = 0
        while remaining > 0:
            lot = lots[0]
            taken = min(lot[0], remaining)
            cost += taken * lot[1]
            lot[0] -= taken
            remaining -= taken
            if lot[0] == 0:
                lots.popleft()
        self._history[sku].append(Movement("SHIP", quantity, cost))
        return cost

    def on_hand(self, sku: str) -> int:
        return sum(lot[0] for lot in self._lots.get(sku, ()))

    def valuation(self) -> int:
        return sum(lot[0] * lot[1] for lots in self._lots.values() for lot in lots)

    def skus(self) -> list[str]:
        return sorted(self._lots)

    def history(self, sku: str) -> list[Movement]:
        return list(self._history.get(sku, ()))
