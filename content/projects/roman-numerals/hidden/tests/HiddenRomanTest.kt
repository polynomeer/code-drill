package tests

import codedrill.*
import roman.Roman

/** 숨은 테스트. 사용자에게 나가지 않는다. */
class HiddenRomanTest {

    fun testSubtractiveFormsWhenWriting() {
        assertEquals("IV", Roman.toRoman(4))
        assertEquals("IX", Roman.toRoman(9))
        assertEquals("XL", Roman.toRoman(40))
        assertEquals("XC", Roman.toRoman(90))
        assertEquals("CD", Roman.toRoman(400))
        assertEquals("CM", Roman.toRoman(900))
        assertEquals("MCMXCIV", Roman.toRoman(1994))
    }

    fun testRangeEnds() {
        assertEquals("I", Roman.toRoman(1))
        assertEquals("MMMCMXCIX", Roman.toRoman(3999))
        assertThrows<IllegalArgumentException> { Roman.toRoman(4000) }
        assertThrows<IllegalArgumentException> { Roman.toRoman(-5) }
    }

    fun testReadsEveryForm() {
        assertEquals(4, Roman.fromRoman("IV"))
        assertEquals(3999, Roman.fromRoman("MMMCMXCIX"))
        assertEquals(1, Roman.fromRoman("I"))
        assertEquals(444, Roman.fromRoman("CDXLIV"))
    }

    fun testRoundTripsEveryNumber() {
        for (n in 1..3999) assertEquals(n, Roman.fromRoman(Roman.toRoman(n)))
    }

    fun testRejectsRepeatedSymbols() {
        for (text in listOf("IIII", "XXXX", "CCCC", "MMMM", "VV", "LL", "DD")) {
            assertThrows<IllegalArgumentException> { Roman.fromRoman(text) }
        }
    }

    fun testRejectsWrongSubtractions() {
        for (text in listOf("IC", "IL", "XM", "VX", "IIV", "IXIX", "XCX")) {
            assertThrows<IllegalArgumentException> { Roman.fromRoman(text) }
        }
    }

    fun testRejectsLowercaseAndJunk() {
        for (text in listOf("xii", "Xii", "X I", "X1", "A")) {
            assertThrows<IllegalArgumentException> { Roman.fromRoman(text) }
        }
    }

    fun testRejectsEmpty() {
        assertThrows<IllegalArgumentException> { Roman.fromRoman("") }
    }
}
