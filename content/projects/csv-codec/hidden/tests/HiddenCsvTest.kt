package tests

import codedrill.*
import csv.*

/** 숨은 테스트. 사용자에게 나가지 않는다. */
class HiddenCsvTest {

    fun testEmptyFieldsAreKept() {
        assertEquals(listOf(listOf("a", "", "b")), Csv.parse("a,,b"))
        assertEquals(listOf(listOf("a", "")), Csv.parse("a,"))
        assertEquals(listOf(listOf("")), Csv.parse("\n"))
        assertEquals(listOf<List<String>>(), Csv.parse(""))
    }

    fun testCrlfAndTrailingNewline() {
        assertEquals(listOf(listOf("a", "b"), listOf("c", "d")), Csv.parse("a,b\r\nc,d\r\n"))
        assertEquals(listOf(listOf("a"), listOf("b")), Csv.parse("a\nb"))
        // 마지막 줄바꿈 하나만 없는 것과 같다 — 둘이면 빈 줄이 하나 있다.
        assertEquals(listOf(listOf("a"), listOf("")), Csv.parse("a\n\n"))
    }

    fun testNewlineInsideQuotes() {
        assertEquals(listOf(listOf("line1\nline2", "z")), Csv.parse("\"line1\nline2\",z\n"))
    }

    fun testMalformed() {
        assertThrows<MalformedCsv> { Csv.parse("\"abc") }
        assertThrows<MalformedCsv> { Csv.parse("\"a\"b,c") }
        assertThrows<MalformedCsv> { Csv.parse("ab\"c,d") }
        val error = assertThrows<MalformedCsv> { Csv.parse("ok,\"a\"x") }
        assertEquals(6, error.offset)
    }

    fun testFormatEscapesQuotesAndNewlines() {
        assertEquals("\"say \"\"hi\"\"\",\"a\nb\",\"c\rd\"\n", Csv.format(listOf(listOf("say \"hi\"", "a\nb", "c\rd"))))
        assertEquals("a,,b\n", Csv.format(listOf(listOf("a", "", "b"))))
        assertEquals("", Csv.format(emptyList()))
    }

    fun testRoundTrip() {
        val rows = listOf(listOf("plain", "with,comma", "with \"quote\"", "", "multi\nline"), listOf("", ""), listOf("x"))
        assertEquals(rows, Csv.parse(Csv.format(rows)))
    }

    fun testRaggedRows() {
        assertEquals(listOf(listOf("a", "b", "c"), listOf("d"), listOf("e", "f")), Csv.parse("a,b,c\nd\ne,f\n"))
    }
}
