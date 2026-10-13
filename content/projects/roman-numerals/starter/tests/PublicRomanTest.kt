package tests

import codedrill.*
import roman.Roman

/** 공개 테스트. 채점은 이것과 숨은 테스트를 함께 돌린다. 하네스의 단언을 쓴다. */
class PublicRomanTest {

    fun testWritesSimpleNumbers() {
        assertEquals("XII", Roman.toRoman(12))
        assertEquals("MMXXVI", Roman.toRoman(2026))
    }

    fun testReadsSubtractiveForms() {
        assertEquals(1994, Roman.fromRoman("MCMXCIV"))
    }

    fun testRejectsZero() {
        assertThrows<IllegalArgumentException> { Roman.toRoman(0) }
    }
}
