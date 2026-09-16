"""매출 집계 — 골격. 시그니처와 데이터 클래스는 그대로 두고 본문을 채운다."""

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
    raise NotImplementedError


def summarize(records: list[Record]) -> dict[str, RegionSummary]:
    raise NotImplementedError


def render(summary: dict[str, RegionSummary]) -> str:
    raise NotImplementedError
