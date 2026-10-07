package com.xiaojidiu.philosophy

import com.xiaojidiu.philosophy.domain.model.*
import com.xiaojidiu.philosophy.domain.usecase.PhilosophyProfileCalculator
import org.junit.Assert.*
import org.junit.Test

class PhilosophyProfileCalculatorTest {
    private val dimension=PhilosophyDimension.Existentialism
    private fun question(id: String) = Question(id,"问题","场景","存在",background="背景",sourceType="原创",source="编辑",options=listOf(Choice("${id}a","A",0,mapOf(dimension to 2),emptyList()),Choice("${id}b","B",1,mapOf(dimension to -1),emptyList())),perspectives=emptyList(),philosopherIds=emptyList(),schoolIds=emptyList())
    private val calc=PhilosophyProfileCalculator()
    @Test fun emptyIsNeutral() { val p=calc.calculate(emptyList(),emptyList()); assertEquals(50,p.scores[dimension]); assertEquals(0,p.answerCount) }
    @Test fun smallSamplesShrink() { val q=question("q"); val p=calc.calculate(listOf(q),listOf(AnswerRecord(1,"q","qa",1))); assertEquals(58,p.scores[dimension]); assertTrue(p.clarity.contains("模糊")) }
    @Test fun negativeWeightsUseFullRange() { val q=question("q"); assertEquals(42,calc.calculate(listOf(q),listOf(AnswerRecord(1,"q","qb",1))).scores[dimension]) }
    @Test fun repeatUsesLatestAndDoesNotInflateConfidence() { val q=question("q"); val p=calc.calculate(listOf(q),listOf(AnswerRecord(1,"q","qa",1),AnswerRecord(2,"q","qb",2))); assertEquals(1,p.answerCount); assertEquals(42,p.scores[dimension]) }
    @Test fun moreEvidenceHasMoreDefinition() { val qs=(1..20).map {question("q$it")}; val p=calc.calculate(qs,qs.mapIndexed {i,q->AnswerRecord(i.toLong(),q.id,q.options[0].id,i.toLong())}); assertEquals(90,p.scores[dimension]); assertTrue(p.scores.values.all {it in 0..100}) }
    @Test fun invalidAnswersAreIgnored() { assertEquals(0,calc.calculate(listOf(question("q")),listOf(AnswerRecord(1,"q","missing",1))).answerCount) }
    @Test fun dimensionsWithoutDiscriminationRemainNeutral() { val p=calc.calculate(listOf(question("q")),listOf(AnswerRecord(1,"q","qa",1))); assertEquals(50,p.scores[PhilosophyDimension.Nihilism]); assertEquals(0,p.evidenceCounts[PhilosophyDimension.Nihilism]) }
}
