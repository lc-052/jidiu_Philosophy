package com.xiaojidiu.philosophy

import com.xiaojidiu.philosophy.domain.model.DebateMessage
import com.xiaojidiu.philosophy.domain.usecase.DiscussionTurnPlanner
import org.junit.Assert.*
import org.junit.Test

class DiscussionTurnPlannerTest {
    private val people=listOf("nietzsche","camus","zhuangzi","kant")
    private val user=DebateMessage("u","user","我","请举例。",null,"user")
    @Test fun openingRoundAllowsAllInvitedPeople() {assertEquals(people,DiscussionTurnPlanner.speakers(people,emptyList(),null))}
    @Test fun followUpCapsSpeakersAndKeepsDirectedPerson() {
        val prior=DebateMessage("target","nietzsche","尼采","观点",null,"argument")
        val next=DiscussionTurnPlanner.speakers(people,listOf(prior,user),"target")
        assertEquals(3,next.size);assertEquals("nietzsche",next.first());assertTrue(people.containsAll(next))
    }
    @Test fun nextRoundsRotateInsteadOfRepeatingEveryone() {
        assertNotEquals(DiscussionTurnPlanner.speakers(people,listOf(user),null),DiscussionTurnPlanner.speakers(people,listOf(user,user.copy(id="u2")),null))
    }
    @Test fun oneInvitedPersonRemainsOne() {assertEquals(listOf("kant"),DiscussionTurnPlanner.speakers(listOf("kant"),listOf(user),"missing"))}
}
