"""숨은 테스트. 사용자에게 나가지 않는다."""

import unittest

from report import Record, RegionSummary, parse, render, summarize


class Parsing(unittest.TestCase):
    def test_bad_header_raises(self):
        with self.assertRaises(ValueError):
            parse("date,region,sku,qty\n2026-03-01,seoul,A,1,1\n")

    def test_errors_carry_one_based_line_numbers(self):
        text = "date,region,sku,qty,unit_price\n2026-03-01,seoul,A,0,10\n2026-03-01,seoul,A,1,-5\nbad line\n"
        result = parse(text)
        self.assertEqual([], result.records)
        self.assertEqual([2, 3, 4], [line for line, _ in result.errors])

    def test_blank_lines_are_skipped_but_counted(self):
        text = "date,region,sku,qty,unit_price\n\n   \n2026-03-01,seoul,A,x,10\n2026-03-02,seoul,B,2,10\n"
        result = parse(text)
        self.assertEqual([4], [line for line, _ in result.errors])
        self.assertEqual([Record("2026-03-02", "seoul", "B", 2, 10)], result.records)

    def test_fields_are_stripped(self):
        result = parse("date,region,sku,qty,unit_price\n 2026-03-01 , seoul , A-1 , 2 , 7 \n")
        self.assertEqual([Record("2026-03-01", "seoul", "A-1", 2, 7)], result.records)

    def test_errors_do_not_stop_parsing(self):
        text = "date,region,sku,qty,unit_price\nnope\n2026-03-01,seoul,A,1,1\n"
        result = parse(text)
        self.assertEqual(1, len(result.errors))
        self.assertEqual(1, len(result.records))

    def test_zero_price_is_allowed(self):
        result = parse("date,region,sku,qty,unit_price\n2026-03-01,seoul,FREE,1,0\n")
        self.assertEqual([], result.errors)


class Summary(unittest.TestCase):
    def test_top_sku_by_quantity_not_revenue(self):
        records = [Record("d", "seoul", "CHEAP", 5, 1), Record("d", "seoul", "DEAR", 1, 1000)]
        self.assertEqual("CHEAP", summarize(records)["seoul"].top_sku)

    def test_top_sku_sums_across_records_and_breaks_ties_lexically(self):
        records = [
            Record("d", "seoul", "B", 2, 1),
            Record("d", "seoul", "A", 1, 1),
            Record("d", "seoul", "A", 1, 1),
        ]
        self.assertEqual(RegionSummary(4, 3, "A"), summarize(records)["seoul"])

    def test_regions_are_independent(self):
        records = [Record("d", "seoul", "A", 1, 10), Record("d", "busan", "B", 3, 5)]
        summary = summarize(records)
        self.assertEqual(RegionSummary(10, 1, "A"), summary["seoul"])
        self.assertEqual(RegionSummary(15, 1, "B"), summary["busan"])


class Rendering(unittest.TestCase):
    def test_sorted_regions_thousands_and_no_trailing_newline(self):
        summary = {"seoul": RegionSummary(1234567, 3, "X"), "busan": RegionSummary(900, 1, "Y")}
        self.assertEqual(
            "busan   revenue=900  orders=1  top=Y\nseoul   revenue=1,234,567  orders=3  top=X",
            render(summary),
        )

    def test_empty_summary_renders_empty_string(self):
        self.assertEqual("", render({}))
