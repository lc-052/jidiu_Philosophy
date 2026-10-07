package com.xiaojidiu.philosophy
import com.xiaojidiu.philosophy.data.remote.dto.*
import org.junit.Test
import org.junit.Assert.*
class ResponseValidatorTest {
    private val message=RoundTableMessage("a","zhuangzi","后端名称","思想概括",null,"argument")
    private fun validate(vararg messages: RoundTableMessage)=ResponseValidator.validate(RoundTableResponse("爱好",messages.toList()),listOf("zhuangzi"),mapOf("zhuangzi" to "庄子"),setOf("old"))
    @Test fun usesCanonicalLocalName() {assertEquals("庄子",validate(message).messages.first().displayName)}
    @Test(expected=IllegalArgumentException::class) fun rejectsUnselectedPerson() {validate(message.copy(philosopherId="unknown"))}
    @Test(expected=IllegalArgumentException::class) fun rejectsDuplicateIds() {validate(message,message)}
    @Test(expected=IllegalArgumentException::class) fun rejectsMissingReplyTarget() {validate(message.copy(responseTo="missing"))}
    @Test fun acceptsReplyToContext() {assertEquals("old",validate(message.copy(responseTo="old")).messages.first().responseTo)}
}
