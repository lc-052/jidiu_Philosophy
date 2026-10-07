package com.xiaojidiu.philosophy.util
import java.time.*
import java.time.format.DateTimeFormatter
fun displayDate(millis: Long): String = Instant.ofEpochMilli(millis).atZone(ZoneId.systemDefault()).format(DateTimeFormatter.ofPattern("yyyy年M月d日 HH:mm"))
