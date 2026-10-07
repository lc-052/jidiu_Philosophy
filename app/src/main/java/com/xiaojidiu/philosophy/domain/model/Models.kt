package com.xiaojidiu.philosophy.domain.model

import kotlinx.serialization.Serializable

@Serializable enum class PhilosophyDimension(val title: String) {
    Existentialism("存在主义"), Stoicism("斯多葛主义"), Utilitarianism("功利主义"), Deontology("义务论"), Hedonism("享乐主义"), VirtueEthics("德性伦理学"), Absurdism("荒诞主义"), Nihilism("虚无主义"), Individualism("个人主义"), Collectivism("集体主义"), Rationalism("理性主义"), Empiricism("经验主义"), Skepticism("怀疑主义")
}
@Serializable data class Perspective(val schoolId: String, val title: String, val content: String)
@Serializable data class Choice(val id: String, val text: String, val order: Int, val weights: Map<PhilosophyDimension, Int>, val tendencies: List<String>)
@Serializable data class Question(val id: String, val title: String, val scenario: String, val category: String, val difficulty: Int = 1, val background: String, val sourceType: String, val source: String, val createdAt: Long = 0, val options: List<Choice>, val perspectives: List<Perspective>, val philosopherIds: List<String>, val schoolIds: List<String>)
@Serializable data class Philosopher(val id: String, val name: String, val years: String, val region: String, val intro: String, val focus: String, val ideas: String, val works: List<String>, val schoolIds: List<String>, val source: String)
@Serializable data class School(val id: String, val name: String, val intro: String, val coreQuestion: String, val ideas: String, val misconception: String, val philosopherIds: List<String>, val source: String)
data class AnswerRecord(val id: Long, val questionId: String, val optionId: String, val answeredAt: Long, val reflection: String = "")
data class Favorite(val type: String, val targetId: String)
data class ProfileSnapshot(val id: Long, val createdAt: Long, val answerCount: Int, val scores: Map<PhilosophyDimension, Int>)
@Serializable data class ThoughtRelation(val era: String, val from: String, val to: String, val kind: String, val description: String)
data class Library(val questions: List<Question> = emptyList(), val philosophers: List<Philosopher> = emptyList(), val schools: List<School> = emptyList(), val relations: List<ThoughtRelation> = emptyList())
data class Journal(val answers: List<AnswerRecord> = emptyList(), val favorites: List<Favorite> = emptyList(), val snapshots: List<ProfileSnapshot> = emptyList())
