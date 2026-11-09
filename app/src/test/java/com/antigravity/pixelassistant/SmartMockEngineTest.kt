package com.antigravity.pixelassistant

import com.antigravity.pixelassistant.ai.SmartMockEngine
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SmartMockEngineTest {

    @Test
    fun testMovieTopicGeneratesMovieReplies() {
        val partnerMsg = "最近Netflixで何かおすすめの映画やアニメある？"
        val replies = SmartMockEngine.generateReplyCandidates(partnerMsg)

        assertEquals(4, replies.size)
        assertTrue(replies.any { it.text.contains("作品") || it.text.contains("映画") })
        assertEquals("😊 共感＋質問", replies[0].label)
    }

    @Test
    fun testFoodTopicGeneratesDiningReplies() {
        val partnerMsg = "最近行ったイタリアンのお店がすごく美味しかったよ！"
        val replies = SmartMockEngine.generateReplyCandidates(partnerMsg)

        assertEquals(4, replies.size)
        assertTrue(replies.any { it.text.contains("美味しそう") || it.text.contains("お店") })
    }

    @Test
    fun testEnglishTopicGeneratesEnglishReplies() {
        val partnerMsg = "Hey there! How has your day been?"
        val replies = SmartMockEngine.generateReplyCandidates(partnerMsg)

        assertEquals(4, replies.size)
        assertTrue(replies[0].text.contains("awesome") || replies[0].text.contains("How"))
    }

    @Test
    fun testSubtitleTranslationFallback() {
        val translated = SmartMockEngine.translateSubtitles("Hello, are you ready?")
        assertTrue(translated.isNotEmpty())
    }
}
