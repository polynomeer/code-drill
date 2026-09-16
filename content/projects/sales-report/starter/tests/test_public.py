"""공개 테스트. 채점은 이것과 숨은 테스트를 함께 돌린다."""

import unittest

from report import Record, RegionSummary, parse, render, summarize

SAMPLE = """date,region,sku,qty,unit_price
2026-03-01,seoul,A-100,3,1200
2026-03-01,busan,B-200,1,5000
"""


class PublicTests(unittest.TestCase):
    def test_parse_records(self):
        result = parse(SAMPLE)
        self.assertEqual([], result.errors)
        self.assertEqual(Record("2026-03-01", "seoul", "A-100", 3, 1200), result.records[0])

    def test_summarize_revenue(self):
        summary = summarize(parse(SAMPLE).records)
        self.assertEqual(RegionSummary(3600, 1, "A-100"), summary["seoul"])

    def test_render_format(self):
        self.assertEqual(
            "busan   revenue=5,000  orders=1  top=B-200\nseoul   revenue=3,600  orders=1  top=A-100",
            render(summarize(parse(SAMPLE).records)),
        )
