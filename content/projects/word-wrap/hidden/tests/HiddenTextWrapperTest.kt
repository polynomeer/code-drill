package tests

import codedrill.*
import wrap.TextWrapper

/** 숨은 테스트. 사용자에게 나가지 않는다 (§8.3). */
class HiddenTextWrapperTest {

    private val wrapper = TextWrapper()

    fun testExactFitStaysOnOneLine() {
        assertEquals(listOf("abc de", "f"), wrapper.wrap("abc de f", 6))
        assertEquals(listOf("abcdef"), wrapper.wrap("abcdef", 6))
    }

    fun testLongWordIsCut() {
        assertEquals(listOf("abcd", "efgh", "ij"), wrapper.wrap("abcdefghij", 4))
        assertEquals(listOf("hi", "abcd", "efgh"), wrapper.wrap("hi abcdefgh", 4))
    }

    fun testWordsJoinTheLastPieceOfALongWord() {
        assertEquals(listOf("abcde", "f gh"), wrapper.wrap("abcdef gh", 5))
        assertEquals(listOf("abc", "def", "g h"), wrapper.wrap("abcdefg h", 3))
    }

    fun testAnyWhitespaceSeparates() {
        assertEquals(listOf("a b c", "d"), wrapper.wrap("a\tb\nc   d", 5))
        assertEquals(listOf("one two"), wrapper.wrap("  one \t two  ", 20))
    }

    fun testBlankTextHasNoLines() {
        assertEquals(emptyList<String>(), wrapper.wrap("   \n\t \n", 5))
    }

    fun testParagraphsAreSeparatedByOneEmptyLine() {
        val text = "first paragraph here\n\nsecond one"
        assertEquals(listOf("first", "paragraph", "here", "", "second", "one"), wrapper.wrap(text, 9))
    }

    fun testManyBlankLinesCollapse() {
        val text = "\n\nalpha\n\n\n   \n\nbeta\n\n"
        assertEquals(listOf("alpha", "", "beta"), wrapper.wrap(text, 10))
    }

    fun testSingleNewlineIsNotAParagraph() {
        assertEquals(listOf("one two", "three"), wrapper.wrap("one\ntwo\nthree", 7))
    }

    fun testWidthOne() {
        assertEquals(listOf("a", "b", "c", "d"), wrapper.wrap("ab c\nd", 1))
    }

    fun testWindowsLineEndings() {
        assertEquals(listOf("a b", "", "c"), wrapper.wrap("a\r\nb\r\n\r\nc", 5))
    }

    fun testNoLineIsWiderThanWidth() {
        val words = (1..400).joinToString(" ") { "w".repeat(it % 13 + 1) }
        for (width in 1..20) {
            val lines = wrapper.wrap(words, width)
            assertTrue(lines.all { it.length <= width })
            assertEquals(words.replace(" ", ""), lines.joinToString("").replace(" ", ""))
        }
    }

    fun testRejectsNegativeWidth() {
        assertThrows<IllegalArgumentException> { wrapper.wrap("a", -3) }
    }
}
