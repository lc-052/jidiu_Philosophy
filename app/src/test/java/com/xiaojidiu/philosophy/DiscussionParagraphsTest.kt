package com.xiaojidiu.philosophy

import com.xiaojidiu.philosophy.util.DiscussionParagraphs
import org.junit.Assert.*
import org.junit.Test

class DiscussionParagraphsTest {
    @Test fun longMessagePreservesEveryCharacterAndQuotes() {
        val original="！？“我们希望世界给出一个答案。”"+"但是，问题未必只有一种理解，也未必只有一种生活方式。".repeat(8)+"最后留下一个没有句号的想法"
        val paragraphs=DiscussionParagraphs.split(original)
        assertTrue(paragraphs.size>1)
        assertEquals(original,paragraphs.joinToString(""))
        assertTrue(paragraphs.first().contains("。”"))
    }
    @Test fun intentionalParagraphsRemainSeparate() {
        assertEquals(listOf("先说立场。","再解释理由。"),DiscussionParagraphs.split("先说立场。\r\n\r\n再解释理由。"))
    }
    @Test fun noPunctuationIsNotArbitrarilyCut() {
        val text="没有句号的长句".repeat(30)
        assertEquals(listOf(text),DiscussionParagraphs.split(text))
    }
    @Test fun blankContentHasNoEmptyParagraphs() {
        assertTrue(DiscussionParagraphs.split(" \n\r\n").isEmpty())
    }
}
