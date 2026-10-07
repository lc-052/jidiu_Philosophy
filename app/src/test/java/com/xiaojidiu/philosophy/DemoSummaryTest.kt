package com.xiaojidiu.philosophy

import com.xiaojidiu.philosophy.data.remote.dto.RoundTableResponse
import kotlinx.serialization.json.Json
import java.io.File
import org.junit.Test
import org.junit.Assert.*

class DemoSummaryTest {
    @Test fun allThreeFixedSummariesReferToTheirOwnSpeeches() {
        val demos=Json.decodeFromString<List<RoundTableResponse>>(File("src/main/assets/seed/conversations.json").readText().removePrefix("\uFEFF"))
        assertEquals(3,demos.size)
        demos.forEach {demo->
            val ids=demo.messages.map {it.id}.toSet()
            val summary=demo.summary!!
            assertTrue(summary.disagreement.isNotEmpty())
            (summary.disagreement+summary.commonGround).forEach {point->assertTrue(point.text.isNotBlank());assertTrue(point.messageIds.isNotEmpty());assertTrue(ids.containsAll(point.messageIds))}
        }
    }
}
