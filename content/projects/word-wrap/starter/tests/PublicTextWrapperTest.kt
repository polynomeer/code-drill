package tests

import codedrill.*
import wrap.TextWrapper

/** 공개 테스트. 채점은 이것과 숨은 테스트를 함께 돌린다. 하네스의 단언을 쓴다. */
class PublicTextWrapperTest {

    fun testWrapsGreedily() {
        val lines = TextWrapper().wrap("the quick brown fox jumps over the lazy dog", 10)
        assertEquals(listOf("the quick", "brown fox", "jumps over", "the lazy", "dog"), lines)
    }

    fun testEmptyTextHasNoLines() {
        assertEquals(emptyList<String>(), TextWrapper().wrap("", 5))
    }

    fun testRejectsZeroWidth() {
        assertThrows<IllegalArgumentException> { TextWrapper().wrap("a", 0) }
    }
}
