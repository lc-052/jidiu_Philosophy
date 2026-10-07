package com.xiaojidiu.philosophy

import com.xiaojidiu.philosophy.data.remote.dto.*
import com.xiaojidiu.philosophy.domain.model.*
import org.junit.Test
import org.junit.Assert.*

class DiscussionSummaryTest {
    private val summary=DiscussionSummary(listOf(SummaryPoint("对目标的尺度存在分歧。",listOf("old"))),emptyList())
    private fun validate(s: DiscussionSummary, messages: List<RoundTableMessage> = emptyList())=
        ResponseValidator.validate(RoundTableResponse("努力",messages,s),listOf("zhuangzi"),mapOf("zhuangzi" to "庄子"),setOf("old"),true)
    @Test fun neutralSummaryCanHaveNoCommonGround() {assertTrue(validate(summary).summary!!.commonGround.isEmpty())}
    @Test(expected=IllegalArgumentException::class) fun inventedSourceIsRejected() {validate(summary.copy(disagreement=listOf(SummaryPoint("分歧",listOf("invented")))))}
    @Test(expected=IllegalArgumentException::class) fun sourceLessClaimIsRejected() {validate(summary.copy(commonGround=listOf(SummaryPoint("大家都同意",emptyList()))))}
    @Test(expected=IllegalArgumentException::class) fun summaryDoesNotAddPhilosopherSpeeches() {validate(summary,listOf(RoundTableMessage("new","zhuangzi","庄子","新观点",null,"argument")))}
}
