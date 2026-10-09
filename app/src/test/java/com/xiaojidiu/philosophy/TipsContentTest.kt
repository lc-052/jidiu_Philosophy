package com.xiaojidiu.philosophy

import com.xiaojidiu.philosophy.domain.model.*
import kotlinx.serialization.json.Json
import org.junit.Assert.*
import org.junit.Test
import java.io.File

class TipsContentTest {
    private fun read(name: String)=File("src/main/assets/seed/$name.json").readText().removePrefix("\uFEFF")
    private fun terms()=Json.decodeFromString<TipsCatalog>(read("tips")).terms
    @Test fun catalogContainsRequestedWordsAndAllLinksResolve() {
        val data=Json.decodeFromString<TipsCatalog>(read("tips"));val terms=data.terms
        assertEquals(1,data.version);assertEquals(30,terms.size)
        assertEquals(terms.size,terms.map {it.id}.distinct().size)
        assertTrue(terms.map {it.name}.containsAll(listOf("形而上学","决定论","自由意志")))
        val questions=Json.decodeFromString<List<Question>>(read("questions")).map {it.id}.toSet()
        val people=Json.decodeFromString<List<Philosopher>>(read("philosophers")).map {it.id}.toSet()
        val schools=Json.decodeFromString<List<School>>(read("schools")).map {it.id}.toSet()
        terms.forEach {term->
            assertTrue(term.name.isNotBlank() && term.english.isNotBlank() && term.category.isNotBlank())
            assertTrue(term.questions.size>=2);assertTrue(term.questions.all {it.endsWith("？")})
            assertTrue(term.readings.isNotEmpty());assertTrue(term.readings.all {it.author.isNotBlank() && it.work.isNotBlank() && it.question.isNotBlank()})
            assertTrue(questions.containsAll(term.questionIds));assertTrue(people.containsAll(term.philosopherIds));assertTrue(schools.containsAll(term.schoolIds))
            assertTrue(terms.map {it.id}.containsAll(term.relatedTermIds));assertFalse(term.id in term.relatedTermIds)
        }
    }
    @Test fun localSearchMatchesAliasesEnglishAndPrivateUnderstanding() {
        val free=terms().first {it.id=="free-will"}
        assertTrue(free.matches("  意志自由  "));assertTrue(free.matches("FREE WILL"));assertTrue(free.matches("我家的小事","我家的小事让我想到选择"))
        assertFalse(free.matches("一个不存在的词"));assertTrue(free.matches("   "))
    }
    @Test fun chineseTermsKeepTheirOwnContextAndAreNotWesternSchoolAliases() {
        val terms=terms()
        listOf("dao","wuwei","ren","human-nature").forEach {id->
            val t=terms.first {it.id==id};assertEquals("中国哲学",t.category)
            assertTrue(t.schoolIds.all {it in listOf("dao","ru")})
            assertTrue(t.readings.all {it.author in listOf("老子","孟子","孔子")})
        }
        assertFalse("性善" in terms.first {it.id=="human-nature"}.aliases)
    }
}
