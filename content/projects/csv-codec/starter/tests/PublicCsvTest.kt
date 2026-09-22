package tests

import codedrill.*
import csv.*

/** 공개 테스트. 채점은 이것과 숨은 테스트를 함께 돌린다. 하네스의 단언을 쓴다. */
class PublicCsvTest {

    fun testPlainRows() {
        assertEquals(listOf(listOf("a", "b"), listOf("1", "2")), Csv.parse("a,b\n1,2\n"))
    }

    fun testQuotedCommaAndEscapedQuote() {
        assertEquals(listOf(listOf("x, y", "he said \"hi\"")), Csv.parse("\"x, y\",\"he said \"\"hi\"\"\"\n"))
    }

    fun testFormatQuotesOnlyWhenNeeded() {
        assertEquals("a,\"b,c\",d\n", Csv.format(listOf(listOf("a", "b,c", "d"))))
    }
}
