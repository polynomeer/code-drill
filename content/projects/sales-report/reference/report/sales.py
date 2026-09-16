"""매출 집계 — 참조 구현."""

from dataclasses import dataclass, field

HEADER = "date,region,sku,qty,unit_price"


@dataclass(frozen=True)
class Record:
    date: str
    region: str
    sku: str
    qty: int
    unit_price: int


@dataclass
class ParseResult:
    records: list[Record] = field(default_factory=list)
    errors: list[tuple[int, str]] = field(default_factory=list)


@dataclass(frozen=True)
class RegionSummary:
    revenue: int
    orders: int
    top_sku: str


def parse(text: str) -> ParseResult:
    lines = text.split("\n")
    if not lines or lines[0].strip() != HEADER:
        raise ValueError("header must be exactly: " + HEADER)

    result = ParseResult()
    for number, line in enumerate(lines[1:], start=2):
        if not line.strip():
            continue
        parts = [part.strip() for part in line.split(",")]
        if len(parts) != 5:
            result.errors.append((number, f"expected 5 fields, got {len(parts)}"))
            continue
        date, region, sku, qty_text, price_text = parts
        try:
            qty = int(qty_text)
            unit_price = int(price_text)
        except ValueError:
            result.errors.append((number, "qty and unit_price must be integers"))
            continue
        if qty < 1:
            result.errors.append((number, "qty must be at least 1"))
            continue
        if unit_price < 0:
            result.errors.append((number, "unit_price must not be negative"))
            continue
        result.records.append(Record(date, region, sku, qty, unit_price))
    return result


def summarize(records: list[Record]) -> dict[str, RegionSummary]:
    revenue: dict[str, int] = {}
    orders: dict[str, int] = {}
    quantities: dict[str, dict[str, int]] = {}
    for record in records:
        revenue[record.region] = revenue.get(record.region, 0) + record.qty * record.unit_price
        orders[record.region] = orders.get(record.region, 0) + 1
        per_sku = quantities.setdefault(record.region, {})
        per_sku[record.sku] = per_sku.get(record.sku, 0) + record.qty

    summary: dict[str, RegionSummary] = {}
    for region, per_sku in quantities.items():
        # 수량 합이 가장 큰 SKU. 같으면 사전순으로 앞선 것.
        top = min(per_sku, key=lambda sku: (-per_sku[sku], sku))
        summary[region] = RegionSummary(revenue[region], orders[region], top)
    return summary


def render(summary: dict[str, RegionSummary]) -> str:
    return "\n".join(
        f"{region:<7} revenue={summary[region].revenue:,}  orders={summary[region].orders}  top={summary[region].top_sku}"
        for region in sorted(summary)
    )
