package com.xiaojidiu.philosophy

import com.xiaojidiu.philosophy.domain.model.*
import kotlinx.serialization.json.Json
import java.io.File
import org.junit.Assert.*
import org.junit.Test

class GameContentTest {
    @Test fun catalogHasRealContentSourcesAndValidInternalLinks() {
        fun read(name: String)=File("src/main/assets/seed/$name.json").readText().removePrefix("\uFEFF")
        val catalog=Json.decodeFromString<GamesCatalog>(read("games"))
        val questions=Json.decodeFromString<List<Question>>(read("questions")).map {it.id}.toSet()
        val people=Json.decodeFromString<List<Philosopher>>(read("philosophers")).map {it.id}.toSet()
        assertEquals(1,catalog.version)
        assertEquals(setOf("subahibi","sakura"),catalog.works.map {it.id}.toSet())
        catalog.works.forEach {work->
            assertEquals(4,work.topics.size);assertEquals(work.topics.size,work.topics.map {it.id}.distinct().size)
            assertTrue(work.summary.isNotBlank());assertTrue(work.topics.any {it.spoiler})
            work.topics.forEach {topic->
                assertTrue(topic.context.isNotBlank());assertTrue(topic.caution.isNotBlank());assertTrue(topic.explanation.size>=2)
                assertEquals(3,topic.entryChoices.size);assertEquals(3,topic.entryChoices.distinct().size)
                assertTrue(topic.relation.contains("解读") || topic.relation.contains("对照"))
                assertTrue(topic.sources.isNotEmpty());assertTrue(people.containsAll(topic.philosopherIds));assertTrue(questions.containsAll(topic.questionIds))
                (topic.sources+work.sources).forEach {assertTrue(it.url.startsWith("https://"));assertTrue(it.note.isNotBlank())}
            }
        }
    }
}
