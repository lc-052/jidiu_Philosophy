package com.xiaojidiu.philosophy
import com.xiaojidiu.philosophy.domain.usecase.ThinkingStreak
import java.time.*
import org.junit.Test
import org.junit.Assert.*
class ThinkingStreakTest {
    private val day=LocalDate.of(2026,10,7);private val zone=ZoneId.of("Asia/Shanghai")
    private fun time(d: Int)=day.minusDays(d.toLong()).atTime(12,0).atZone(zone).toInstant().toEpochMilli()
    @Test fun repeatedSameDayCountsOnce() {assertEquals(2,ThinkingStreak.calculate(listOf(time(0),time(0),time(1)),day,zone))}
    @Test fun yesterdayStillCounts() {assertEquals(2,ThinkingStreak.calculate(listOf(time(1),time(2)),day,zone))}
    @Test fun gapEndsStreak() {assertEquals(0,ThinkingStreak.calculate(listOf(time(2)),day,zone))}
}
