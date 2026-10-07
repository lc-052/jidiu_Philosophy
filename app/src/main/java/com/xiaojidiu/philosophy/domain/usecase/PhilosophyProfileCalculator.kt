package com.xiaojidiu.philosophy.domain.usecase

import com.xiaojidiu.philosophy.domain.model.*
import kotlin.math.roundToInt

data class PhilosophyProfile(val scores: Map<PhilosophyDimension,Int>, val evidenceCounts: Map<PhilosophyDimension,Int>, val answerCount: Int) {
    val clarity: String get() = when { answerCount < 5 -> "画像还比较模糊。再想几道，脑回路才看得清。"; answerCount < 15 -> "这是初步画像，您的理由还在慢慢浮现。"; else -> "这些是目前的倾向，未来仍然可以改变。" }
}
/** 每题以所有选项的 min/max 作为可得分范围；只取每题最新回答，避免反复答题放大权重。
 * 无区分力的题不计证据；拉普拉斯式收缩至中点，k=5。百分数表示编辑性倾向程度，不是概率。
 */
class PhilosophyProfileCalculator {
    fun calculate(questions: List<Question>, answers: List<AnswerRecord>): PhilosophyProfile {
        val latest = answers.sortedWith(compareByDescending<AnswerRecord> { it.answeredAt }.thenByDescending { it.id }).distinctBy { it.questionId }
        val valid = latest.mapNotNull { a -> questions.find { it.id == a.questionId }?.let { q -> q.options.find { it.id==a.optionId }?.let { q to it } } }
        val evidence = mutableMapOf<PhilosophyDimension,Int>()
        val scores = PhilosophyDimension.entries.associateWith { dimension ->
            var numerator=0.0; var capacity=0.0; var count=0
            valid.forEach { (q, selected) ->
                val values=q.options.map { it.weights[dimension] ?: 0 }
                val min=values.minOrNull() ?: 0; val max=values.maxOrNull() ?: 0
                if(max>min) { numerator += (selected.weights[dimension] ?: 0)-min; capacity += max-min; count++ }
            }
            evidence[dimension]=count
            if(capacity==0.0) 50 else {
                val normalized=numerator/capacity
                val confidence=count.toDouble()/(count+5.0)
                (100*(0.5+(normalized-0.5)*confidence)).roundToInt().coerceIn(0,100)
            }
        }
        return PhilosophyProfile(scores,evidence,valid.size)
    }
}
