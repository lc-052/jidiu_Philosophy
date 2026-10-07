package com.xiaojidiu.philosophy.util

import android.Manifest
import android.app.*
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.work.*
import com.xiaojidiu.philosophy.MainActivity
import com.xiaojidiu.philosophy.R
import com.xiaojidiu.philosophy.data.local.*
import kotlinx.coroutines.flow.first
import java.time.*
import java.util.concurrent.TimeUnit

class ReminderScheduler(private val context: Context) {
    fun configure(settings: SettingsState) {
        val manager=WorkManager.getInstance(context)
        if(!settings.reminder) {manager.cancelUniqueWork("daily-thought");return}
        val now=ZonedDateTime.now();var next=now.withHour(settings.hour).withMinute(settings.minute).withSecond(0).withNano(0)
        if(!next.isAfter(now)) next=next.plusDays(1)
        val work=PeriodicWorkRequestBuilder<ReminderWorker>(24,TimeUnit.HOURS).setInitialDelay(Duration.between(now,next).toMillis(),TimeUnit.MILLISECONDS).build()
        manager.enqueueUniquePeriodicWork("daily-thought",ExistingPeriodicWorkPolicy.CANCEL_AND_REENQUEUE,work)
    }
}
class ReminderWorker(context: Context,params: WorkerParameters): CoroutineWorker(context,params) {
    override suspend fun doWork(): Result {
        if(!PreferencesRepository(applicationContext).settings.first().reminder) return Result.success()
        if(Build.VERSION.SDK_INT>=33 && ContextCompat.checkSelfPermission(applicationContext,Manifest.permission.POST_NOTIFICATIONS)!=PackageManager.PERMISSION_GRANTED) return Result.success()
        val system=applicationContext.getSystemService(NotificationManager::class.java)
        system.createNotificationChannel(NotificationChannel("daily-thought","每日怪问题",NotificationManager.IMPORTANCE_DEFAULT).apply {description="可在 App 设置中完全关闭"})
        val intent=Intent(applicationContext,MainActivity::class.java)
        val pending=PendingIntent.getActivity(applicationContext,0,intent,PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        val notification=NotificationCompat.Builder(applicationContext,"daily-thought").setSmallIcon(R.drawable.ic_notification).setContentTitle("小寄丢大道理").setContentText("今天有个怪问题，要想一下吗？").setContentIntent(pending).setAutoCancel(true).build()
        try {NotificationManagerCompat.from(applicationContext).notify(71,notification)} catch(_: SecurityException) {return Result.success()}
        return Result.success()
    }
}
