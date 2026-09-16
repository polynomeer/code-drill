# 매출 집계 보고서

매출 기록을 읽어 지역별 보고서를 만드는 `report/sales.py` 를 완성한다. 입력은 파일이 아니라
**문자열 하나**다 — 첫 줄이 헤더이고 그 아래 한 줄이 기록 하나다.

```
date,region,sku,qty,unit_price
2026-03-01,seoul,A-100,3,1200
2026-03-01,busan,B-200,1,5000
```

## `parse(text) -> ParseResult`

- 첫 줄은 헤더다. 정확히 `date,region,sku,qty,unit_price` 가 아니면 `ValueError`.
- 빈 줄과 공백만 있는 줄은 건너뛴다. 줄 번호는 세지만 기록도 오류도 아니다.
- 각 기록은 쉼표로 정확히 다섯 조각이어야 하고, `qty` 는 1 이상의 정수, `unit_price` 는 0 이상의
  정수여야 한다. 조각의 앞뒤 공백은 지운다. 어긋나면 **예외를 던지지 않고** `errors` 에
  `(줄 번호, 사유)` 를 모은다. 줄 번호는 **헤더를 1 로 세는 1 기반**이다.
- `records` 는 `Record(date, region, sku, qty, unit_price)` 의 목록, 입력 순서다.

## `summarize(records) -> dict[str, RegionSummary]`

지역마다 `RegionSummary(revenue, orders, top_sku)`:

- `revenue` 는 `qty × unit_price` 의 합, `orders` 는 기록 수.
- `top_sku` 는 그 지역에서 **수량 합이 가장 큰** SKU. 같으면 사전순으로 앞선 것.

## `render(summary) -> str`

지역을 **사전순**으로, 한 줄에 하나. 형식은 정확히:

```
seoul   revenue=3,600  orders=1  top=A-100
```

즉 `f"{region:<7} revenue={revenue:,}  orders={orders}  top={top_sku}"`. 줄은 `\n` 으로 잇고
마지막 줄 뒤에는 개행이 없다. 지역이 없으면 빈 문자열이다.

## 제출

`report/sales.py` 를 고친다. `tests/` 아래의 파일은 채점 때 숨은 스위트로 덮인다.
