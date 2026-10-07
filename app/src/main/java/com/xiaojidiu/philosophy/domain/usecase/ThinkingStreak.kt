package com.xiaojidiu.philosophy.domain.usecase
import java.time.*
object ThinkingStreak {
    fun calculate(times: List<Long>, today: LocalDate=LocalDate.now(), zone: ZoneId=ZoneId.systemDefault()): Int {
        val days=times.map {Instant.ofEpochMilli(it).atZone(zone).toLocalDate()}.filter {it<=today}.toSet()
        var day=if(today in days) today else today.minusDays(1); var count=0
        while(day in days) { count++; day=day.minusDays(1) }
        return count
    }
}
